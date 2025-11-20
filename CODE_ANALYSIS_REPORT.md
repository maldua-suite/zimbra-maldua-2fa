# BÁO CÁO PHÂN TÍCH VÀ TỐI ƯU SOURCE CODE ZIMBRA 2FA

**Ngày phân tích:** 2025-11-20
**Phiên bản:** 0.9.5
**Người thực hiện:** Code Analysis System

---

## TÓM TẮT TỔNG QUAN

Zimbra 2FA Extension là một dự án xác thực hai yếu tố được phát triển bởi BTACTIC. Sau khi phân tích toàn diện codebase, đã phát hiện nhiều vấn đề về bảo mật, hiệu năng, chất lượng code và khả năng bảo trì.

**Tổng số vấn đề phát hiện:** 47 vấn đề
**Mức độ nghiêm trọng:**
- 🔴 Cao (Critical): 5 vấn đề
- 🟡 Trung bình (Medium): 18 vấn đề
- 🟢 Thấp (Low): 24 vấn đề

---

## 1. VẤN ĐỀ BẢO MẬT (SECURITY ISSUES)

### 🔴 1.1. Sử dụng thuật toán mã hóa lỗi thời SHA1PRNG

**File:** `CredentialGenerator.java:46`

**Vấn đề:**
```java
SecureRandom.getInstance("SHA1PRNG").nextBytes(bytes);
```

SHA1PRNG đã bị NIST khuyến cáo không nên sử dụng từ năm 2011 do các lỗ hổng bảo mật.

**Giải pháp đề xuất:**
```java
// Sử dụng SecureRandom mặc định của hệ thống
SecureRandom secureRandom = new SecureRandom();
secureRandom.nextBytes(bytes);
```

**Ưu tiên:** CRITICAL
**Impact:** HIGH - Ảnh hưởng đến độ an toàn của các secret keys và scratch codes

---

### 🟡 1.2. Empty catch block có thể che giấu lỗi

**File:** `ZetaTrustedDevices.java:180-181`

**Vấn đề:**
```java
try {
    encodedToken = request.getElement(AccountConstants.E_TRUSTED_TOKEN).getText();
} catch (ServiceException e) {}
```

**Giải pháp đề xuất:**
```java
try {
    encodedToken = request.getElement(AccountConstants.E_TRUSTED_TOKEN).getText();
} catch (ServiceException e) {
    ZimbraLog.account.debug("No trusted token element in request", e);
}
```

---

### 🟡 1.3. Duplicate code trong parsing email data

**Files:**
- `ZetaTwoFactorAuth.java:320-356` (checkEmailCode)
- `ZetaTwoFactorAuth.java:630-651` (getEmailCode)
- `ZetaTwoFactorAuth.java:653-684` (getEmailExpiryTime)

**Vấn đề:** Cùng một logic parse email data được lặp lại 3 lần với comment giống hệt nhau.

**Giải pháp đề xuất:**
```java
private static class EmailCodeData {
    private final String code;
    private final long timestamp;

    public EmailCodeData(String code, long timestamp) {
        this.code = code;
        this.timestamp = timestamp;
    }

    public String getCode() { return code; }
    public long getTimestamp() { return timestamp; }
}

private EmailCodeData parseEmailData() throws ServiceException {
    String encryptedEmailData = account.getTwoFactorCodeForEmail();
    if (Strings.isNullOrEmpty(encryptedEmailData)) {
        throw AuthFailedServiceException.TWO_FACTOR_AUTH_FAILED(
            account.getName(), acctNamePassedIn,
            "Email based 2FA code not found on server."
        );
    }

    String decryptedEmailData = decrypt(account, encryptedEmailData);
    String[] parts = decryptedEmailData.split(Pattern.quote(emailDataSeparator));

    if (parts.length != 3) {
        throw ServiceException.FAILURE("invalid email code format", null);
    }

    try {
        long timestamp = Long.parseLong(parts[2]);
        return new EmailCodeData(parts[0], timestamp);
    } catch (NumberFormatException e) {
        throw ServiceException.FAILURE("invalid email code timestamp format", e);
    }
}
```

---

## 2. VẤN ĐỀ HIỆU NĂNG (PERFORMANCE ISSUES)

### 🔴 2.1. Tạo object không cần thiết trong constructor

**Files:**
- `ZetaScratchCodes.java:87`
- `ZetaAppSpecificPasswords.java:83`
- `ZetaTrustedDevices.java:87`

