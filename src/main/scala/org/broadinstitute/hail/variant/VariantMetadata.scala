package org.broadinstitute.hail.variant

import org.broadinstitute.hail.annotations._
import org.broadinstitute.hail.io.annotators.SampleAnnotator

object VariantMetadata {

  def apply(sampleIds: Array[String]): VariantMetadata = new VariantMetadata(Array.empty[(String, String)],
    sampleIds,
    Annotation.emptyIndexedSeq(sampleIds.length),
    Annotation.emptySignature,
    Annotation.emptySignature)

  def apply(filters: IndexedSeq[(String, String)], sampleIds: Array[String],
    sa: IndexedSeq[Annotation], sas: Signature, vas: Signature): VariantMetadata = {
    new VariantMetadata(filters, sampleIds, sa, sas, vas)
  }
}

case class VariantMetadata(filters: IndexedSeq[(String, String)],
  sampleIds: IndexedSeq[String],
  sampleAnnotations: IndexedSeq[Annotation],
  saSignatures: Signature,
  vaSignatures: Signature,
  wasSplit: Boolean = false) {

  def nSamples: Int = sampleIds.length
}
