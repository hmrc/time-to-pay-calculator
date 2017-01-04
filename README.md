
# time-to-pay-calculator

[![Build Status](https://travis-ci.org/hmrc/time-to-pay-calculator.svg?branch=master)](https://travis-ci.org/hmrc/time-to-pay-calculator) [ ![Download](https://api.bintray.com/packages/hmrc/releases/time-to-pay-calculator/images/download.svg) ](https://bintray.com/hmrc/releases/time-to-pay-calculator/_latestVersion)

## Endpoint URLs

POST `/time-to-pay-calculator/paymentschedule`

## Service Definitions

All requests use the HTTP `POST` method

### Single Payment Schedule Calculator (external facing API - `/paymentschedule`)

#### Functionality

Return a payment schedule for a bounded period (next 'event' date)

* Given:
    * A collection of `debit` items (type, amount, interest already accrued, interest calculated to date)
    * A `start date` for the arrangement (default to today)
    * An `end date` for the arrangement (default to start date + 11 months)
    * An `initial payment` amount affordable

* Invoke the `Payment Schedule Calculator` (below) with an end date set to `months` in the future
* Collate results in a PaymentSchedule element and return

#### Input structure
Calculator input model:

| Parameter                                            | Type            | Description                                                            |
|:----------------------------------------------------:|:---------------:|:----------------------------------------------------------------------:|
| debits                                               | Seq[Debit]     | Collection of debits owed                                              |
| initialPayment                                       | BigDecimal      | How much they can pay now                                              |
| startDate                                            | LocalDate       | The start date of the TTP arrangement                                  |
| endDate                                              | LocalDate       | The end date of the TTP arrangement                                    |
| firstPaymentDate                                     | Option[LocalDate]       | The date on which the first payment will be taken                      |
| paymentFrequency            [DAILY\|WEEKLY\|MONTHLY] | String          | The frequency of the instalment payments                               |


Debit input model:

| Parameter               | Type       | Description                                                             |
|:-----------------------:|:----------:|:-----------------------------------------------------------------------:|
| originCode              | Option[String]     | The originCode of the debt                                              |
| amount                  | BigDecimal | The amount owed for that debt                                           |
| interest                | Option[Interest]   | An interest object                                                      |
| dueDate     | LocalDate  | The due date of the debt, used to calculate which debt to pay off first |

Interest model:

| Parameter               | Type       | Description                                                             |
|:-----------------------:|:----------:|:-----------------------------------------------------------------------:|
| amountAccrued           | BigDecimal | How much interest has been accumulated to date                          |
| calculationDate         | LocalDate  | At what date was the interest last calculated up to                     |

Sample input request:
```
{
   "debits":[
      {
         "originCode":"IN1",
         "amount":5000,
         "dueDate":"2017-01-25",
         "interest":{
            "calculationDate":"2017-01-25",
            "amountAccrued":500
         },
         "taxYearEnd":"2017-04-05"
      }
   ],
   "initialPayment":0,
   "startDate":"2017-01-04",
   "endDate":"2017-03-04",
   "firstPaymentDate":"2017-01-04",
   "paymentFrequency":"MONTHLY"
}
```

#### Output structure
Payment schedule output model:

| Parameter                 | Type              | Description                                         |
|:-------------------------:|:-----------------:|:---------------------------------------------------:|
| startDate            | LocalDate        | Start date of the payment schedule                         |
| endDate            | LocalDate        | End date of the payment schedule                          |
| initialPayment            | BigDecimal        | How much they can pay now                           |
| amountToPay               | BigDecimal        | Total debt owed                                     |
| instalmentBalance         | BigDecimal        | The amount to pay over the instalments              |
| totalInterestCharged      | BigDecimal        | How much interest has been applied                  |
| totalPayable              | BigDecimal        | The amount to be paid from today including interest |
| instalments               | Seq[Instalment]  | List of the instalment payment amounts and dates    |

Instalment (inner class) output model:

| Parameter      	| Type       	| Description                           	|
|:-----------------:|:-------------:|:-----------------------------------------:|
| paymentDate    	| LocalDate  	| The date that the payment is due      	|
| amount 	        | BigDecimal 	| The amount to pay for that instalment 	|
| interest 	        | BigDecimal 	| The amount of interest for that instalment 	|

Sample successful output response:
````
[
  {
    "startDate": "2017-01-04",
    "endDate": "2017-03-04",
    "initialPayment": 0,
    "amountToPay": 5000,
    "instalmentBalance": 5000,
    "totalInterestCharged": 511.29,
    "totalPayable": 5511.29,
    "instalments": [
      {
        "paymentDate": "2017-01-04",
        "amount": 1666.67,
        "interest": 0
      },
      {
        "paymentDate": "2017-02-04",
        "amount": 1666.67,
        "interest": 3.89
      },
      {
        "paymentDate": "2017-03-04",
        "amount": 2177.96,
        "interest": 7.4
      }
    ]
  }
]
````

### Payment Schedule Calculator definition (internal - for information only)

* Given:
    * A collection of debt line items (`type`, `amount`, `interest already accrued`, `interest calculated to date`)
    * A `start date` for the arrangement (default to today)
    * An `end date` for the arrangement (end date)
    * An initial payable now amount

* Order debt line items by their due date (earliest date first)
* For each debt line item - establish the `liability to start date` and `interest over arrangement` values:
    * Establish interest rates for period between `interest calculated to date` and `start date`
    * For each interest rate within that period:
        * Establish `number of days` @ `interest rate`
        * Apply simple interest rate formula using `number of days`, `interest rate` & debt line item `amount`
    * Sum all interest values calculated (no precision loss - `interest to start date`)
    * Subtract any remaining `payable now` amount from the `amount` (`remaining amount`)
        * If this results in a negative number (debt line item fully paid off - only interest remaining), move to the next line and reduce the `payable now` amount remaining to apply to that line by the value of this line item's `amount`, set `liability to start date` of this line to `interest to start date` (only the interest component remains)
    * For the final interest period, add a further calculation based on `remaining amount` for interest from `start date` to arrangement `end date`
* Sum all debt line item interest costs (`total interest` no precision loss)
* Sum `total interest` calculated with sum of all debt line item `amounts` (`total liability` over period of TTP)
* Establish `number of payments` (using monthly frequency and end date to establish number)
* Divide `total liability` by `number of payments` & truncate (FLOOR) the value to 2 decimal places (monthly payment amounts for months-1 payments)
* Multiply monthly payment amount by months-1 (total paid over all but last period)
* Subtract the "total for all but last period" from the total liability to establish the potentially slightly higher (but never lower) final payment
* Build a payment plan based on the calculations

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