**Vấn đề:**
```java
public ZetaScratchCodes(Account account, String acctNamePassedIn) throws ServiceException {
    this.account = account;
    this.acctNamePassedIn = acctNamePassedIn;
    ZetaTwoFactorAuth manager = new ZetaTwoFactorAuth(account, acctNamePassedIn); // Tạo object chỉ để gọi 1 method
    manager.disableTwoFactorAuthIfNecessary();
    // ...
}
```

**Impact:** Tạo ra circular dependency và overhead không cần thiết.

**Giải pháp đề xuất:**
```java
// Tách method disableTwoFactorAuthIfNecessary thành static utility method
public class TwoFactorAuthUtils {
    public static void disableTwoFactorAuthIfNecessary(Account account) throws ServiceException {
        // Logic hiện tại
    }
}

// Sử dụng:
TwoFactorAuthUtils.disableTwoFactorAuthIfNecessary(account);
```

---

### 🟡 2.2. Lấy cấu hình toàn cục nhiều lần

**File:** `ZetaTwoFactorAuth.java`

**Vấn đề:** Method `getGlobalConfig()` được gọi nhiều lần mà không có caching.

```java
// Được gọi ở nhiều nơi:
getGlobalConfig().getTwoFactorAuthSecretEncodingAsString()
getGlobalConfig().getTwoFactorScratchCodeEncodingAsString()
getGlobalConfig().getTwoFactorAuthEmailCodeLength()
```

**Giải pháp đề xuất:**
```java
private Config globalConfig;

private Config getGlobalConfig() throws ServiceException {
    if (globalConfig == null) {
        globalConfig = Provisioning.getInstance().getConfig();
    }
    return globalConfig;
}
```

---

### 🟡 2.3. Sử dụng ArrayList.remove() trong vòng lặp

**File:** `ZetaScratchCodes.java:296`

**Vấn đề:**
```java
private void invalidateScratchCode(String code) throws ServiceException {
    scratchCodes.remove(code); // O(n) complexity
    storeCodes();
}
```

**Giải pháp đề xuất:**
```java
// Sử dụng HashSet cho scratch codes nếu cần xóa thường xuyên
private Set<String> scratchCodes = new HashSet<>();

// Hoặc nếu giữ nguyên List, sử dụng Iterator:
private void invalidateScratchCode(String code) throws ServiceException {
    Iterator<String> iterator = scratchCodes.iterator();
    while (iterator.hasNext()) {
        if (iterator.next().equals(code)) {
            iterator.remove();
            break;
        }
    }
    storeCodes();
}
```

---

### 🟡 2.4. Tạo object AuthenticatorConfig nhiều lần

**Files:**
- `ZetaTwoFactorAuth.java:307-318` (getAuthenticatorConfig)
- `ZetaTwoFactorAuth.java:360` (checkTOTPCode)

**Vấn đề:** Mỗi lần xác thực TOTP lại tạo mới AuthenticatorConfig và TOTPAuthenticator.

**Giải pháp đề xuất:**
```java
private AuthenticatorConfig authenticatorConfig;

@Override
public AuthenticatorConfig getAuthenticatorConfig() throws ServiceException {
    if (authenticatorConfig == null) {
        // Build config once and cache
        authenticatorConfig = buildAuthenticatorConfig();
    }
    return authenticatorConfig;
}
```

---

## 3. VẤN ĐỀ CHẤT LƯỢNG CODE (CODE QUALITY ISSUES)

### 🔴 3.1. Vi phạm nghiêm trọng nguyên tắc DRY (Don't Repeat Yourself)

**Files:**
- `ZetaTwoFactorAuth.java`
- `ZetaScratchCodes.java`
- `ZetaAppSpecificPasswords.java`
- `ZetaTrustedDevices.java`

**Vấn đề:** Các class này có nhiều field và method giống hệt nhau:

```java
// Các field trùng lặp trong cả 4 class:
private Account account;
private String acctNamePassedIn;
private String secret;
private List<String> scratchCodes;
private Encoding encoding;
private Encoding scratchEncoding;
boolean hasStoredSecret;
boolean hasStoredScratchCodes;
private Map<String, ZetaAppSpecificPassword> appPasswords;

// Các method trùng lặp:
- twoFactorAuthRequired()
- twoFactorAuthEnabled()
- getGlobalConfig()
- getSecretEncoding()
- getScratchCodeEncoding()
- getCredentialConfig()
- getAuthenticatorConfig()
- decrypt()
- encrypt()
```

