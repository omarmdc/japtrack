# JapTrack Development Guide

## Project Purpose

JapTrack is a portfolio-quality full-stack application for managing job and internship applications.

The goal is not to generate the maximum amount of code or features. The goal is to build a clean, professional, maintainable, secure, and explainable application that could reasonably resemble a small production SaaS product.

The final application should not look or feel AI-generated.

## Developer Workflow

- Never commit or push changes unless explicitly instructed.
- Do not make unrelated changes while completing a task.
- Prefer small, focused changes over large rewrites.
- Before making a substantial architectural change, explain the proposed approach first.
- Preserve working existing behavior unless the task specifically requires changing it.
- Do not refactor working code merely because another style is possible.
- Run the relevant tests/build after making code changes.
- Do not claim a task is complete if the project does not compile or relevant tests fail.
- If requirements are ambiguous, ask rather than inventing major product behavior.

## Backend Technology

- Java 17
- Spring Boot
- Maven
- Spring Web / REST
- Spring Data JPA
- Spring Security
- Bean Validation
- PostgreSQL for the finished production application
- H2 may be used only for development/testing when appropriate

## Backend Architecture

Maintain a clear layered architecture:

Controller -> Service -> Repository -> Database

### Controllers
- Controllers handle HTTP concerns only.
- Do not place business logic in controllers.
- Validate request DTOs.
- Return appropriate HTTP status codes.
- Do not expose JPA entities directly.

### Services
- Business logic belongs in the service layer.
- Services enforce ownership and domain rules.
- Use transactions where appropriate.
- Avoid duplicated business logic.

### Repositories
- Repositories are responsible only for persistence/query operations.
- Do not place business logic in repositories.

### DTOs
- Use request/response DTOs rather than exposing entities.
- Prefer separate DTOs when create, update, authentication, or response requirements differ.
- Validate incoming DTOs using Jakarta Bean Validation.
- Never expose password hashes or other sensitive information.

## Security

Security must be treated as a first-class requirement.

- Never store plaintext passwords.
- Passwords must be hashed using a secure PasswordEncoder such as BCrypt.
- Never log passwords, tokens, credentials, or secrets.
- Never hard-code secrets or production credentials.
- Users may only access or modify resources they own unless explicitly designed otherwise.
- Do not trust userId supplied by the frontend for authorization.
- Derive the authenticated user from the security context.
- Authentication failures should not reveal whether a particular account exists.
- Password changes should use a dedicated secure flow rather than a general profile PATCH.
- Development-only tools such as the H2 console must never be exposed in production.

## REST API

- Use consistent resource-oriented routes.
- Return appropriate HTTP status codes.
- Use 201 for successful creation.
- Prefer 204 for successful deletion when no response body is required.
- Return 400 for invalid client input.
- Return 401 for unauthenticated requests.
- Return 403 for authenticated users attempting unauthorized actions.
- Return 404 for missing resources.
- Return 409 for genuine resource conflicts.
- Use a consistent structured JSON error response.
- Do not convert normal client errors into generic 500 responses.

## Database

- The finished application must use persistent storage.
- Production configuration must not use an in-memory database.
- Separate development/test/production configuration where appropriate.
- Do not commit real credentials.
- Prefer migrations such as Flyway once the database schema becomes production-oriented.

## Testing

New important behavior should include appropriate tests.

Prioritize tests for:

- authentication
- authorization and ownership
- validation
- service/domain logic
- REST status codes
- repository queries
- regression bugs

Tests should verify behavior rather than simply increasing coverage numbers.

## Git

- Never automatically commit.
- Never automatically push.
- Never force-push.
- Never rewrite Git history unless explicitly instructed.
- Keep changes scoped to the current task.
- Before major work, confirm the working tree is clean or clearly report existing changes.

## Frontend Direction

The frontend will be a modern TypeScript application consuming the Spring Boot REST API.

Current intended stack:
- React
- TypeScript
- Vite
- Tailwind CSS
- customized shadcn/ui components
- TanStack Query
- React Hook Form
- Zod where appropriate

Do not begin frontend implementation until the backend authentication/API contract is stable and the frontend architecture is explicitly approved.

## UI / UX Principles

JapTrack should look intentionally designed rather than generated from a generic AI dashboard prompt.

Avoid:
- unnecessary gradients
- excessive glassmorphism
- oversized hero text inside authenticated application screens
- excessive rounded cards
- random decorative elements
- inconsistent spacing
- generic "Welcome back" dashboard clichés unless specifically designed
- blindly using default shadcn styling everywhere

Prefer:
- strong information hierarchy
- restrained visual design
- consistent spacing and typography
- clear application status indicators
- accessible components
- responsive behavior
- useful empty/loading/error states
- intentional tables, filters, forms, and navigation
- reusable components
- a cohesive JapTrack visual identity

AI should implement the agreed design, not invent the product's visual identity independently.

## Code Quality

- Prefer readable code over clever code.
- Use descriptive names.
- Avoid unnecessary abstractions.
- Avoid premature optimization.
- Remove dead code when directly relevant to the task.
- Do not introduce duplicate logic.
- Do not add dependencies without a clear reason.
- Prefer constructor injection over field injection.
- Keep methods focused.
- Follow the conventions already established by the project unless there is a reason to improve them.

## Change Discipline

Before implementing a major feature:
1. Inspect the relevant existing code.
2. Explain the proposed implementation.
3. Identify which files need to change.
4. Identify security/data-model implications.
5. Implement only after the approach is understood or approved.

After implementation:
1. Compile/build the project.
2. Run relevant tests.
3. Review the resulting diff.
4. Report exactly what changed.
5. Report any remaining concerns or failures.

## Primary Goal

Optimize for a professional, secure, understandable portfolio application—not for generating the most code.

The developer should be able to understand and explain important architecture and implementation decisions in a technical interview.
