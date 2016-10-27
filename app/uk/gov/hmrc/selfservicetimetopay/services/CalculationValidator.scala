package uk.gov.hmrc.selfservicetimetopay.services

import uk.gov.hmrc.selfservicetimetopay.models.Calculation

object CalculationValidator extends CalculationValidator {
  override val validator = ???
}

trait CalculationValidator {
  val validator: Validator = ???

  def validate(calculation: Calculation) {
    val violations: Seq[ConstraintViolation[Calculation]] = validator.validate(calculation)
    if (!CollectionUtils.isEmpty(violations)) throw new ConstraintViolationException("calculation validation failure", violations)
  }
}