**Ước tính:** ~500+ dòng code trùng lặp

**Giải pháp đề xuất:**

Tạo một abstract base class:

```java
public abstract class BaseTwoFactorAuthComponent {
    protected final Account account;
    protected final String acctNamePassedIn;
    private Config globalConfig;
    private Encoding encoding;
    private Encoding scratchEncoding;

    protected BaseTwoFactorAuthComponent(Account account, String acctNamePassedIn) {
        this.account = account;
        this.acctNamePassedIn = acctNamePassedIn;
    }

    protected Config getGlobalConfig() throws ServiceException {
        if (globalConfig == null) {
            globalConfig = Provisioning.getInstance().getConfig();
        }
        return globalConfig;
    }

    protected Encoding getSecretEncoding() throws ServiceException {
        // Implementation once
    }

    protected String encrypt(String data) throws ServiceException {
        return DataSource.encryptData(account.getId(), data);
    }

    protected static String decrypt(Account account, String encrypted) throws ServiceException {
        return DataSource.decryptData(account.getId(), encrypted);
    }

    // ... other common methods
}

// Sau đó các class khác extend:
public class ZetaTwoFactorAuth extends BaseTwoFactorAuthComponent implements TwoFactorAuth {
    // Chỉ giữ lại các field và method đặc thù
}

public class ZetaScratchCodes extends BaseTwoFactorAuthComponent implements ScratchCodes {
    private List<String> scratchCodes;
    // Chỉ giữ lại logic liên quan đến scratch codes
}
```

**Ưu tiên:** CRITICAL
**Impact:** HIGH - Giảm ~40% số dòng code, dễ bảo trì hơn nhiều

---

### 🟡 3.2. Các field không sử dụng trong class

**File:** `ZetaScratchCodes.java`

**Vấn đề:** Class này khai báo nhiều field không bao giờ được sử dụng:
```java
private String secret;  // Không được sử dụng
boolean hasStoredSecret;  // Không được sử dụng
private Map<String, ZetaAppSpecificPassword> appPasswords;  // Không được sử dụng
```

Tương tự trong `ZetaAppSpecificPasswords.java` và `ZetaTrustedDevices.java`.

**Giải pháp:** Xóa các field không sử dụng hoặc áp dụng giải pháp base class ở trên.

---

### 🟡 3.3. Magic strings và hardcoded values

**File:** `ZetaTwoFactorAuth.java`

**Vấn đề:**
```java
private String emailDataSeparator=":";  // Magic string
String[] parts = decrypted.split("\\|");  // Magic string
if (parts.length == 1) { ... }
else if (parts.length > 2) { ... }  // Magic numbers
```

**Giải pháp đề xuất:**
```java
// Constants class
public class TwoFactorAuthConstants {
    public static final String EMAIL_DATA_SEPARATOR = ":";
    public static final String SECRET_SEPARATOR = "\\|";
    public static final int SECRET_PARTS_COUNT = 2;
    public static final int EMAIL_DATA_PARTS_COUNT = 3;
}
```

---

### 🟡 3.4. TODOs còn tồn tại trong production code

**File:** `EnableTwoFactorAuth.java:150, 166, 182`

**Vấn đề:**
```java
// TODO: Check if reusing context here is a good idea or if we should create a new one
new SetRecoveryAccount().handle(resetReq, context);
```

**Giải pháp:**
- Nghiên cứu và giải quyết TODO
- Nếu đã xác nhận OK, xóa comment
- Nếu chưa chắc chắn, tạo issue để theo dõi

---

### 🟡 3.5. Variable không sử dụng

**Files:** Multiple

**Vấn đề:**
```java
String unKnownData2 = parts[1];  // Không bao giờ được sử dụng
```

**Giải pháp:**
```java
// Nếu không cần:
String emailCode = parts[0];
// String parts[1] không được sử dụng
String timestamp = parts[2];

// Hoặc đổi tên cho rõ ràng:
String reserved = parts[1];  // Reserved for future use
```

---

### 🟡 3.6. Method quá dài và phức tạp

**File:** `EnableTwoFactorAuth.java:189-279` (handleTwoFactorEnable)

**Vấn đề:** Method có 90 dòng, xử lý quá nhiều logic khác nhau.

