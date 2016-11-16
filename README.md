
# time-to-pay-calculator

[![Build Status](https://travis-ci.org/hmrc/time-to-pay-calculator.svg?branch=master)](https://travis-ci.org/hmrc/time-to-pay-calculator) [ ![Download](https://api.bintray.com/packages/hmrc/releases/time-to-pay-calculator/images/download.svg) ](https://bintray.com/hmrc/releases/time-to-pay-calculator/_latestVersion)

## Endpoint URLs

POST `/paymentschedule`

## Service Definitions

All requests use the HTTP `POST` method

### Multiple Payment Schedule Calculator (external facing API - `/paymentschedule`)

#### Functionality

Return a set of payment schedules for a bounded period (next 'event' date)

* Given:
    * A collection of `debit` items (type, amount, interest already accrued, interest calculated to date)
    * A `start date` for the arrangement (default to today)
    * An `end date` for the arrangement (default to start date + 11 months)
    * An `initial payment` amount affordable

* For each "number of arrangements" (`months` = 2 .. `end date` months)
    * Invoke the `Payment Schedule Calculator` (below) with an end date set to `months` in the future
* Collate results in a PaymentSchedules element and return

#### Input structure
Calculator input model:

| Parameter                                            | Type            | Description                                                            |
|:----------------------------------------------------:|:---------------:|:----------------------------------------------------------------------:|
| debits                                               | List[Debit]     | Collection of debits owed                                              |
| initialPayment                                       | BigDecimal      | How much they can pay now                                              |
| startDate                                            | LocalDate       | The start date of the TTP arrangement                                  |
| endDate                                              | LocalDate       | The end date of the TTP arrangement                                    |
| firstPaymentDate                                     | LocalDate       | The date on which the first payment will be taken                      |
| paymentFrequency            [DAILY\|WEEKLY\|MONTHLY] | String          | The frequency of the instalment payments                               |


Debit input model:

| Parameter               | Type       | Description                                                             |
|:-----------------------:|:----------:|:-----------------------------------------------------------------------:|
| originCode              | String     | The originCode of the debt                                              |
| amount                  | BigDecimal | The amount owed for that debt                                           |
| interest                | Interest   | An interest object                                                      |
| dueDate (optional)      | LocalDate  | The due date of the debt, used to calculate which debt to pay off first |

Interest model:

| Parameter               | Type       | Description                                                             |
|:-----------------------:|:----------:|:-----------------------------------------------------------------------:|
| amountAccrued           | BigDecimal | How much interest has been accumulated to date                          |
| calculationDate         | LocalDate  | At what date was the interest last calculated up to                     |

Sample input request:
```
{
  "debits" : [
   {
   	 "originCode": "POA2";
   	 "amount": 250.52,
   	 "interest" : {
   	    "amountAccrued": 42.32,
       	"calculationDate": "2016-06-01"
   	 },
   	 "dueDate": "2016-01-31"
   },
   {
     "originCode": "POA1";
   	 "amount": 134.07,
   	 "interest" : {
   	    "amountAccrued": 10.50,
       	"calculationDate": "2016-02-01"
   	 },
   	 "dueDate": ""
   }
  ]
  "initialPayment": 50,
  "startDate": "2016-09-01",
  "endDate": "2017-08-01",
  "paymentFrequency": "MONTHLY"
}
```

#### Output structure
Payment schedule output model:

| Parameter                 | Type              | Description                                         |
|:-------------------------:|:-----------------:|:---------------------------------------------------:|
| initialPayment            | BigDecimal        | How much they can pay now                           |
| amountToPay               | BigDecimal        | Total debt owed                                     |
| instalmentBalance         | BigDecimal        | The amount to pay over the instalments              |
| totalInterestCharged      | BigDecimal        | How much interest has been applied                  |
| totalPayable              | BigDecimal        | The amount to be paid from today including interest |
| instalments               | List[Instalment]  | List of the instalment payment amounts and dates    |

Instalment (inner class) output model:

| Parameter      	| Type       	| Description                           	|
|:-----------------:|:-------------:|:-----------------------------------------:|
| paymentDate    	| LocalDate  	| The date that the payment is due      	|
| amount 	        | BigDecimal 	| The amount to pay for that instalment 	|

Sample successful output response:
````
[
  {
    "initialPayment": 50,
    "amountToPay": 5000,
    "instalmentBalance": 4950,
    "totalInterestCharged": 22.91,
    "totalPayable": 5022.91,
    "instalments": [
      {
        "paymentDate": "2016-10-01",
        "amount": 2486.45
      },
      {
        "paymentDate": "2016-11-01",
        "amount": 2486.45
      }
    ]
  },
  {
    "initialPayment": 50,
    "amountToPay": 5000,
    "instalmentBalance": 4950,
    "totalInterestCharged": 34.18,
    "totalPayable": 5034.18,
    "instalments": [
      {
        "paymentDate": "2016-10-01",
        "amount": 1661.39
      },
      {
        "paymentDate": "2016-11-01",
        "amount": 1661.39
      },
      {
        "paymentDate": "2016-12-01",
        "amount": 1661.39
      }
    ]
  },
  {
    "initialPayment": 50,
    "amountToPay": 5000,
    "instalmentBalance": 4950,
    "totalInterestCharged": 45.83,
    "totalPayable": 5045.83,
    "instalments": [
      {
        "paymentDate": "2016-10-01",
        "amount": 1248.95
      },
      {
        "paymentDate": "2016-11-01",
        "amount": 1248.95
      },
      {
        "paymentDate": "2016-12-01",
        "amount": 1248.95
      },
      {
        "paymentDate": "2017-01-01",
        "amount": 1248.95
      }
    ]
  },
  {
    "initialPayment": 50,
    "amountToPay": 5000,
    "instalmentBalance": 4950,
    "totalInterestCharged": 57.5,
    "totalPayable": 5057.5,
    "instalments": [
      {
        "paymentDate": "2016-10-01",
        "amount": 1001.5
      },
      {
        "paymentDate": "2016-11-01",
        "amount": 1001.5
      },
      {
        "paymentDate": "2016-12-01",
        "amount": 1001.5
      },
      {
        "paymentDate": "2017-01-01",
        "amount": 1001.5
      },
      {
        "paymentDate": "2017-02-01",
        "amount": 1001.5
      }
    ]
  },
  {
    "initialPayment": 50,
    "amountToPay": 5000,
    "instalmentBalance": 4950,
    "totalInterestCharged": 68.05,
    "totalPayable": 5068.05,
    "instalments": [
      {
        "paymentDate": "2016-10-01",
        "amount": 836.34
      },
      {
        "paymentDate": "2016-11-01",
        "amount": 836.34
      },
      {
        "paymentDate": "2016-12-01",
        "amount": 836.34
      },
      {
        "paymentDate": "2017-01-01",
        "amount": 836.34
      },
      {
        "paymentDate": "2017-02-01",
        "amount": 836.34
      },
      {
        "paymentDate": "2017-03-01",
        "amount": 836.34
      }
    ]
  },
  {
    "initialPayment": 50,
    "amountToPay": 5000,
    "instalmentBalance": 4950,
    "totalInterestCharged": 79.73,
    "totalPayable": 5079.73,
    "instalments": [
      {
        "paymentDate": "2016-10-01",
        "amount": 718.53
      },
      {
        "paymentDate": "2016-11-01",
        "amount": 718.53
      },
      {
        "paymentDate": "2016-12-01",
        "amount": 718.53
      },
      {
        "paymentDate": "2017-01-01",
        "amount": 718.53
      },
      {
        "paymentDate": "2017-02-01",
        "amount": 718.53
      },
      {
        "paymentDate": "2017-03-01",
        "amount": 718.53
      },
      {
        "paymentDate": "2017-04-01",
        "amount": 718.53
      }
    ]
  },
  {
    "initialPayment": 50,
    "amountToPay": 5000,
    "instalmentBalance": 4950,
    "totalInterestCharged": 91.03,
    "totalPayable": 5091.03,
    "instalments": [
      {
        "paymentDate": "2016-10-01",
        "amount": 630.12
      },
      {
        "paymentDate": "2016-11-01",
        "amount": 630.12
      },
      {
        "paymentDate": "2016-12-01",
        "amount": 630.12
      },
      {
        "paymentDate": "2017-01-01",
        "amount": 630.12
      },
      {
        "paymentDate": "2017-02-01",
        "amount": 630.12
      },
      {
        "paymentDate": "2017-03-01",
        "amount": 630.12
      },
      {
        "paymentDate": "2017-04-01",
        "amount": 630.12
      },
      {
        "paymentDate": "2017-05-01",
        "amount": 630.12
      }
    ]
  },
  {
    "initialPayment": 50,
    "amountToPay": 5000,
    "instalmentBalance": 4950,
    "totalInterestCharged": 102.71,
    "totalPayable": 5102.71,
    "instalments": [
      {
        "paymentDate": "2016-10-01",
        "amount": 561.41
      },
      {
        "paymentDate": "2016-11-01",
        "amount": 561.41
      },
      {
        "paymentDate": "2016-12-01",
        "amount": 561.41
      },
      {
        "paymentDate": "2017-01-01",
        "amount": 561.41
      },
      {
        "paymentDate": "2017-02-01",
        "amount": 561.41
      },
      {
        "paymentDate": "2017-03-01",
        "amount": 561.41
      },
      {
        "paymentDate": "2017-04-01",
        "amount": 561.41
      },
      {
        "paymentDate": "2017-05-01",
        "amount": 561.41
      },
      {
        "paymentDate": "2017-06-01",
        "amount": 561.41
      }
    ]
  },
  {
    "initialPayment": 50,
    "amountToPay": 5000,
    "instalmentBalance": 4950,
    "totalInterestCharged": 114.01,
    "totalPayable": 5114.01,
    "instalments": [
      {
        "paymentDate": "2016-10-01",
        "amount": 506.4
      },
      {
        "paymentDate": "2016-11-01",
        "amount": 506.4
      },
      {
        "paymentDate": "2016-12-01",
        "amount": 506.4
      },
      {
        "paymentDate": "2017-01-01",
        "amount": 506.4
      },
      {
        "paymentDate": "2017-02-01",
        "amount": 506.4
      },
      {
        "paymentDate": "2017-03-01",
        "amount": 506.4
      },
      {
        "paymentDate": "2017-04-01",
        "amount": 506.4
      },
      {
        "paymentDate": "2017-05-01",
        "amount": 506.4
      },
      {
        "paymentDate": "2017-06-01",
        "amount": 506.4
      },
      {
        "paymentDate": "2017-07-01",
        "amount": 506.4
      }
    ]
  },
  {
    "initialPayment": 50,
    "amountToPay": 5000,
    "instalmentBalance": 4950,
    "totalInterestCharged": 125.69,
    "totalPayable": 5125.69,
    "instalments": [
      {
        "paymentDate": "2016-10-01",
        "amount": 461.42
      },
      {
        "paymentDate": "2016-11-01",
        "amount": 461.42
      },
      {
        "paymentDate": "2016-12-01",
        "amount": 461.42
      },
      {
        "paymentDate": "2017-01-01",
        "amount": 461.42
      },
      {
        "paymentDate": "2017-02-01",
        "amount": 461.42
      },
      {
        "paymentDate": "2017-03-01",
        "amount": 461.42
      },
      {
        "paymentDate": "2017-04-01",
        "amount": 461.42
      },
      {
        "paymentDate": "2017-05-01",
        "amount": 461.42
      },
      {
        "paymentDate": "2017-06-01",
        "amount": 461.42
      },
      {
        "paymentDate": "2017-07-01",
        "amount": 461.42
      },
      {
        "paymentDate": "2017-08-01",
        "amount": 461.42
      }
    ]
  },
  {
    "initialPayment": 50,
    "amountToPay": 5000,
    "instalmentBalance": 4950,
    "totalInterestCharged": 137.37,
    "totalPayable": 5137.37,
    "instalments": [
      {
        "paymentDate": "2016-10-01",
        "amount": 423.94
      },
      {
        "paymentDate": "2016-11-01",
        "amount": 423.94
      },
      {
        "paymentDate": "2016-12-01",
        "amount": 423.94
      },
      {
        "paymentDate": "2017-01-01",
        "amount": 423.94
      },
      {
        "paymentDate": "2017-02-01",
        "amount": 423.94
      },
      {
        "paymentDate": "2017-03-01",
        "amount": 423.94
      },
      {
        "paymentDate": "2017-04-01",
        "amount": 423.94
      },
      {
        "paymentDate": "2017-05-01",
        "amount": 423.94
      },
      {
        "paymentDate": "2017-06-01",
        "amount": 423.94
      },
      {
        "paymentDate": "2017-07-01",
        "amount": 423.94
      },
      {
        "paymentDate": "2017-08-01",
        "amount": 423.94
      },
      {
        "paymentDate": "2017-09-01",
        "amount": 423.94
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
