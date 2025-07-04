# CLAUDE.md - MorningCat Backend Development Guide

## Role Definition

You are an expert-level software architect with deep expertise in:

- Kotlin
- Ktor framework
- Domain-Driven Design (DDD)
- Hexagonal Architecture (Ports & Adapters)
- CQRS (Command Query Responsibility Segregation)

## Project Overview

**Project Name**: MorningCat (아침형 고양이) Application Backend  
**Development Methodology**: Test-Driven Development (TDD)  
**Architectural References**:

- Eric Evans (Domain-Driven Design)
- Robert C. Martin (Clean Architecture, SOLID)
- Greg Young (CQRS)
- Alistair Cockburn (Hexagonal Architecture)

## Core Principles

### 1. Domain-Driven Design (DDD)

- Model software to match the business domain
- Use Ubiquitous Language throughout the codebase
- Identify and respect Bounded Contexts
- **CRITICAL**: Always reference `UbiquitousLanguage.md` when creating, modifying, or deleting Domain Objects. Update
  this file when adding new domain concepts.

As Eric Evans states: "The heart of software is its ability to solve domain-related problems for its user"

### 2. Hexagonal Architecture

- Keep domain and application layers pure
- Remove dependencies on frameworks or infrastructure details
- **Dependency Rule**: "Source code dependencies can only point inwards" (toward the domain)
- Enable testing in isolation

### 3. CQRS

- Separate data reading (queries) from writing (commands) models
- "You can use a different model to update information than the model you use to read information"
- Optimization goals:
    - Performance
    - Scalability
    - Complexity management

### 4. Test-Driven Development (TDD)

- **Development Order**: Write failing test → Implement code
- **Development Cycle**: Red → Green → Refactor
- Mandatory tests for all business logic
- Use `kotest` with `kotest-assertions-arrow` for Either testing

### 5. Clean Code Principles

- **SOLID**: Single Responsibility, Open-Closed, Liskov Substitution, Interface Segregation, Dependency Inversion
- **DRY**: Don't Repeat Yourself
- **KISS**: Keep It Simple, Stupid
- **YAGNI**: You Aren't Gonna Need It
- Code quality goals:
    - High readability
    - Maintainability
    - Self-documentation

### 6. Type-safe Error Handling

- Use Arrow-kt's Either for predictable business errors
- All use cases and domain services return `Either<ErrorType, SuccessType>`
- Distinguish between:
    - **Contract violations**: Use `require`, `check`, `assert` → System crash
    - **Validation failures**: Use `Either`, `ensure`, `either { }` → Return Left

### 7. Kotlin Coding Conventions (STRICTLY ENFORCED)

#### Forbidden Patterns

These patterns are NEVER acceptable. They represent industry-recognized anti-patterns.

1. **`!!` (not-null assertion) ABSOLUTELY FORBIDDEN**
    - Reason: Causes NullPointerException
    - Reference: JetBrains team reports significant runtime exceptions from `!!` usage
    - Alternatives:
      ```kotlin
      // ❌ NEVER
      val result = someNullableValue!!.doSomething()

      // ✅ ALWAYS
      val result = someNullableValue?.doSomething() ?: defaultValue
      val result = requireNotNull(someNullableValue) { "Clear error message" }
      someNullableValue?.let { value -> value.doSomething() }
      ```

2. **Type Casting (`as`) Minimization**
    - Reason: ClassCastException risk
    - Allowed only: sealed class/when expressions
    - Alternatives:
      ```kotlin
      // ❌ Dangerous
      val string = someValue as String

      // ✅ Safe
      val string = someValue as? String ?: "default"
      if (someValue is String) { someValue.length }  // Smart cast
      ```

3. **`lateinit` Restricted Use**
    - Reason: UninitializedPropertyAccessException
    - Allowed only: DI injection (@Inject), test setup (@Before)
    - Alternatives:
      ```kotlin
      // ❌ Business logic
      lateinit var userPreferences: UserPrefs

      // ✅ Alternatives
      var userPreferences: UserPrefs? = null
      val userPreferences by lazy { loadUserPreferences() }
      ```

4. **Magic Numbers/Strings Forbidden**
    ```kotlin
    // ❌ Forbidden
    if (deliveryHour > 6) { }
    if (contentType == "NEWS") { }

    // ✅ Required
    companion object {
        const val MORNING_DELIVERY_START_HOUR = 6
    }
    enum class ContentType { NEWS, WEATHER, HOROSCOPE }
    ```

5. **Collection Chaining with Sequence**
    ```kotlin
    // ❌ Inefficient
    val result = contents.filter { it.enabled }.map { transform(it) }

    // ✅ Efficient
    val result = contents.asSequence()
        .filter { it.enabled }
        .map { transform(it) }
        .toList()
    ```

6. **Scope Function Nesting Limited to 2 Levels**
7. **Platform Types Must Be Explicitly Handled**

**Violation**: Code review rejection. No merge allowed.

### 8. Domain Object Creation Rules

#### Multi-module Project Structure

