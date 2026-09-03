# Customer Rewards API

A Spring Boot REST service that calculates the reward points a retailer awards its
customers, broken down per calendar month and in total, over any time frame the caller
asks for.

---

## Table of contents

1. [The reward rule](#the-reward-rule)
2. [Design](#design)
3. [Technical details](#technical-details)
4. [Running the application](#running-the-application)
5. [API reference](#api-reference)
6. [Demo data set](#demo-data-set)
7. [Testing](#testing)
8. [Design decisions](#design-decisions)

---

## The reward rule

A customer earns:

* **2 points** for every whole dollar spent **over $100** in a transaction, plus
* **1 point** for every whole dollar spent **between $50 and $100** in a transaction.

Expressed as a formula, where `d` is the transaction amount truncated to whole dollars:

```
points = 2 x max(0, d - 100)  +  1 x max(0, min(d, 100) - 50)
```

Worked example from the requirements — a **$120** purchase:

```
2 x $20  (the amount above $100)   =  40
1 x $50  (the amount from $50-$100) =  50
                                      ---
                                       90 points
```

Partial dollars do not earn points, so the amount is **truncated, not rounded**. A $50.99
purchase earns the same as a $50.00 purchase: nothing. Refunds and zero-value transactions
earn nothing rather than producing negative points.

| Amount | Points | Why |
|---|---|---|
| $49.99 | 0 | below the lower threshold |
| $50.00 | 0 | at the threshold, nothing above it |
| $50.99 | 0 | truncated to $50 |
| $51.00 | 1 | one whole dollar above $50 |
| $100.00 | 50 | the full lower tier |
| $120.00 | **90** | the worked example |
| $999.99 | 1848 | `2 x 899 + 50` |

---

## Design

### Layering

```
HTTP  ─►  Controller  ─►  Service  ─►  Repository  ─►  H2 (in-memory)
             │              │
             │              ├─ RewardCalculator    (the business rule, pure)
             │              └─ DateRangeResolver   (defaulting + validation)
             │
             └─ GlobalExceptionHandler  ─►  uniform ErrorResponse
```

Each layer has one job. Controllers bind and validate the request; services own the
business logic and the transaction boundary; repositories only talk to the database. DTOs
are returned to the caller rather than JPA entities, so the persistence model can change
without breaking the API contract.

### Package structure

```
com.retailer.rewards
├── RewardsApplication.java        entry point
├── config/                        RewardProperties, AsyncConfig, ClockConfig,
│                                  OpenApiConfig, DataSeeder
├── controller/                    RewardController, CustomerController
├── service/                       RewardService (+Impl), AsyncRewardService,
│                                  CustomerService, RewardCalculator,
│                                  DateRangeResolver, DateRange
├── repository/                    CustomerRepository, TransactionRepository
├── entity/                        Customer, Transaction
├── dto/                           CustomerRewardResponse, MonthlyRewardSummary,
│                                  TransactionDetail, RewardSummary, CustomerResponse,
│                                  PagedResponse, ErrorResponse
└── exception/                     CustomerNotFoundException, InvalidDateRangeException,
                                   GlobalExceptionHandler
```

### Request flow

1. `RewardController` binds `customerId`, `startDate` and `endDate`, and validates them.
2. `DateRangeResolver` fills in whatever the caller omitted and rejects unusable ranges.
3. `RewardServiceImpl` loads the customer (404 if absent) and their transactions in range.
4. `RewardCalculator` scores each transaction.
5. The service folds those scores into a monthly breakdown, a grand total and a set of
   spending statistics, and returns a `CustomerRewardResponse`.

---

## Technical details

| Concern | Choice |
|---|---|
| Language | Java (source/target 21, written in a Java 8 compatible style — see [Design decisions](#design-decisions)) |
| Framework | Spring Boot 3.5.6 (Spring MVC, Spring Data JPA, Bean Validation) |
| Build | Maven |
| Database | H2, in-memory, seeded at start up |
| Money | `BigDecimal` throughout — never `double` |
| Dates | `java.time` (`LocalDate`, `YearMonth`), injected `Clock` for testability |
| Async | `@Async` on a dedicated bounded `ThreadPoolTaskExecutor`, returning `CompletableFuture` |
| Docs | springdoc-openapi (Swagger UI) |
| Logging | SLF4J + Logback, console and rolling file. No `System.out` anywhere |
| Tests | JUnit 5, Mockito, AssertJ, MockMvc; JaCoCo for coverage |

---

## Running the application

Requires JDK 21+ and Maven 3.9+.

```bash
mvn spring-boot:run
```

The service starts on <http://localhost:8080>.

| Resource | URL |
|---|---|
| Swagger UI | <http://localhost:8080/swagger-ui.html> |
| OpenAPI document | <http://localhost:8080/v3/api-docs> |
| H2 console | <http://localhost:8080/h2-console> (JDBC URL `jdbc:h2:mem:rewardsdb`, user `sa`, no password) |

Build a runnable jar with `mvn clean package`. Compiled artefacts are **not** committed —
`target/` and all binaries are excluded by `.gitignore`.

---

## API reference

### `GET /api/v1/rewards/customers/{customerId}`

Reward points for one customer.

| Parameter | In | Required | Description |
|---|---|---|---|
| `customerId` | path | yes | Positive customer identifier |
| `startDate` | query | no | Inclusive start, `yyyy-MM-dd`. Defaults to the first day of the three month window |
| `endDate` | query | no | Inclusive end, `yyyy-MM-dd`. Defaults to today |

Both dates are optional, so the endpoint answers the "three month period" requirement out
of the box while remaining usable for any period.

```bash
curl "http://localhost:8080/api/v1/rewards/customers/1"
curl "http://localhost:8080/api/v1/rewards/customers/1?startDate=2026-07-01&endDate=2026-09-30"
```

**200 OK** (actual response from the seeded data set):

```json
{
  "customerId": 1,
  "customerName": "Alice Johnson",
  "email": "alice.johnson@example.com",
  "memberSince": "2021-03-18",
  "periodStart": "2026-07-01",
  "periodEnd": "2026-09-02",
  "totalPoints": 884,
  "monthlyBreakdown": [
    { "period": "2026-07", "month": "JULY",      "monthNumber": 7, "year": 2026, "transactionCount": 3, "amountSpent": 240.50, "pointsEarned": 115 },
    { "period": "2026-08", "month": "AUGUST",    "monthNumber": 8, "year": 2026, "transactionCount": 2, "amountSpent": 299.99, "pointsEarned": 299 },
    { "period": "2026-09", "month": "SEPTEMBER", "monthNumber": 9, "year": 2026, "transactionCount": 2, "amountSpent": 360.25, "pointsEarned": 470 }
  ],
  "summary": {
    "totalTransactions": 7,
    "totalAmountSpent": 900.74,
    "averageTransactionAmount": 128.68,
    "highestTransactionAmount": 310.25,
    "monthsCovered": 3,
    "monthsWithActivity": 3,
    "firstTransactionDate": "2026-07-06",
    "lastTransactionDate": "2026-09-02"
  },
  "transactions": [
    { "transactionId": 1, "transactionDate": "2026-07-06", "amount": 120.00, "pointsEarned": 90,  "description": "Electronics" },
    { "transactionId": 2, "transactionDate": "2026-07-17", "amount": 75.50,  "pointsEarned": 25,  "description": "Groceries" },
    { "transactionId": 3, "transactionDate": "2026-07-25", "amount": 45.00,  "pointsEarned": 0,   "description": "Books" },
    { "transactionId": 4, "transactionDate": "2026-08-04", "amount": 200.00, "pointsEarned": 250, "description": "Home appliance" },
    { "transactionId": 5, "transactionDate": "2026-08-21", "amount": 99.99,  "pointsEarned": 49,  "description": "Clothing" },
    { "transactionId": 6, "transactionDate": "2026-09-02", "amount": 310.25, "pointsEarned": 470, "description": "Furniture" },
    { "transactionId": 7, "transactionDate": "2026-09-02", "amount": 50.00,  "pointsEarned": 0,   "description": "Pharmacy" }
  ]
}
```

Months with no activity are still returned, with zero counts, so a client can render a
complete timeline without filling in gaps itself.

### `GET /api/v1/rewards/customers`

The same calculation across every customer. Accepts the same `startDate` / `endDate`
parameters and returns a JSON array. Customers with no activity appear with zero totals.

```bash
curl "http://localhost:8080/api/v1/rewards/customers?startDate=2026-07-01&endDate=2026-09-02"
```

### `GET /api/v1/rewards/customers/{customerId}/async`

Identical payload, produced asynchronously. The request thread is released while the work
runs on the `rewards-async-*` pool behind a configurable simulated downstream delay
(`rewards.async-simulated-latency-ms`, default 750 ms), which stands in for a slow remote
data source.

```bash
curl "http://localhost:8080/api/v1/rewards/customers/3/async"
```

### `GET /api/v1/customers`

Lists every customer with a lifetime transaction count — handy for discovering the ids in
the demo data set.

### `GET /api/v1/customers/{customerId}/transactions`

Paged transaction history, newest first.

| Parameter | Default | Constraint |
|---|---|---|
| `page` | `0` | must not be negative |
| `size` | `20` | 1 to 100 |

### Error responses

Every failure returns the same shape.

| Status | Cause |
|---|---|
| `400 Bad Request` | malformed date, inverted range, future date, range wider than 24 months, non-positive id, invalid paging |
| `404 Not Found` | unknown customer |
| `500 Internal Server Error` | unexpected failure; details are logged, never returned |

```json
{
  "timestamp": "2026-09-02T16:50:58",
  "status": 404,
  "error": "Not Found",
  "message": "No customer found with id 9999",
  "path": "/api/v1/rewards/customers/9999"
}
```

```json
{
  "timestamp": "2026-09-02T16:50:58",
  "status": 400,
  "error": "Bad Request",
  "message": "Parameter 'startDate' has an invalid value '01-07-2026'. Expected LocalDate in yyyy-MM-dd format.",
  "path": "/api/v1/rewards/customers/1"
}
```

---

## Demo data set

Six customers and twenty-two transactions are loaded into H2 at start up by `DataSeeder`.
The dates are anchored to the **current month** rather than to fixed calendar dates, so the
default window always has data no matter when the application is run.

The data is chosen to exercise every branch of the rule:

| Customer | What it demonstrates | Points in the default window |
|---|---|---|
| Alice Johnson | steady activity in all three months, including the $120 worked example | 884 |
| Brian Chen | a $500 purchase four months ago that must be **excluded** from the window | 258 |
| Carla Mendes | the threshold edges ($49.99, $50.00, $50.99, $51.00), a month with no activity, and a $999.99 purchase | 1849 |
| Daniel O'Neill | high-value purchases plus one that earns nothing | 2200 |
| Emily Watson | a customer with **no transactions at all** | 0 |
| Frank Miller | activity only in the current month | 115 |

---

## Testing

```bash
mvn test
```

### Results

```
[INFO] Tests run:  5, Failures: 0, Errors: 0, Skipped: 0 -- CustomerControllerTest
[INFO] Tests run:  9, Failures: 0, Errors: 0, Skipped: 0 -- RewardControllerTest
[INFO] Tests run: 13, Failures: 0, Errors: 0, Skipped: 0 -- RewardsApiIntegrationTest
[INFO] Tests run:  3, Failures: 0, Errors: 0, Skipped: 0 -- AsyncRewardServiceTest
[INFO] Tests run: 10, Failures: 0, Errors: 0, Skipped: 0 -- DateRangeResolverTest
[INFO] Tests run: 25, Failures: 0, Errors: 0, Skipped: 0 -- RewardCalculatorTest
[INFO] Tests run:  9, Failures: 0, Errors: 0, Skipped: 0 -- RewardServiceImplTest
[INFO]
[INFO] Tests run: 74, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

A JaCoCo coverage report is written to `target/site/jacoco/index.html`.

### What is covered

| Suite | Scope | Scenarios |
|---|---|---|
| `RewardCalculatorTest` | the rule in isolation | 19 parameterised amounts across both thresholds, null, refunds, overflow guard, reconfigured thresholds |
| `DateRangeResolverTest` | defaulting and validation | default window, partial input, explicit range, single day, inverted range, future dates, oversized range |
| `RewardServiceImplTest` | aggregation, repositories mocked | monthly grouping, totals, spending summary, empty months, no transactions, unknown customer, validation short-circuit, bulk report, and a spy asserting the rule runs exactly once per transaction |
| `AsyncRewardServiceTest` | the async wrapper, delegate mocked | pass-through payload, period forwarding, failure propagation |
| `RewardControllerTest` | HTTP layer, service mocked | JSON contract, parameter binding, 400/404 mapping, malformed dates, async dispatch |
| `CustomerControllerTest` | HTTP layer | listing, paging defaults, paging limits, 404 |
| `RewardsApiIntegrationTest` | full context + real H2 | end-to-end totals against the seeded data, window filtering, threshold edges, inactive customer, async endpoint, error paths |

---

## Design decisions

**Optional date range rather than a required one.** The requirement is a three month
period, but hard-coding that would make the endpoint useless for anything else. Making both
dates optional gives the three month behaviour by default and an arbitrary window when the
caller wants one, from a single endpoint.

**Truncate partial dollars.** The rule pays "per dollar spent", so a partial dollar has not
earned anything. `$50.99` therefore scores 0, not 1. Rounding up would award points for
money the customer did not spend.

**`BigDecimal` for money.** `double` cannot represent `0.01` exactly, and reward totals are
customer-visible. Amounts are stored with `precision = 12, scale = 2`.

**Points as `int`, with a guard.** Realistic totals are far inside `int` range. Rather than
let an absurd input overflow silently, `RewardCalculator` rejects amounts above
$1,000,000,000 with a clear message.

**Injected `Clock`.** Anything that defaults a date takes a `Clock`, so "today" can be
pinned in tests and the expectations do not drift with the calendar.

**Externalised thresholds.** The $50/$100 boundaries and the 1x/2x rates live in
`application.yml` under `rewards.*`. A promotional change becomes a config edit, and the
tests can exercise alternative rule sets.

**The async wrapper is a separate bean.** Spring applies `@Transactional` through a proxy,
and a proxy is only consulted for calls arriving from outside the object. Keeping the
asynchronous method inside `RewardServiceImpl` and having it call its own
`calculateRewardsForCustomer` would be a self-invocation: the proxy is skipped, and the
async path would run without the read-only transaction the synchronous path gets.
`AsyncRewardService` calls across a bean boundary, so both paths behave identically — and
the simulated latency happens before the delegate is called, so it never holds a database
connection open.

**Each transaction is scored exactly once.** The per-transaction detail, the monthly
breakdown and the grand total are all derived from one already-scored list rather than each
re-running the calculator over the same rows. A Mockito spy in `RewardServiceImplTest`
asserts the invocation count, so the duplication cannot creep back in.

**Two queries for the bulk endpoint.** Reporting on every customer loads all customers once
and all in-range transactions once, with the customer fetch-joined, then groups in memory.
The database round trips stay constant instead of growing with the customer count.

**Custom `PagedResponse` instead of Spring Data's `Page`.** `Page`'s JSON shape is an
implementation detail that has changed between Spring versions; pinning our own wrapper
keeps the API contract stable.

**Java 21 bytecode, Java 8 style.** The role targets Java 8, and the code stays within Java
8 idioms — streams, `Optional`, `java.time`, explicit types, no records or `var`. It is
compiled at release 21 because Spring Boot 3.x requires Java 17+; the last Java 8 compatible
line, Spring Boot 2.7, reached end of open-source support in 2023. Nothing in the source
depends on a post-Java-8 language feature, so the same code compiles at release 8 under
Spring Boot 2.7 if a Java 8 runtime is a hard requirement.