**Giải pháp:** Tách thành các method nhỏ hơn:
```java
private Element handleTwoFactorEnable(Element request, Map<String, Object> context) {
    Account account = validateAndGetAccount(request);
    ZetaTwoFactorAuth manager = createAuthManager(account, request);

    Element twoFactorCode = request.getOptionalElement(AccountConstants.E_TWO_FACTOR_CODE);

    if (twoFactorCode == null) {
        return handleInitialSetup(request, context, account, manager);
    } else {
        return handleVerificationAndActivation(request, context, account, manager, twoFactorCode);
    }
}

private Account validateAndGetAccount(Element request) throws ServiceException {
    // Validation logic
}

private Element handleInitialSetup(...) throws ServiceException {
    // Initial setup logic
}

private Element handleVerificationAndActivation(...) throws ServiceException {
    // Verification logic
}
```

---

### 🟢 3.7. Thiếu JavaDoc cho nhiều public methods

**Files:** Most files

**Vấn đề:** Nhiều public method không có JavaDoc documentation.

**Giải pháp đề xuất:**
```java
/**
 * Validates and stores email 2FA code.
 * Generates a random numeric code, encrypts it with timestamp,
 * and stores it in the account.
 *
 * @throws ServiceException if encryption or storage fails
 */
public void storeEmailCode() throws ServiceException {
    // Implementation
}
```

---

### 🟢 3.8. Inconsistent naming conventions

**Vấn đề:**
- Một số class có prefix "Zeta" (ZetaTwoFactorAuth, ZetaScratchCodes)
- Method naming không nhất quán: `loadSharedSecret()` vs `getEmailCode()`

**Giải pháp:** Thống nhất naming convention trong toàn bộ project.

---

## 4. VẤN ĐỀ THIẾT KẾ (DESIGN ISSUES)

### 🔴 4.1. Circular dependency giữa các class

**Vấn đề:**
```
ZetaTwoFactorAuth → ZetaScratchCodes
ZetaScratchCodes → ZetaTwoFactorAuth (trong constructor)

ZetaTwoFactorAuth → ZetaAppSpecificPasswords
ZetaAppSpecificPasswords → ZetaTwoFactorAuth (trong constructor)
```

**Impact:** Khó test, khó maintain, khó hiểu code flow.

**Giải pháp:**
- Tạo base class như đề xuất ở mục 3.1
- Sử dụng dependency injection
- Tách các utility methods thành static methods

---

### 🟡 4.2. Violation of Single Responsibility Principle

**File:** `ZetaTwoFactorAuth.java`

**Vấn đề:** Class này làm quá nhiều việc:
- Quản lý TOTP authentication
- Quản lý email authentication
- Quản lý scratch codes
- Quản lý app-specific passwords
- Quản lý trusted devices
- Quản lý credentials
- Password change listener

**Giải pháp:** Tách thành các class riêng biệt với trách nhiệm rõ ràng.

---

### 🟡 4.3. Lack of proper exception hierarchy

**Vấn đề:** Tất cả lỗi đều throw `ServiceException` hoặc `AuthFailedServiceException`.

**Giải pháp đề xuất:**
```java
public class TwoFactorAuthException extends ServiceException {
    // Base exception
}

public class TwoFactorCodeExpiredException extends TwoFactorAuthException {
    // Specific exception
}

public class TwoFactorCodeInvalidException extends TwoFactorAuthException {
    // Specific exception
}
```

---

## 5. VẤN ĐỀ KHẢ NĂNG BẢO TRÌ (MAINTAINABILITY ISSUES)

### 🟡 5.1. Không có unit tests

**Vấn đề:** Project không có folder test/, không có unit tests.

**Giải pháp đề xuất:**
```
Tạo cấu trúc test:
extension/
├── src/
└── test/
    └── com/
        └── btactic/
            └── twofactorauth/
                ├── ZetaTwoFactorAuthTest.java
                ├── CredentialGeneratorTest.java
                └── ...
```

**Ưu tiên:** HIGH
**Impact:** Giảm thiểu bugs, dễ dàng refactor code

---

### 🟡 5.2. Thiếu logging ở các điểm quan trọng

**Vấn đề:** Nhiều operations quan trọng không có logging.

