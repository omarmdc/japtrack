# japtrack
An application to help individuals organize and manage their job and internship applications in one centralized platform.

## Database setup

JapTrack uses PostgreSQL 18+ for local development and production. H2 is used only by the automated tests.

### Local PostgreSQL

Create a database named `japtrack` and an application role named `japtrack_app` that owns it, so the role
can create and change tables when migrations run.

### Environment variables

The application reads its database connection only from environment variables. None of them have defaults,
so the application will not start until all three are set.

| Variable      | Description                       | Example                                    |
|---------------|-----------------------------------|--------------------------------------------|
| `DB_URL`      | JDBC connection URL               | `jdbc:postgresql://localhost:5432/japtrack` |
| `DB_USERNAME` | Database role                     | `japtrack_app`                             |
| `DB_PASSWORD` | Password for that role            | (never commit this value)                  |

For local development, set them in your IDE run configuration or as user environment variables.
If `DB_URL` is missing, startup fails with `'url' must start with "jdbc"`.

In production, also set `SPRING_PROFILES_ACTIVE=prod`. The `prod` profile marks the session cookie as
`Secure`, so it is only sent over HTTPS.

### Schema management

Flyway owns the database schema. Migrations live in `src/main/resources/db/migration` and run automatically
at startup. Applied migrations are never edited; every schema change is a new versioned file.

Hibernate never changes the schema in PostgreSQL environments (`ddl-auto=validate`): it only checks that the
entities match the migrated tables, and the application refuses to start if they don't. The tests use an
in-memory H2 database with `ddl-auto=create-drop` and Flyway disabled.

## Running

With the environment variables set:

```
./mvnw spring-boot:run
```

## Running the tests

The tests use the `test` profile and in-memory H2, so they need neither PostgreSQL nor the database
environment variables:

```
./mvnw test
```

On Windows, use `mvnw.cmd` in place of `./mvnw`.