```
morningcat-backend/
├── domain/                          # Domain module
│   ├── src/
│   │   ├── main/
│   │   │   └── kotlin/
│   │   │       └── com/morningcat/domain/
│   │   │           ├── shared/
│   │   │           │   ├── valueobject/
│   │   │           │   ├── specification/
│   │   │           │   └── exception/
│   │   │           ├── user/
│   │   │           │   ├── aggregate/
│   │   │           │   ├── entity/
│   │   │           │   ├── valueobject/
│   │   │           │   ├── repository/
│   │   │           │   └── event/
│   │   │           ├── content/
│   │   │           ├── notification/
│   │   │           └── schedule/
│   │   └── test/
│   ├── build.gradle.kts
│   └── README.md
├── application/                     # Application module
│   ├── src/
│   │   ├── main/
│   │   │   └── kotlin/
│   │   │       └── com/morningcat/application/
│   │   │           ├── user/
│   │   │           ├── content/
│   │   │           ├── notification/
│   │   │           └── schedule/
│   │   └── test/
│   ├── build.gradle.kts
│   └── README.md
├── infrastructure/                  # Infrastructure module
│   ├── src/
│   ├── build.gradle.kts
│   └── README.md
├── presentation/                    # Presentation module
│   ├── src/
│   ├── build.gradle.kts
│   └── README.md
├── boot/                           # Bootstrap module
│   ├── src/
│   ├── build.gradle.kts
│   └── README.md
├── common/                         # Common module
│   ├── src/
│   ├── build.gradle.kts
│   └── README.md
├── build.gradle.kts
├── settings.gradle.kts
├── CLAUDE.md
├── UbiquitousLanguage.md
└── detekt.yml
```

#### Module Responsibilities

**Domain Module**

- Business logic
- Entity, Value Object, Aggregate
- Domain Service, Specification
- Repository Interface (no implementation)
- Domain Event

**Application Module**

- Use Case implementation (Command/Query Handler)
- Application Service
- DTO definitions
- Port Interface definitions

**Infrastructure Module**

- Repository implementations
- External API integrations (Weather, News, etc.)
- Database connections
- Email/Push notification services

**Presentation Module**

- Web controllers (Ktor Routes)
- Request/Response DTO
- API documentation
- WebSocket endpoints for real-time updates

**Boot Module**

- Application entry point
- DI configuration (Koin)
- Environment configuration

**Common Module**

- Shared utilities
- Cross-cutting concerns
- Date/Time utilities

#### Naming Conventions

| Type         | Convention                   | Example                             |
|--------------|------------------------------|-------------------------------------|
| Value Object | `{ConceptName}`              | `UserId`, `ContentType`, `TimeZone` |
| Entity       | `{ConceptName}`              | `ContentItem`, `DeliverySchedule`   |
| Aggregate    | `{ConceptName}`              | `User`, `DailyContent`              |
| Repository   | `{Aggregate}Repository`      | `UserRepository`                    |
| Command      | `{Verb}{Target}Command`      | `ScheduleDeliveryCommand`           |
| Query        | `Get{Target}{Criteria}Query` | `GetContentByDateQuery`             |
| Event        | `{Target}{PastVerb}`         | `ContentDelivered`                  |
| DTO          | `{ConceptName}Dto`           | `UserPreferencesDto`                |

## Technical Stack

### Core Technologies

| Category     | Technology                   | Version       |
|:-------------|:-----------------------------|:--------------|
| Language     | Kotlin                       | Latest stable |
| Framework    | Ktor                         | Latest stable |
| Core Library | arrow-kt (core, fx)          | Latest stable |
| ORM          | Exposed                      | Latest stable |
| Build Tool   | Gradle (Kotlin DSL)          | Latest stable |
| DI           | Koin                         | Latest stable |
| Scheduling   | Quartz or kotlinx-coroutines | Latest stable |
| Email        | kotlinx-mail or SendGrid SDK | Latest stable |

### Testing

| Purpose             | Technology              | Notes                  |
|:--------------------|:------------------------|:-----------------------|
| Test Framework      | Kotest                  | Latest stable version  |
| Either Testing      | kotest-assertions-arrow | Arrow-kt matchers      |
| Property-based Test | kotest-property         | Randomized testing     |
| Mocking             | MockK                   | Kotlin mocking library |
| API Testing         | Ktor Test Host          | Integration testing    |

### Data Persistence

| Purpose         | Technology    | Notes                             |
|:----------------|:--------------|:----------------------------------|
| Write Model     | PostgreSQL    | User data, preferences, schedules |
| Read Model      | Redis         | Cached content, delivery status   |
| Content Storage | Cloud Storage | Pre-generated content templates   |

### External Integrations

| Service       | Purpose                     | Provider Options            |
|:--------------|:----------------------------|:----------------------------|
| Weather API   | Current weather, UV index   | OpenWeatherMap, WeatherAPI  |
| News API      | Headlines and summaries     | NewsAPI, Google News        |
| Email Service | Email delivery              | SendGrid, AWS SES           |
| Push Service  | Mobile notifications        | Firebase Cloud Messaging    |
| Calendar API  | Schedule integration        | Google Calendar API         |
| Task API      | Task management integration | Todoist API                 |
| AI Service    | Content generation          | OpenAI API, Claude API      |
| Holiday API   | Holiday information         | Nager.Date, Google Calendar |

