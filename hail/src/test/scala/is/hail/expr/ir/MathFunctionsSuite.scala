package is.hail.expr.ir

import is.hail.{ExecStrategy, HailSuite}
import is.hail.expr.types._
import is.hail.utils._
import is.hail.TestUtils._
import is.hail.expr.types.virtual.{TArray, TFloat64}
import org.apache.spark.sql.Row
import org.testng.annotations.{DataProvider, Test}


class MathFunctionsSuite extends HailSuite {
  hc
  implicit val execStrats = ExecStrategy.values

  val tfloat = TFloat64()

  @Test def basicUnirootFunction() {
    implicit val execStrats = ExecStrategy.javaOnly

    val ir = Uniroot("x",
      ApplyBinaryPrimOp(Add(), Ref("x", tfloat), F64(3)),
      F64(-6), F64(0))

    assertEvalsTo(ir, -3.0)
  }

  @Test def unirootWithExternalBinding() {
    implicit val execStrats = ExecStrategy.javaOnly

    val fn = ApplyBinaryPrimOp(Add(),
      Ref("x", tfloat),
      Ref("b", tfloat))
    val ir = Let("b", F64(3),
      Uniroot("x", fn, F64(-6), F64(0)))

    assertEvalsTo(ir, -3.0)
  }

  @Test def unirootWithRegionManipulation() {
    implicit val execStrats = ExecStrategy.javaOnly

    def sum(array: IR): IR =
      ArrayFold(array, F64(0), "sum", "i", ApplyBinaryPrimOp(Add(), Ref("sum", tfloat), Ref("i", tfloat)))
    val fn = ApplyBinaryPrimOp(Add(),
      sum(MakeArray(Seq(Ref("x", tfloat), Ref("x", tfloat)), TArray(tfloat))),
      Ref("b", tfloat))
    val ir = Let("b", F64(6),
      Uniroot("x", fn, F64(-6), F64(0)))

    assertEvalsTo(ir, -3.0)
  }

  @Test def isnan() {
    implicit val execStrats = ExecStrategy.javaOnly

    assertEvalsTo(invoke("isnan", F32(0)), false)
    assertEvalsTo(invoke("isnan", F32(Float.NaN)), true)

    assertEvalsTo(invoke("isnan", F64(0)), false)
    assertEvalsTo(invoke("isnan", F64(Double.NaN)), true)
  }

  @Test def is_finite() {
    implicit val execStrats = ExecStrategy.javaOnly

    assertEvalsTo(invoke("is_finite", F32(0)), expected = true)
    assertEvalsTo(invoke("is_finite", F32(Float.MaxValue)), expected = true)
    assertEvalsTo(invoke("is_finite", F32(Float.NaN)), expected = false)
    assertEvalsTo(invoke("is_finite", F32(Float.PositiveInfinity)), expected = false)
    assertEvalsTo(invoke("is_finite", F32(Float.NegativeInfinity)), expected = false)

    assertEvalsTo(invoke("is_finite", F64(0)), expected = true)
    assertEvalsTo(invoke("is_finite", F64(Double.MaxValue)), expected = true)
    assertEvalsTo(invoke("is_finite", F64(Double.NaN)), expected = false)
    assertEvalsTo(invoke("is_finite", F64(Double.PositiveInfinity)), expected = false)
    assertEvalsTo(invoke("is_finite", F64(Double.NegativeInfinity)), expected = false)
  }

  @Test def is_infinite() {
    implicit val execStrats = ExecStrategy.javaOnly

    assertEvalsTo(invoke("is_infinite", F32(0)), expected = false)
    assertEvalsTo(invoke("is_infinite", F32(Float.MaxValue)), expected = false)
    assertEvalsTo(invoke("is_infinite", F32(Float.NaN)), expected = false)
    assertEvalsTo(invoke("is_infinite", F32(Float.PositiveInfinity)), expected = true)
    assertEvalsTo(invoke("is_infinite", F32(Float.NegativeInfinity)), expected = true)

    assertEvalsTo(invoke("is_infinite", F64(0)), expected = false)
    assertEvalsTo(invoke("is_infinite", F64(Double.MaxValue)), expected = false)
    assertEvalsTo(invoke("is_infinite", F64(Double.NaN)), expected = false)
    assertEvalsTo(invoke("is_infinite", F64(Double.PositiveInfinity)), expected = true)
    assertEvalsTo(invoke("is_infinite", F64(Double.NegativeInfinity)), expected = true)
  }


