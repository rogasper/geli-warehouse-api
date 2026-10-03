# Warehouse API

REST API for managing a small shop's warehouse: items, variants, prices, and stock.

Built with Java 21, Spring Boot 3.5, Spring Data JPA, and SQLite.

> Work in progress: the API, tests, and documentation are being built step by step.

## Requirements

- JDK 17 or newer (developed and tested on JDK 21)
- No local Maven needed: the Maven wrapper (`./mvnw`) is included

## How to run

From the project root:

```bash
./mvnw spring-boot:run
```

The app starts on <http://localhost:8080>, creates the SQLite database file
`warehouse.db` in the project root, and applies `sql/01-schema.sql` and
`sql/02-seed.sql` automatically.

If `JAVA_HOME` is not set, point it at a JDK 17+ first, for example:

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
```

On this machine a `Makefile` is included which picks the right JDK automatically:

```bash
make run    # start the app
make test   # run the test suite
```

## API

_To be documented (endpoints, examples, error format)._

## Design decisions

_To be documented._

## Assumptions

_To be documented._

## Testing

_To be documented._