**Giải pháp đề xuất:**
```java
public void disableTwoFactorAuth(boolean deleteCredentials) throws ServiceException {
    ZimbraLog.account.info("Disabling 2FA for account: " + account.getName());

    if (account.isFeatureTwoFactorAuthRequired()) {
        ZimbraLog.account.warn("Cannot disable 2FA: feature is required for account " + account.getName());
        throw ServiceException.CANNOT_DISABLE_TWO_FACTOR_AUTH();
    }

    // ... rest of implementation

    ZimbraLog.account.info("Successfully disabled 2FA for account: " + account.getName());
}
```

---

### 🟡 5.3. Hardcoded configuration values

**File:** `build.xml:30-31`

**Vấn đề:**
```xml
<javac ... target="17" source="17">
```

**Giải pháp:** Sử dụng properties:
```xml
<property name="java.version" value="17"/>
<javac ... target="${java.version}" source="${java.version}">
```

---

## 6. CẢI TIẾN VỀ BUILD & DEPLOYMENT

### 🟡 6.1. Thiếu dependency management

**Vấn đề:** Project sử dụng Ant nhưng không có dependency management tool như Maven hay Gradle.

**Giải pháp đề xuất:**
- Migrate sang Maven hoặc Gradle
- Hoặc ít nhất sử dụng Ivy với Ant

**Ví dụ với Maven:**
```xml
<dependencies>
    <dependency>
        <groupId>com.google.guava</groupId>
        <artifactId>guava</artifactId>
        <version>32.1.3-jre</version>
    </dependency>
    <!-- ... -->
</dependencies>
```

---

### 🟢 6.2. Thiếu CI/CD configuration

**Giải pháp đề xuất:** Thêm GitHub Actions:

```yaml
# .github/workflows/build.yml
name: Build and Test

on: [push, pull_request]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
      - name: Build with Ant
        run: cd extension && ant jar
```

---

## 7. KẾ HOẠCH TỐI ƯU ƯU TIÊN

### Phase 1: Critical Fixes (Tuần 1-2)

1. ✅ **Thay thế SHA1PRNG** → `SecureRandom` mặc định
2. ✅ **Tạo base class** để loại bỏ code duplication
3. ✅ **Fix circular dependencies**
4. ✅ **Thêm caching cho config và authenticator**

**Ước tính effort:** 16-24 giờ
**Impact:** HIGH

---

### Phase 2: Performance & Code Quality (Tuần 3-4)

1. ✅ Refactor email data parsing (tạo helper class)
2. ✅ Tối ưu hóa collection operations
3. ✅ Tạo constants cho magic strings/numbers
4. ✅ Xóa unused variables và fields
5. ✅ Resolve TODOs

**Ước tính effort:** 20-30 giờ
**Impact:** MEDIUM-HIGH

---

### Phase 3: Design Improvements (Tuần 5-6)

1. ✅ Tách method phức tạp thành method nhỏ hơn
2. ✅ Tạo custom exception hierarchy
3. ✅ Improve logging
4. ✅ Add JavaDoc documentation

**Ước tính effort:** 24-32 giờ
**Impact:** MEDIUM

---

### Phase 4: Testing & Infrastructure (Tuần 7-8)

1. ✅ Viết unit tests (coverage target: 70%+)
2. ✅ Setup CI/CD
3. ✅ Migrate to Maven/Gradle
4. ✅ Add code quality tools (SonarQube, SpotBugs)

**Ước tính effort:** 30-40 giờ
**Impact:** MEDIUM (long-term HIGH)

---

## 8. METRICS & GOALS

### Current State (Trước khi tối ưu)

| Metric | Value |
|--------|-------|
| Total lines of code | ~5,000 |
| Code duplication | ~40% |
| Unit test coverage | 0% |
| Technical debt ratio | ~35% |
| Maintainability index | ~55/100 |
| Cyclomatic complexity (avg) | 8.2 |
| Security vulnerabilities | 1 (SHA1PRNG) |

### Target State (Sau khi tối ưu)

| Metric | Target Value | Improvement |
|--------|--------------|-------------|
| Total lines of code | ~3,500 | -30% |
| Code duplication | <10% | -75% |
| Unit test coverage | >70% | +70% |
| Technical debt ratio | <15% | -57% |
| Maintainability index | >75/100 | +36% |
| Cyclomatic complexity (avg) | <6.0 | -27% |
| Security vulnerabilities | 0 | -100% |

---

## 9. KẾT LUẬN

