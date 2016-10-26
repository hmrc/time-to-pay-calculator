package uk.gov.hmrc.selfservicetimetopay.services

import uk.gov.hmrc.selfservicetimetopay.models.Calculation

trait CalculationValidator {
  val validator: Validator = ???
}

class CalculationValidator extends CalculationValidator {
  val validator = ???

  def validate(calculation: Calculation) {
    val violations: Seq[ConstraintViolation[Calculation]] = validator.validate(calculation)
    if (!CollectionUtils.isEmpty(violations)) throw new ConstraintViolationException("calculation validation failure", violations)
  }
}