  @Test def sign() {
    implicit val execStrats = ExecStrategy.javaOnly

    assertEvalsTo(invoke("sign", I32(2)), 1)
    assertEvalsTo(invoke("sign", I32(0)), 0)
    assertEvalsTo(invoke("sign", I32(-2)), -1)

    assertEvalsTo(invoke("sign", I64(2)), 1l)
    assertEvalsTo(invoke("sign", I64(0)), 0l)
    assertEvalsTo(invoke("sign", I64(-2)), -1l)

    assertEvalsTo(invoke("sign", F32(2)), 1.0f)
    assertEvalsTo(invoke("sign", F32(0)), 0.0f)
    assertEvalsTo(invoke("sign", F32(-2)), -1.0f)

    assertEvalsTo(invoke("sign", F64(2)), 1.0)
    assertEvalsTo(invoke("sign", F64(0)), 0.0)
    assertEvalsTo(invoke("sign", F64(-2)), -1.0)

    assert(eval(invoke("sign", F64(Double.NaN))).asInstanceOf[Double].isNaN)
    assertEvalsTo(invoke("sign", F64(Double.PositiveInfinity)), 1.0)
    assertEvalsTo(invoke("sign", F64(Double.NegativeInfinity)), -1.0)
  }

  @Test def approxEqual() {
    implicit val execStrats = ExecStrategy.javaOnly

    assertEvalsTo(invoke("approxEqual", F64(0.025), F64(0.0250000001), F64(1e-4), False(), False()), true)
    assertEvalsTo(invoke("approxEqual", F64(0.0154), F64(0.0156), F64(1e-4), True(), False()), false)
    assertEvalsTo(invoke("approxEqual", F64(0.0154), F64(0.0156), F64(1e-3), True(), False()), true)
    assertEvalsTo(invoke("approxEqual", F64(Double.NaN), F64(Double.NaN), F64(1e-3), True(), False()), false)
    assertEvalsTo(invoke("approxEqual", F64(Double.NaN), F64(Double.NaN), F64(1e-3), True(), True()), true)
    assertEvalsTo(invoke("approxEqual", F64(Double.PositiveInfinity), F64(Double.PositiveInfinity), F64(1e-3), True(), False()), true)
    assertEvalsTo(invoke("approxEqual", F64(Double.NegativeInfinity), F64(Double.NegativeInfinity), F64(1e-3), True(), False()), true)
    assertEvalsTo(invoke("approxEqual", F64(Double.PositiveInfinity), F64(Double.NegativeInfinity), F64(1e-3), True(), False()), false)
  }

  @Test def entropy() {
    implicit val execStrats = ExecStrategy.javaOnly

    assertEvalsTo(invoke("entropy", Str("")), 0.0)
    assertEvalsTo(invoke("entropy", Str("a")), 0.0)
    assertEvalsTo(invoke("entropy", Str("aa")), 0.0)
    assertEvalsTo(invoke("entropy", Str("ac")), 1.0)
    assertEvalsTo(invoke("entropy", Str("accctg")), 1.7924812503605778)
  }

  @Test def unirootIsStrictInMinAndMax() {
    implicit val execStrats = ExecStrategy.javaOnly

    assertEvalsTo(
      Uniroot("x", Ref("x", tfloat), F64(-6), NA(tfloat)),
      null)
    assertEvalsTo(
      Uniroot("x", Ref("x", tfloat), NA(tfloat), F64(0)),
      null)
  }

  @DataProvider(name = "chi_squared_test")
  def chiSquaredData(): Array[Array[Any]] = Array(
    Array(0, 0, 0, 0, Double.NaN, Double.NaN),
    Array(0, 1, 1, 1, 0.38647623077123266, 0.0),
    Array(1, 0, 1, 1, 0.38647623077123266, Double.PositiveInfinity),
    Array(1, 1, 0, 1, 0.38647623077123266, Double.PositiveInfinity),
    Array(1, 1, 1, 0, 0.38647623077123266, 0.0),
    Array(10, 10, 10, 10, 1.0, 1.0),
    Array(51, 43, 22, 92, 1.462626e-7, (51.0 * 92) / (22 * 43))
  )

  @Test(dataProvider = "chi_squared_test")
  def chiSquaredTest(a: Int, b: Int, c: Int, d: Int, pValue: Double, oddsRatio: Double) {
      val r = eval(invoke("chi_squared_test", a, b, c, d)).asInstanceOf[Row]
      assert(D0_==(pValue, r.getDouble(0)))
      assert(D0_==(oddsRatio, r.getDouble(1)))
  }