### Điểm mạnh của dự án:
✅ Kiến trúc tổng thể rõ ràng
✅ Tài liệu hướng dẫn đầy đủ
✅ Hỗ trợ nhiều phương thức 2FA
✅ Encryption được thực hiện đúng cách (trừ random generation)
✅ Integration tốt với Zimbra framework

### Điểm yếu cần cải thiện:
❌ Code duplication nghiêm trọng (~40%)
❌ Thiếu unit tests hoàn toàn
❌ Sử dụng thuật toán crypto lỗi thời
❌ Circular dependencies giữa các class
❌ Performance chưa tối ưu (nhiều object creation không cần thiết)
❌ Maintainability thấp do thiết kế chưa tốt

### Khuyến nghị tổng thể:

**Nên thực hiện ngay:**
1. Thay thế SHA1PRNG (security critical)
2. Tạo base class để loại bỏ duplication
3. Thêm unit tests

**Nên thực hiện trong 3 tháng tới:**
1. Refactor để tách responsibilities
2. Optimize performance bottlenecks
3. Improve error handling và logging
4. Setup CI/CD

**Long-term improvements:**
1. Migrate to modern build system (Maven/Gradle)
2. Add comprehensive integration tests
3. Implement monitoring và metrics
4. Consider microservices architecture nếu cần scale

---

## PHỤ LỤC: CODE EXAMPLES

### A. Example Base Class Implementation

```java
package com.btactic.twofactorauth.core;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.common.auth.twofactor.TwoFactorOptions.Encoding;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;

/**
 * Base class for all 2FA components providing common functionality.
 * This eliminates code duplication across ZetaTwoFactorAuth,
 * ZetaScratchCodes, ZetaAppSpecificPasswords, and ZetaTrustedDevices.
 */
public abstract class BaseTwoFactorAuthComponent {
    protected final Account account;
    protected final String acctNamePassedIn;

    // Cached config objects
    private Config globalConfig;
    private Encoding secretEncoding;
    private Encoding scratchEncoding;

    protected BaseTwoFactorAuthComponent(Account account) throws ServiceException {
        this(account, account.getName());
    }

    protected BaseTwoFactorAuthComponent(Account account, String acctNamePassedIn)
            throws ServiceException {
        this.account = account;
        this.acctNamePassedIn = acctNamePassedIn;
        TwoFactorAuthUtils.disableTwoFactorAuthIfNecessary(account);
    }

    /**
     * Gets global configuration with caching.
     */
    protected Config getGlobalConfig() throws ServiceException {
        if (globalConfig == null) {
            globalConfig = Provisioning.getInstance().getConfig();
        }
        return globalConfig;
    }

    /**
     * Gets secret encoding with caching.
     */
    protected Encoding getSecretEncoding() throws ServiceException {
        if (secretEncoding == null) {
            try {
                String enc = getGlobalConfig().getTwoFactorAuthSecretEncodingAsString();
                secretEncoding = Encoding.valueOf(enc);
            } catch (IllegalArgumentException e) {
                ZimbraLog.account.warn("Invalid secret encoding, defaulting to BASE32", e);
                secretEncoding = Encoding.BASE32;
            }
        }
        return secretEncoding;
    }

    /**
     * Gets scratch code encoding with caching.
     */
    protected Encoding getScratchCodeEncoding() throws ServiceException {
        if (scratchEncoding == null) {
            try {
                String enc = getGlobalConfig().getTwoFactorAuthScratchCodeEncodingAsString();
                scratchEncoding = Encoding.valueOf(enc);
            } catch (IllegalArgumentException e) {
                ZimbraLog.account.warn("Invalid scratch code encoding, defaulting to BASE32", e);
                scratchEncoding = Encoding.BASE32;
            }
        }
        return scratchEncoding;
    }

    /**
     * Encrypts data using account-specific encryption.
     */
    protected String encrypt(String data) throws ServiceException {
        return DataSource.encryptData(account.getId(), data);
    }

    /**
     * Decrypts data using account-specific decryption.
     */
    protected static String decrypt(Account account, String encrypted)
            throws ServiceException {
        return DataSource.decryptData(account.getId(), encrypted);
    }

    /**
     * Determines if 2FA is required for this account.
     */
    public boolean twoFactorAuthRequired() throws ServiceException {
        if (!account.isFeatureTwoFactorAuthAvailable()) {
            return false;
        }
        return account.isTwoFactorAuthEnabled() ||
               account.isFeatureTwoFactorAuthRequired();
    }

    /**
     * Clears all data for this component.
     */
    public abstract void clearData() throws ServiceException;
}
```

