# Development Guidelines for DHC Server

## 필수 규칙
모든 응답은 반드시 한국어로 이루어져야 합니다. 영어 또는 기타 언어로의 응답은 허용되지 않습니다.

## Build, Lint, and Test Commands

### Build Commands
```bash
# Build the project
./gradlew build

# Build fat jar for deployment
./gradlew buildFatJar

# Build and run locally
./gradlew run

# Clean build
./gradlew clean build
```

### Lint Commands
```bash
# Run ktlint check
./gradlew ktlintCheck

# Run ktlint format
./gradlew ktlintFormat

# Run ktlint with auto-correction
./gradlew ktlintCheck --continue
```

### Test Commands
```bash
# Run all tests
./gradlew test

# Run a specific test class
./gradlew test --tests "UserServiceTest"

# Run a specific test method
./gradlew test --tests "UserServiceTest.switchTodayMission returns not always same category"

# Run tests with coverage
./gradlew test jacocoTestReport
```

## Code Style Guidelines

### Imports
- Group imports in order: Java standard library, Kotlin standard library, third-party libraries, project-specific imports
- Sort imports alphabetically within each group
- Use star imports sparingly, prefer explicit imports
- Remove unused imports automatically

### Formatting
- Follow KtLint formatting rules (configured in build.gradle.kts)
- Use 4-space indents
- Maximum line width: 120 characters
- No trailing whitespace
- Newline at end of file

### Types
- Prefer `val` over `var` when possible
- Use `data class` for immutable data structures
- Use sealed classes for exhaustive matching
- Prefer `when` expressions over if-else chains when appropriate
- Use `lateinit` for properties that are initialized later
- Use `lazy` for expensive properties that are accessed once

### Naming Conventions
- Use PascalCase for classes and interfaces
- Use camelCase for functions and properties
- Use UPPER_CASE for constants
- Use descriptive names (avoid abbreviations unless widely understood)
- Prefix boolean properties with `is`, `has`, `can` or `should`
- Package names in lowercase

### Error Handling
- Use Kotlin's exception handling mechanisms
- Prefer `try-catch` blocks for recoverable errors
- Use `Result` type for operations that might fail
- Log errors appropriately with context using logger
- Don't catch generic exceptions unless necessary

### Documentation
- Document public APIs with KDoc
- Use meaningful doc comments for functions and classes
- Explain the "why" behind complex logic
- Include parameter descriptions for functions

### Testing
- Follow AAA (Arrange, Act, Assert) pattern for tests
- Use descriptive test names that explain the scenario
- Use mockk for mocking dependencies
- Test edge cases and error conditions
- Use @Test annotation for test methods
- Separate test data setup from test logic
- Use proper assertions (assertEquals, assertTrue, assertFalse)

### Kotlin-Specific Guidelines
- Prefer extension functions for utility methods
- Use infix notation for binary operations
- Leverage Kotlin's null safety features
- Use destructuring declarations when appropriate
- Use Kotlin collections instead of Java equivalents
- Prefer functional programming constructs like map, filter, fold

### Dependencies
- Use dependency injection where appropriate
- Prefer immutable data structures
- Avoid deep nesting in functions
- Keep functions small and focused
- Use coroutines for asynchronous operations

### Git Hooks
- Pre-commit hooks run ktlintCheck automatically
- All code must pass linting before committing

### GitHub Operations
- GitHub 관련 모든 작업은 gh 명령어를 통해 수행합니다
- Pull Request 생성, 이슈 관리, 릴리즈 관리 등은 모두 gh CLI를 사용합니다