  @DataProvider(name = "fisher_exact_test")
  def fisherExactData(): Array[Array[Any]] = Array(
    Array(0, 0, 0, 0, Double.NaN, Double.NaN, Double.NaN, Double.NaN),
    Array(10, 10, 10, 10, 1.0, 1.0, 0.243858, 4.100748),
    Array(51, 43, 22, 92, 2.1565e-7, 4.918058, 2.565937, 9.677930)
  )

  @Test(dataProvider = "fisher_exact_test")
  def fisherExactTest(a: Int, b: Int, c: Int, d: Int, pValue: Double, oddsRatio: Double, confLower: Double, confUpper: Double) {
    val r = eval(invoke("fisher_exact_test", a, b, c, d)).asInstanceOf[Row]
    assert(D0_==(pValue, r.getDouble(0)))
    assert(D0_==(oddsRatio, r.getDouble(1)))
    assert(D0_==(confLower, r.getDouble(2)))
    assert(D0_==(confUpper, r.getDouble(3)))
  }

  @DataProvider(name = "contingency_table_test")
  def contingencyTableData(): Array[Array[Any]] = Array(
    Array(51, 43, 22, 92, 22, 1.462626e-7, 4.95983087),
    Array(51, 43, 22, 92, 23, 2.1565e-7, 4.91805817)
  )

  @Test(dataProvider = "contingency_table_test")
  def contingencyTableTest(a: Int, b: Int, c: Int, d: Int, minCellCount: Int, pValue: Double, oddsRatio: Double) {
    val r = eval(invoke("contingency_table_test", a, b, c, d, minCellCount)).asInstanceOf[Row]
    assert(D0_==(pValue, r.getDouble(0)))
    assert(D0_==(oddsRatio, r.getDouble(1)))
  }

  @DataProvider(name = "hardy_weinberg_test")
  def hardyWeinbergData(): Array[Array[Any]] = Array(
    Array(0, 0, 0, Double.NaN, 0.5),
    Array(1, 2, 1, 0.57142857, 0.65714285),
    Array(0, 1, 0, 1.0, 0.5),
    Array(100, 200, 100, 0.50062578, 0.96016808)
  )

  @Test(dataProvider = "hardy_weinberg_test")
  def hardyWeinbergTest(nHomRef: Int, nHet: Int, nHomVar: Int, pValue: Double, hetFreq: Double) {
    val r = eval(invoke("hardy_weinberg_test", nHomRef, nHet, nHomVar)).asInstanceOf[Row]
    assert(D0_==(pValue, r.getDouble(0)))
    assert(D0_==(hetFreq, r.getDouble(1)))
  }

  @Test def modulusTest() {
    assertFatal(invoke("%", I32(1), I32(0)), "modulo by zero")
    assertFatal(invoke("%", I64(1), I64(0)), "modulo by zero")
    assertFatal(invoke("%", F32(1), F32(0)), "modulo by zero")
    assertFatal(invoke("%", F64(1), F64(0)), "modulo by zero")
  }

  @Test def testMinMax() {
    implicit val execStrats = ExecStrategy.javaOnly
    assertAllEvalTo(
      (invoke("min", F32(1.0f), F32(2.0f)), 1.0f),
      (invoke("max", F32(1.0f), F32(2.0f)), 2.0f),
      (invoke("min", F64(1.0), F64(2.0)), 1.0),
      (invoke("max", F64(1.0), F64(2.0)), 2.0),
      (invoke("min", I32(1), I32(2)), 1),
      (invoke("max", I32(1), I32(2)), 2),
      (invoke("min", I64(1L), I64(2L)), 1L),
      (invoke("max", I64(1L), I64(2L)), 2L),
      (invoke("min", F32(Float.NaN), F32(1.0f)), Float.NaN),
      (invoke("min", F64(Double.NaN), F64(1.0)), Double.NaN),
      (invoke("max", F32(Float.NaN), F32(1.0f)), Float.NaN),
      (invoke("max", F64(Double.NaN), F64(1.0)), Double.NaN),
      (invoke("min", F32(1.0f), F32(Float.NaN)), Float.NaN),
      (invoke("min", F64(1.0), F64(Double.NaN)), Double.NaN),
      (invoke("max", F32(1.0f), F32(Float.NaN)), Float.NaN),
      (invoke("max", F64(1.0), F64(Double.NaN)), Double.NaN)
    )
  }
}