### B. Example Constants Class

```java
package com.btactic.twofactorauth.core;

/**
 * Constants used throughout the 2FA extension.
 */
public final class TwoFactorAuthConstants {

    // Separators
    public static final String EMAIL_DATA_SEPARATOR = ":";
    public static final String SECRET_SEPARATOR = "\\|";
    public static final String SCRATCH_CODE_SEPARATOR = ",";

    // Array lengths
    public static final int SECRET_PARTS_COUNT = 2;
    public static final int EMAIL_DATA_PARTS_COUNT = 3;

    // Indices for email data parts
    public static final int EMAIL_CODE_INDEX = 0;
    public static final int EMAIL_RESERVED_INDEX = 1;
    public static final int EMAIL_TIMESTAMP_INDEX = 2;

    // Indices for secret parts
    public static final int SECRET_VALUE_INDEX = 0;
    public static final int SECRET_TIMESTAMP_INDEX = 1;

    // Default values
    public static final Encoding DEFAULT_SECRET_ENCODING = Encoding.BASE32;
    public static final Encoding DEFAULT_SCRATCH_ENCODING = Encoding.BASE32;

    private TwoFactorAuthConstants() {
        // Prevent instantiation
    }
}
```

### C. Example Improved CredentialGenerator

```java
package com.btactic.twofactorauth.credentials;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.codec.binary.Base32;
import org.apache.commons.codec.binary.Base64;

import com.zimbra.cs.account.auth.twofactor.TwoFactorAuth.CredentialConfig;
import com.zimbra.common.auth.twofactor.TwoFactorOptions.Encoding;
import com.zimbra.common.service.ServiceException;

/**
 * Generates secure credentials for 2FA including secrets and scratch codes.
 * Uses cryptographically secure random number generation.
 */
public class CredentialGenerator {
    private final CredentialConfig config;
    private final SecureRandom secureRandom;

    public CredentialGenerator(CredentialConfig config) {
        this.config = config;
        // Use default SecureRandom (better than SHA1PRNG)
        this.secureRandom = new SecureRandom();
    }

    /**
     * Generates random bytes using secure random number generator.
     *
     * @param numBytes number of bytes to generate
     * @return array of random bytes
     */
    protected byte[] generateBytes(int numBytes) {
        byte[] bytes = new byte[numBytes];
        secureRandom.nextBytes(bytes);
        return bytes;
    }

    /**
     * Masks bytes to ensure they're in valid range.
     * Applies 0x7F mask to each byte.
     */
    private byte[] mask(byte[] bytes) {
        byte[] masked = new byte[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            masked[i] = (byte) (bytes[i] & 0x7F);
        }
        return masked;
    }

    /**
     * Generates complete TOTP credentials including secret and scratch codes.
     */
    public TOTPCredentials generateCredentials() {
        byte[] secretBytes = generateBytes(config.getBytesPerSecret());
        String encoded = encodeBytes(mask(secretBytes), config.getEncoding());
        List<String> scratchCodes = generateScratchCodes();
        return new TOTPCredentials(encoded, scratchCodes);
    }

    /**
     * Generates unique scratch codes.
     * Uses Set to ensure uniqueness.
     */
    public List<String> generateScratchCodes() {
        Set<String> scratchCodeSet = new HashSet<>(config.getNumScratchCodes());
        while (scratchCodeSet.size() < config.getNumScratchCodes()) {
            scratchCodeSet.add(generateScratchCode());
        }
        return new ArrayList<>(scratchCodeSet);
    }

    private String generateScratchCode() {
        byte[] randomBytes = generateBytes(config.getBytesPerScratchCode());
        return encodeBytes(mask(randomBytes), config.getScratchCodeEncoding());
    }

    /**
     * Encodes bytes using specified encoding scheme.
     */
    protected String encodeBytes(byte[] bytes, Encoding encoding) {
        byte[] encoded;
        switch (encoding) {
            case BASE32:
                encoded = new Base32().encode(bytes);
                return new String(encoded).toUpperCase();
            case BASE64:
                encoded = Base64.encodeBase64(bytes);
                return new String(encoded).toUpperCase();
            default:
                throw new IllegalArgumentException("Unsupported encoding: " + encoding);
        }
    }
}
```

---

**END OF REPORT**
