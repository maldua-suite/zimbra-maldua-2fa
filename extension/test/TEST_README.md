# Zimbra 2FA Extension - Unit Tests

## Overview

This directory contains unit tests for the Zimbra 2FA Extension. The tests are organized to mirror the source code structure and provide comprehensive coverage of critical components.

## Test Structure

```
test/java/com/btactic/twofactorauth/
├── core/
│   └── EmailCodeParserTest.java          # Tests for email code parsing logic
├── credentials/
│   └── CredentialGeneratorTest.java      # Tests for credential generation
└── exception/
    └── TwoFactorExceptionTest.java       # Tests for custom exception classes
```

## Test Coverage

### Current Test Coverage (Phase 4)

| Component | Test Class | Coverage | Notes |
|-----------|------------|----------|-------|
| EmailCodeParser | EmailCodeParserTest | High | Covers parsing, validation, expiration |
| CredentialGenerator | CredentialGeneratorTest | High | Covers generation, encoding, uniqueness |
| Exception Classes | TwoFactorExceptionTest | High | All 6 custom exceptions tested |

### Components Needing Tests

Future test development should focus on:

1. **ZetaTwoFactorAuth** - Core 2FA management
   - TOTP authentication
   - Email code validation
   - Credential management

2. **ZetaScratchCodes** - Scratch code management
   - Code generation
   - Validation and invalidation
   - Storage operations

3. **ZetaAppSpecificPasswords** - App password management
   - Password generation
   - Authentication
   - Expiration handling

4. **ZetaTrustedDevices** - Trusted device management
   - Device registration
   - Token validation
   - Device revocation

5. **EnableTwoFactorAuth** - SOAP handler
   - Setup workflow
   - Verification workflow
   - Error handling

## Running Tests

### Prerequisites

- JUnit 4.12 or higher
- Mockito 2.x or higher
- Zimbra SDK libraries
- Java 17

### Build Configuration

Add to your `build.xml` (Ant) or `pom.xml` (Maven):

#### Ant Configuration

```xml
<target name="test" depends="compile-tests">
    <junit printsummary="yes" haltonfailure="no">
        <classpath>
            <path refid="test.classpath"/>
        </classpath>
        <formatter type="plain"/>
        <batchtest fork="yes" todir="${test.reports}">
            <fileset dir="${test.dir}">
                <include name="**/*Test.java"/>
            </fileset>
        </batchtest>
    </junit>
</target>
```

#### Maven Configuration

```xml
<dependencies>
    <!-- JUnit -->
    <dependency>
        <groupId>junit</groupId>
        <artifactId>junit</artifactId>
        <version>4.13.2</version>
        <scope>test</scope>
    </dependency>

    <!-- Mockito -->
    <dependency>
        <groupId>org.mockito</groupId>
        <artifactId>mockito-core</artifactId>
        <version>3.12.4</version>
        <scope>test</scope>
    </dependency>
</dependencies>
```

### Running Tests

```bash
# Compile tests
ant compile-tests

# Run all tests
ant test

# Run specific test class
ant test -Dtest.class=EmailCodeParserTest

# Generate coverage report (with JaCoCo)
ant test-coverage
```

## Test Guidelines

### Writing New Tests

1. **Follow naming conventions**:
   - Test classes: `<ClassName>Test.java`
   - Test methods: `test<MethodName>_<Scenario>()`

2. **Use proper annotations**:
   ```java
   @Before
   public void setUp() { /* initialize test fixtures */ }

   @Test
   public void testMethodName_Scenario() { /* test logic */ }

   @Test(expected = ExpectedException.class)
   public void testMethodName_ThrowsException() { /* test exception */ }
   ```

3. **Mock external dependencies**:
   ```java
   @Mock
   private Account mockAccount;

   @Before
   public void setUp() {
       MockitoAnnotations.initMocks(this);
       when(mockAccount.getName()).thenReturn("test@example.com");
   }
   ```

4. **Test edge cases**:
   - Null inputs
   - Empty strings
   - Boundary values
   - Invalid formats
   - Expired data

5. **Verify behavior**:
   - Use `assertEquals()` for value checks
   - Use `assertTrue()`/`assertFalse()` for boolean checks
   - Use `assertNotNull()` for null checks
   - Use `verify()` for interaction checks (Mockito)

### Test Organization

Each test class should test one component and include:

1. **Setup** - Initialize mocks and test data
2. **Happy path tests** - Normal operation
3. **Edge case tests** - Boundary conditions
4. **Error tests** - Exception handling
5. **Integration tests** - Component interaction

## Test Data

### Sample Test Constants

```java
private static final String TEST_ACCOUNT = "user@example.com";
private static final String TEST_CODE = "123456";
private static final long TEST_TIMESTAMP = System.currentTimeMillis();
private static final long ONE_HOUR_MS = 3600000;
```

### Mock Account Setup

```java
@Before
public void setUp() {
    MockitoAnnotations.initMocks(this);
    when(mockAccount.getName()).thenReturn(TEST_ACCOUNT);
    when(mockAccount.getId()).thenReturn("account-id-123");
    when(mockAccount.isFeatureTwoFactorAuthAvailable()).thenReturn(true);
}
```

## Coverage Goals

### Phase 4 Targets

- **Overall coverage**: 70%+
- **Core components**: 80%+
- **Utility classes**: 90%+
- **Exception handling**: 100%

### Priority Testing Order

1. **Critical Path** (High Priority):
   - Credential generation (CredentialGenerator)
   - Authentication logic (ZetaTwoFactorAuth.authenticate)
   - Exception handling (all custom exceptions)

2. **Important Components** (Medium Priority):
   - Email code parsing (EmailCodeParser)
   - Scratch code management (ZetaScratchCodes)
   - App-specific passwords (ZetaAppSpecificPasswords)

3. **Supporting Components** (Lower Priority):
   - Trusted devices (ZetaTrustedDevices)
   - SOAP handlers (EnableTwoFactorAuth)
   - Utility classes (TwoFactorAuthUtils)

## Continuous Integration

### Recommended CI Pipeline

```yaml
# Example GitHub Actions workflow
name: Tests

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - name: Set up JDK 17
        uses: actions/setup-java@v2
        with:
          java-version: '17'
      - name: Run tests
        run: ant test
      - name: Generate coverage
        run: ant test-coverage
      - name: Upload coverage
        uses: codecov/codecov-action@v2
```

## Best Practices

1. **Fast tests**: Keep unit tests fast (<100ms each)
2. **Isolated tests**: No test should depend on another
3. **Deterministic tests**: Same input = same output
4. **Clear assertions**: Make expected vs actual obvious
5. **Meaningful names**: Test name should describe scenario

## Troubleshooting

### Common Issues

**Issue**: `java.lang.NoClassDefFoundError: org/mockito/Mockito`
**Solution**: Add Mockito to test classpath

**Issue**: `Account cannot be mocked`
**Solution**: Use `@Mock` annotation and `MockitoAnnotations.initMocks(this)`

**Issue**: Tests pass locally but fail in CI
**Solution**: Check for timing-dependent tests, use fixed timestamps

## Resources

- [JUnit 4 Documentation](https://junit.org/junit4/)
- [Mockito Documentation](https://site.mockito.org/)
- [Zimbra SDK Documentation](https://www.zimbra.com/docs/)

## Contributing

When adding new tests:

1. Follow existing test patterns
2. Maintain high coverage
3. Document complex test scenarios
4. Update this README if adding new test categories