### Deployment Environment

- Containerization: Docker
- Cloud Platform: Google Cloud Platform (GCP)
- Orchestration: Cloud Run or GKE
- Scheduled Jobs: Cloud Scheduler + Cloud Functions

### Configuration Management

- **Format**: YAML (application.yaml)
- **Environments**: development, staging, production
- **Location**: `src/main/resources/application.yaml`

#### Configuration Example

```yaml
ktor:
  deployment:
    port: 8080
    host: 0.0.0.0

morningcat:
  database:
    postgres:
      host: localhost
      port: 5432
      database: morningcat
      username: morningcat

  delivery:
    default_time: "07:00"
    timezone: "Asia/Seoul"
    retry_attempts: 3

  content:
    cache_ttl: 3600
    generation_timeout: 30

  integrations:
    weather:
      api_key: ${WEATHER_API_KEY}
      base_url: "https://api.openweathermap.org"
    news:
      api_key: ${NEWS_API_KEY}
      base_url: "https://newsapi.org"
```

**Note**: Environment variables handled in application code, not YAML parsing.

## Development Guidelines

### Test Writing Standards

#### Test Structure Example

**Domain Module Test**

```kotlin
class UserPreferencesTest : StringSpec({
    "Valid delivery time should be accepted" {
        val result = UserPreferences.create(
            deliveryTime = LocalTime.of(7, 0),
            weekendDelivery = true,
            contentCategories = setOf(ContentCategory.NEWS, ContentCategory.WEATHER)
        )

        result.shouldBeRight { preferences ->
            preferences.deliveryTime shouldBe LocalTime.of(7, 0)
            preferences.weekendDelivery shouldBe true
        }
    }

    "Invalid delivery time should return error" {
        val result = UserPreferences.create(
            deliveryTime = LocalTime.of(3, 0), // Too early
            weekendDelivery = true,
            contentCategories = emptySet()
        )

        result.shouldBeLeft { error ->
            error shouldBe ValidationError.InvalidDeliveryTime
        }
    }
})
```

### MorningCat Specific Bounded Contexts

1. **User Context**
    - User registration and authentication
    - Preference management
    - Timezone handling
    - Delivery settings

2. **Content Context**
    - Content type definitions
    - Content generation
    - Content caching
    - Personalization rules

3. **Notification Context**
    - Delivery scheduling
    - Email formatting
    - Push notification handling
    - Retry logic

4. **Schedule Context**
    - Cron job management
    - Holiday detection
    - Timezone calculations
    - Delivery time optimization

5. **Integration Context**
    - External API management
    - Rate limiting
    - Error handling
    - Data transformation

### User Configuration Items

Based on business requirements, users can configure:

**Delivery Settings**

- Preferred delivery time
- Timezone
- Weekend delivery preference
- Holiday delivery preference
- Delivery channel (email/app/both)

**Content Preferences**

- Enabled content categories
- News topics of interest
- Preferred language
- Content detail level (brief/detailed)

**Location Settings**

- City/Region for weather
- Country for holidays
- Preferred units (metric/imperial)

**Integration Settings**

- Connected calendar accounts
- Task management services
- Custom RSS feeds
- AI personalization level

**Notification Settings**

- Email format preference (HTML/plain text)
- Push notification sound
- Quiet hours
- Emergency alert preferences

## Development Process

1. Analyze requirements and model domain
2. Check and update UbiquitousLanguage.md
3. Write failing test (Kotest)
4. Implement minimal code to pass test
5. Refactor for quality
6. Repeat

## AI Agent Required Checks

1. **Current Module Verification**
   ```bash
   pwd  # Verify current module
   ```

2. **Domain Object Work**
   ```bash
   cd domain
   find . -name "*.kt" | grep -i {ClassName}
   ```

3. **Implementation Order**
    1. Domain Module: Entity/Value Object
    2. Application Module: Command/Query Handler
    3. Infrastructure Module: Repository implementation
    4. Presentation Module: Controller
    5. Write tests at each step

4. **Absolute Prohibitions**
    - Domain importing Ktor/external libraries
    - Application referencing infrastructure/presentation
    - Circular module dependencies
    - Duplicate class names
    - Entity/Aggregate in shared folder
    - Implementation without tests

## Critical Reminders

- Encapsulate all business logic in appropriate architecture layers
- Abstract external dependencies with interfaces
- Keep domain layer framework-independent
- Test all Either-returning functions with shouldBeRight/shouldBeLeft
- Clearly distinguish Contract violations from Validation failures
- Always reference UbiquitousLanguage.md for domain work
- Follow the "Dependency Rule" - dependencies point inward
- Apply CQRS: "reading and writing are different"

**Understand and adhere to this multi-module structure and each module's responsibilities.**