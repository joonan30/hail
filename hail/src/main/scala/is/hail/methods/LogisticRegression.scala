package is.hail.methods

import breeze.linalg._
import is.hail.annotations._
import is.hail.expr.ir.functions.MatrixToTableFunction
import is.hail.expr.ir.{MatrixValue, TableValue}
import is.hail.expr.types.virtual.{TArray, TFloat64, TStruct}
import is.hail.expr.types.{MatrixType, TableType}
import is.hail.rvd.RVDType
import is.hail.stats._
import is.hail.utils._
import org.apache.spark.storage.StorageLevel

case class LogisticRegression(
  test: String,
  yFields: Array[String],
  xField: String,
  covFields: Array[String],
  passThrough: Array[String]) extends MatrixToTableFunction {

  def typeInfo(childType: MatrixType, childRVDType: RVDType): (TableType, RVDType) = {
    val logRegTest = LogisticRegressionTest.tests(test)
    val multiPhenoSchema = TStruct(("logistic_regression", TArray(logRegTest.schema)))
    val passThroughType = TStruct(passThrough.map(f => f -> childType.rowType.field(f).typ): _*)
    val tableType = TableType(childType.rowKeyStruct ++ passThroughType ++ multiPhenoSchema, childType.rowKey, TStruct())
    (tableType, tableType.canonicalRVDType)
  }

  def preservesPartitionCounts: Boolean = true

  def execute(mv: MatrixValue): TableValue = {
    val logRegTest = LogisticRegressionTest.tests(test)
    val (tableType, newRVDType) = typeInfo(mv.typ, mv.rvd.typ)

    val multiPhenoSchema = TStruct(("logistic_regression", TArray(logRegTest.schema)))

    val (yVecs, cov, completeColIdx) = RegressionUtils.getPhenosCovCompleteSamples(mv, yFields, covFields)

    (0 until yVecs.cols).foreach(col => {
      if (!yVecs(::, col).forall(yi => yi == 0d || yi == 1d))
        fatal(s"For logistic regression, y at index ${col} must be bool or numeric with all present values equal to 0 or 1")
      val sumY = sum(yVecs(::,col))
      if (sumY == 0d || sumY == yVecs(::,col).length)
        fatal(s"For logistic regression, y at index ${col} must be non-constant")
    })

    val n = yVecs.rows
    val k = cov.cols
    val d = n - k - 1

    if (d < 1)
      fatal(s"$n samples and ${ k + 1 } ${ plural(k, "covariate") } (including x) implies $d degrees of freedom.")

    info(s"logistic_regression_rows: running $test on $n samples for response variable y,\n"
      + s"    with input variable x, and ${ k } additional ${ plural(k, "covariate") }...")

    val nullFits = (0 until yVecs.cols).map(col => {
      val nullModel = new LogisticRegressionModel(cov, yVecs(::, col))
      var nullFit = nullModel.fit()

      if (!nullFit.converged)
        if (logRegTest == LogisticFirthTest)
          nullFit = GLMFit(nullModel.bInterceptOnly(),
            None, None, 0, nullFit.nIter, exploded = nullFit.exploded, converged = false)
        else
          fatal("Failed to fit logistic regression null model (standard MLE with covariates only): " + (
            if (nullFit.exploded)
              s"exploded at Newton iteration ${nullFit.nIter}"
            else
              "Newton iteration failed to converge"))
      nullFit
    })

    val sc = mv.sparkContext
    val completeColIdxBc = sc.broadcast(completeColIdx)

    val yVecsBc = sc.broadcast(yVecs)
    val XBc = sc.broadcast(new DenseMatrix[Double](n, k + 1, cov.toArray ++ Array.ofDim[Double](n)))
    val nullFitBc = sc.broadcast(nullFits)
    val logRegTestBc = sc.broadcast(logRegTest)
    val resultSchemaBc = sc.broadcast(logRegTest.schema)

    val fullRowType = mv.typ.rvRowType.physicalType
    val entryArrayType = mv.typ.entryArrayType.physicalType
    val entryType = mv.typ.entryType.physicalType
    val fieldType = entryType.field(xField).typ

    assert(fieldType.virtualType.isOfType(TFloat64()))

    val entryArrayIdx = mv.typ.entriesIdx
    val fieldIdx = entryType.fieldIdx(xField)

    val copiedFieldIndices = (mv.typ.rowKey ++ passThrough).map(mv.typ.rvRowType.fieldIdx(_)).toArray

    val newRVD = mv.rvd.mapPartitions(newRVDType) { it =>
      val rvb = new RegionValueBuilder()
      val rv2 = RegionValue()

      val missingCompleteCols = new ArrayBuilder[Int]()
      val _nullFits = nullFitBc.value
      val _yVecs = yVecsBc.value
      val _resultSchema = resultSchemaBc.value
      val X = XBc.value.copy
      it.map { rv =>
        RegressionUtils.setMeanImputedDoubles(X.data, n * k, completeColIdxBc.value, missingCompleteCols,
          rv, fullRowType, entryArrayType, entryType, entryArrayIdx, fieldIdx)
        val logregAnnotations = (0 until _yVecs.cols).map(col => {
          logRegTestBc.value.test(X, _yVecs(::,col), _nullFits(col), "logistic")
        })

        rvb.set(rv.region)
        rvb.start(newRVDType.rowType)
        rvb.startStruct()
        rvb.addFields(fullRowType, rv, copiedFieldIndices)
        rvb.startArray(_yVecs.cols)
        logregAnnotations.foreach(stats => {
          rvb.startStruct()
          stats.addToRVB(rvb)
          rvb.endStruct()

        })
        rvb.endArray()
        rvb.endStruct()
        rv2.set(rv.region, rvb.end())
        rv2
      }
    }.persist(StorageLevel.MEMORY_AND_DISK)

    TableValue(tableType, BroadcastRow.empty(), newRVD)
  }
}
