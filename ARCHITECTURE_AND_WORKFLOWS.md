# Zimbra 2FA Extension - Architecture and Workflows

## Mục Lục

1. [Tổng Quan Kiến Trúc](#tổng-quan-kiến-trúc)
2. [Các Thành Phần Chính](#các-thành-phần-chính)
3. [Luồng Xử Lý 2FA](#luồng-xử-lý-2fa)
4. [Chi Tiết Các Use Cases](#chi-tiết-các-use-cases)
5. [Data Flow](#data-flow)
6. [Tích Hợp Với Zimbra](#tích-hợp-với-zimbra)
7. [Security Model](#security-model)
8. [Troubleshooting Guide](#troubleshooting-guide)

---

## Tổng Quan Kiến Trúc

### Kiến Trúc Tổng Thể

```
┌─────────────────────────────────────────────────────────────────┐
│                         Zimbra Server                           │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │                    SOAP Service Layer                     │  │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐   │  │
│  │  │   Enable2FA  │  │   Auth2FA    │  │  Disable2FA  │   │  │
│  │  │   Handler    │  │   Handler    │  │   Handler    │   │  │
│  │  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘   │  │
│  └─────────┼──────────────────┼──────────────────┼───────────┘  │
│            │                  │                  │              │
│  ┌─────────┼──────────────────┼──────────────────┼───────────┐  │
│  │         ▼                  ▼                  ▼           │  │
│  │              2FA Extension Core Layer                    │  │
│  │  ┌────────────────────────────────────────────────────┐  │  │
│  │  │          ZetaTwoFactorAuth (Main Manager)          │  │  │
│  │  │  • TOTP Authentication                             │  │  │
│  │  │  • Email Code Authentication                       │  │  │
│  │  │  • Credential Management                           │  │  │
│  │  │  • Method Enablement (App/Email)                   │  │  │
│  │  └───────────┬────────────────────────────────────────┘  │  │
│  │              │                                           │  │
│  │     ┌────────┴────────┬────────────┬────────────────┐   │  │
│  │     ▼                 ▼            ▼                ▼   │  │
│  │  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐│  │
│  │  │  Scratch │  │   App    │  │ Trusted  │  │ Credential││  │
│  │  │  Codes   │  │ Specific │  │ Devices  │  │ Generator ││  │
│  │  │          │  │ Password │  │          │  │           ││  │
│  │  └──────────┘  └──────────┘  └──────────┘  └──────────┘│  │
│  │                                                           │  │
│  └───────────────────────────────────────────────────────────┘  │
│                                                                  │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │                  Core Infrastructure                      │  │
│  │  ┌─────────────────┐  ┌──────────────┐  ┌─────────────┐ │  │
│  │  │ Base Component  │  │   Constants  │  │    Utils    │ │  │
│  │  │  • Encryption   │  │  • Separators│  │ • Disable   │ │  │
│  │  │  • Caching      │  │  • Indices   │  │   Check     │ │  │
│  │  │  • Validation   │  │  • Encodings │  │ • Clear Data│ │  │
│  │  └─────────────────┘  └──────────────┘  └─────────────┘ │  │
│  └───────────────────────────────────────────────────────────┘  │
│                                                                  │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │                  Exception Hierarchy                      │  │
│  │               TwoFactorAuthException (base)               │  │
│  │    ├─ TwoFactorCodeExpiredException                       │  │
│  │    ├─ TwoFactorCodeInvalidException                       │  │
│  │    ├─ TwoFactorCredentialException                        │  │
│  │    ├─ TwoFactorSetupException                             │  │
│  │    └─ TwoFactorAuthRequiredException                      │  │
│  └───────────────────────────────────────────────────────────┘  │
│                                                                  │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │                    Data Storage (LDAP)                    │  │
│  │  • zimbraTwoFactorAuthSecret (encrypted TOTP secret)      │  │
│  │  • zimbraTwoFactorAuthScratchCodes (encrypted codes)      │  │
│  │  • zimbraTwoFactorAuthEnabled (boolean)                   │  │
│  │  • zimbraTwoFactorAuthMethodEnabled (app/email)           │  │
│  │  • zimbraAppSpecificPassword (encrypted passwords)        │  │
│  │  • zimbraTwoFactorAuthTrustedDevices (device tokens)      │  │
│  │  • zimbraTwoFactorCodeForEmail (encrypted email code)     │  │
│  └───────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

### Layer Responsibilities

#### 1. SOAP Service Layer
- **Chức năng**: Xử lý SOAP requests từ clients
- **Components**: EnableTwoFactorAuth, AuthTwoFactorAuth handlers
- **Input validation**: Request parameters, account validation
- **Response formatting**: SOAP responses với auth tokens

#### 2. 2FA Extension Core Layer
- **Chức năng**: Business logic cho 2FA
- **Components**: ZetaTwoFactorAuth, ZetaScratchCodes, etc.
- **Orchestration**: Điều phối giữa các components
- **State management**: Quản lý trạng thái 2FA

#### 3. Core Infrastructure
- **Chức năng**: Shared utilities và base classes
- **Components**: BaseTwoFactorAuthComponent, Constants, Utils
- **Services**: Encryption, caching, validation

#### 4. Data Storage
- **Chức năng**: Persistent storage
- **Backend**: LDAP
- **Encryption**: All sensitive data encrypted

---

## Các Thành Phần Chính

### 1. ZetaTwoFactorAuth (Core Manager)

**Vai trò**: Component trung tâm quản lý toàn bộ 2FA logic

**Responsibilities**:
```java
public class ZetaTwoFactorAuth extends BaseTwoFactorAuthComponent {
    // Authentication
    public void authenticate(String code)           // Xác thực code (TOTP/Email/Scratch)
    public void authenticateTOTP(String code)      // Xác thực TOTP code

    // Credential Management
    public TOTPCredentials generateCredentials()   // Generate TOTP secret + scratch codes
    public String loadSharedSecret()               // Load encrypted secret from LDAP

    // Enablement
    public void enableTwoFactorAuth()              // Enable 2FA for account
    public void addEnabledMethod(String method)    // Enable specific method (app/email)

    // Checking
    public boolean twoFactorAuthRequired()         // Check if 2FA is required
    public boolean isEnabledMethod(String method)  // Check if method is enabled
    public boolean isAllowedMethod(String method)  // Check if method is allowed
}
```

**Key Features**:
- ✅ Multi-method support (TOTP, Email, Scratch codes)
- ✅ Credential encryption/decryption
- ✅ Config caching for performance
- ✅ Legacy fallback support

---

### 2. ZetaScratchCodes (Backup Codes)

**Vai trò**: Quản lý backup codes cho recovery

**Responsibilities**:
```java
public class ZetaScratchCodes extends BaseTwoFactorAuthComponent {
    public void authenticate(String scratchCode)      // Validate and invalidate code
    public boolean checkScratchCodes(String code)     // Check if code is valid
    public List<String> generateCodes(...)            // Generate new scratch codes
    public List<String> getCodes()                    // Get current codes
    public void clearData()                           // Delete all codes
}
```

**Features**:
- ✅ One-time use codes (invalidated after use)
- ✅ Encrypted storage in LDAP
- ✅ Configurable number of codes
- ✅ Iterator-based removal (optimized for performance)

---

### 3. ZetaAppSpecificPasswords (App Passwords)

**Vai trò**: Quản lý passwords cho apps không hỗ trợ 2FA

**Responsibilities**:
```java
public class ZetaAppSpecificPasswords extends BaseTwoFactorAuthComponent {
    public void authenticate(String password)         // Authenticate with app password
    public AppSpecificPassword generatePassword(...)  // Generate new app password
    public void revoke(String name)                   // Revoke specific password
    public void revokeAll()                           // Revoke all passwords
    public Set<AppSpecificPasswordData> getPasswords() // Get all passwords
}
```

**Use Cases**:
- Mobile email clients (IMAP/POP3)
- Desktop email clients
- Calendar sync apps
- Contact sync apps

---

### 4. ZetaTrustedDevices (Trusted Devices)

**Vai trò**: Quản lý thiết bị đáng tin cậy (skip 2FA)

**Responsibilities**:
```java
public class ZetaTrustedDevices extends BaseTwoFactorAuthComponent {
    public TrustedDeviceToken registerTrustedDevice(...)  // Register new device
    public void verifyTrustedDevice(...)                  // Verify device token
    public void revokeTrustedDevice(...)                  // Revoke specific device
    public void revokeAllTrustedDevices()                 // Revoke all devices
    public List<TrustedDevice> getTrustedDevices()        // Get all devices
}
```

**Features**:
- ✅ Cookie-based device recognition
- ✅ Configurable expiration time
- ✅ Device fingerprinting
- ✅ Token-based authentication

---

### 5. CredentialGenerator (Security Core)

**Vai trò**: Generate cryptographically secure credentials

**Responsibilities**:
```java
public class CredentialGenerator {
    public TOTPCredentials generateCredentials()   // Generate TOTP secret + codes
    public List<String> generateScratchCodes()     // Generate scratch codes
    protected byte[] generateBytes(int n)          // Secure random generation
    protected String encodeBytes(...)              // BASE32/BASE64 encoding
}
```

**Security Features**:
- ✅ Uses SecureRandom (not SHA1PRNG)
- ✅ Byte masking for encoding compatibility
- ✅ Unique scratch codes (Set-based)
- ✅ Input validation

---

### 6. EmailCodeParser (Helper)

**Vai trò**: Parse và validate email 2FA codes

**Responsibilities**:
```java
public final class EmailCodeParser {
    public static EmailCodeData parse(...)              // Parse encrypted email code
    public static EmailCodeData validateAndParse(...)   // Parse and validate

    public static class EmailCodeData {
        public boolean isExpired(long lifetimeMs)       // Check expiration
        public long getExpiryTime(long lifetimeMs)      // Get expiry timestamp
    }
}
```

---

## Luồng Xử Lý 2FA

### Workflow 1: Enable 2FA (App-based TOTP)

```
┌─────────┐         ┌──────────────┐         ┌──────────────────┐         ┌──────────┐
│ Client  │         │ SOAP Handler │         │ ZetaTwoFactorAuth│         │   LDAP   │
└────┬────┘         └──────┬───────┘         └────────┬─────────┘         └────┬─────┘
     │                     │                          │                        │
     │ 1. EnableTwoFactorAuth                         │                        │
     │  (name, password)   │                          │                        │
     ├────────────────────>│                          │                        │
     │                     │                          │                        │
     │                     │ 2. Validate account      │                        │
     │                     │    & check 2FA available │                        │
     │                     ├─────────────────────────>│                        │
     │                     │                          │                        │
     │                     │                          │ 3. Check if already    │
     │                     │                          │    enabled             │
     │                     │                          ├───────────────────────>│
     │                     │                          │<───────────────────────┤
     │                     │                          │                        │
     │                     │                          │ 4. Generate credentials│
     │                     │                          │    (secret + scratch)  │
     │                     │                          │    via CredentialGen   │
     │                     │                          │                        │
     │                     │                          │ 5. Encrypt & store     │
     │                     │                          │    secret in LDAP      │
     │                     │                          ├───────────────────────>│
     │                     │                          │<───────────────────────┤
     │                     │                          │                        │
     │                     │ 6. Return secret +       │                        │
     │                     │    auth token            │                        │
     │                     │<─────────────────────────┤                        │
     │                     │                          │                        │
     │ 7. Response:        │                          │                        │
     │    {secret, token}  │                          │                        │
     │<────────────────────┤                          │                        │
     │                     │                          │                        │
     │ ════════════════════════════════════════════════════════════════════════
     │ User scans QR code với Google Authenticator
     │ ════════════════════════════════════════════════════════════════════════
     │                     │                          │                        │
     │ 8. EnableTwoFactorAuth                         │                        │
     │  (name, code,       │                          │                        │
     │   authToken)        │                          │                        │
     ├────────────────────>│                          │                        │
     │                     │                          │                        │
     │                     │ 9. Validate auth token   │                        │
     │                     ├─────────────────────────>│                        │
     │                     │                          │                        │
     │                     │                          │ 10. Authenticate TOTP  │
     │                     │                          │     code               │
     │                     │                          │                        │
     │                     │                          │ 11. Enable 2FA +       │
     │                     │                          │     add method         │
     │                     │                          ├───────────────────────>│
     │                     │                          │<───────────────────────┤
     │                     │                          │                        │
     │                     │                          │ 12. Update token       │
     │                     │                          │     validity           │
     │                     │                          ├───────────────────────>│
     │                     │                          │<───────────────────────┤
     │                     │                          │                        │
     │                     │ 13. Return scratch codes │                        │
     │                     │     + new auth token     │                        │
     │                     │<─────────────────────────┤                        │
     │                     │                          │                        │
     │ 14. Response:       │                          │                        │
     │  {scratchCodes,     │                          │                        │
     │   authToken}        │                          │                        │
     │<────────────────────┤                          │                        │
     │                     │                          │                        │
```

**Bước Chi Tiết**:

1. **Initial Request**: Client gửi username + password
2. **Validation**: Handler validates account, checks 2FA available
3. **Check Status**: Kiểm tra 2FA đã enable chưa
4. **Generate**: `CredentialGenerator` tạo TOTP secret (20 bytes) + 10 scratch codes
5. **Encrypt & Store**: Encrypt secret với account ID, store to LDAP
6. **Return Secret**: Trả về secret (BASE32) + temporary auth token
7. **User Setup**: User scan QR code với authenticator app
8. **Verification Request**: Client gửi TOTP code + auth token
9. **Validate Token**: Verify auth token có usage type = ENABLE_TWO_FACTOR_AUTH
10. **Authenticate**: Verify TOTP code matches (with time window)
11. **Enable**: Set `zimbraTwoFactorAuthEnabled=true`, add method "app"
12. **Update Token**: Increment token validity value
13. **Return Codes**: Trả về scratch codes cho user save
14. **Complete**: 2FA enabled thành công

---

### Workflow 2: Login với 2FA (TOTP)

```
┌─────────┐       ┌──────────────┐       ┌──────────────────┐       ┌──────────┐
│ Client  │       │ Auth Handler │       │ ZetaTwoFactorAuth│       │   LDAP   │
└────┬────┘       └──────┬───────┘       └────────┬─────────┘       └────┬─────┘
     │                   │                        │                      │
     │ 1. Auth(username, │                        │                      │
     │        password)  │                        │                      │
     ├──────────────────>│                        │                      │
     │                   │                        │                      │
     │                   │ 2. Verify password     │                      │
     │                   ├───────────────────────────────────────────────>│
     │                   │<───────────────────────────────────────────────┤
     │                   │                        │                      │
     │                   │ 3. Check 2FA required  │                      │
     │                   ├───────────────────────>│                      │
     │                   │<───────────────────────┤                      │
     │                   │   (2FA Required=true)  │                      │
     │                   │                        │                      │
     │ 4. Response:      │                        │                      │
     │    Need2FA=true   │                        │                      │
     │<──────────────────┤                        │                      │
     │                   │                        │                      │
     │ 5. Auth(username, │                        │                      │
     │    password,      │                        │                      │
     │    twoFactorCode) │                        │                      │
     ├──────────────────>│                        │                      │
     │                   │                        │                      │
     │                   │ 6. Verify password     │                      │
     │                   ├───────────────────────────────────────────────>│
     │                   │<───────────────────────────────────────────────┤
     │                   │                        │                      │
     │                   │ 7. Authenticate code   │                      │
     │                   ├───────────────────────>│                      │
     │                   │                        │                      │
     │                   │                        │ 8. Load secret       │
     │                   │                        ├─────────────────────>│
     │                   │                        │<─────────────────────┤
     │                   │                        │                      │
     │                   │                        │ 9. Determine code    │
     │                   │                        │    type (length)     │
     │                   │                        │                      │
     │                   │                        │ 10. Check TOTP code  │
     │                   │                        │     with time window │
     │                   │                        │                      │
     │                   │<───────────────────────┤                      │
     │                   │   (Auth Success)       │                      │
     │                   │                        │                      │
     │ 11. Response:     │                        │                      │
     │     authToken +   │                        │                      │
     │     session cookie│                        │                      │
     │<──────────────────┤                        │                      │
     │                   │                        │                      │
```

**Code Type Detection** (Bước 9):
```java
if (code.length == 6)     → TOTP code
if (code.length == 8)     → Scratch code
if (code.length == 12)    → Email code (nếu enabled)
```

**TOTP Validation** (Bước 10):
```java
// Get current time in 30-second intervals
long timeWindow = currentTimeMillis / 30000;

// Check code against time window ± offset
for (int i = -offset; i <= offset; i++) {
    String expectedCode = generateTOTP(secret, timeWindow + i);
    if (providedCode.equals(expectedCode)) {
        return true; // Valid
    }
}
return false; // Invalid
```

---

### Workflow 3: Login với Email 2FA

```
┌─────────┐     ┌──────────────┐     ┌──────────────────┐     ┌──────────┐
│ Client  │     │ SOAP Handler │     │ ZetaTwoFactorAuth│     │   LDAP   │
└────┬────┘     └──────┬───────┘     └────────┬─────────┘     └────┬─────┘
     │                 │                      │                    │
     │ 1. Enable Email │                      │                    │
     │    2FA (email,  │                      │                    │
     │    password)    │                      │                    │
     ├────────────────>│                      │                    │
     │                 │                      │                    │
     │                 │ 2. Validate account  │                    │
     │                 ├─────────────────────>│                    │
     │                 │                      │                    │
     │                 │                      │ 3. Generate random │
     │                 │                      │    12-digit code   │
     │                 │                      │                    │
     │                 │                      │ 4. Store code      │
     │                 │                      │    (encrypted):    │
     │                 │                      │    code:reserved:  │
     │                 │                      │    timestamp       │
     │                 │                      ├───────────────────>│
     │                 │                      │<───────────────────┤
     │                 │                      │                    │
     │                 │                      │ 5. Send email with │
     │                 │                      │    code to user    │
     │                 │                      │                    │
     │                 │ 6. Response: Code    │                    │
     │                 │    sent to email     │                    │
     │                 │<─────────────────────┤                    │
     │                 │                      │                    │
     │ 7. Email sent   │                      │                    │
     │<────────────────┤                      │                    │
     │                 │                      │                    │
     │ ═════════════════════════════════════════════════════════════
     │ User receives email với code
     │ ═════════════════════════════════════════════════════════════
     │                 │                      │                    │
     │ 8. Enable Email │                      │                    │
     │    2FA (code)   │                      │                    │
     ├────────────────>│                      │                    │
     │                 │                      │                    │
     │                 │ 9. Validate code     │                    │
     │                 ├─────────────────────>│                    │
     │                 │                      │                    │
     │                 │                      │ 10. Load & decrypt │
     │                 │                      │     email code     │
     │                 │                      ├───────────────────>│
     │                 │                      │<───────────────────┤
     │                 │                      │                    │
     │                 │                      │ 11. Parse:         │
     │                 │                      │     code:_:time    │
     │                 │                      │                    │
     │                 │                      │ 12. Check expired  │
     │                 │                      │     (lifetime)     │
     │                 │                      │                    │
     │                 │                      │ 13. Compare code   │
     │                 │                      │                    │
     │                 │<─────────────────────┤                    │
     │                 │   (Valid)            │                    │
     │                 │                      │                    │
     │                 │                      │ 14. Enable email   │
     │                 │                      │     method         │
     │                 │                      ├───────────────────>│
     │                 │                      │<───────────────────┤
     │                 │                      │                    │
     │ 15. Success     │                      │                    │
     │<────────────────┤                      │                    │
     │                 │                      │                    │
```

**Email Code Format**:
```
Encrypted in LDAP: "code:reserved:timestamp"
Example: "123456789012::1700000000000"
         └──12 digits─┘  └─timestamp─┘
```

**Expiration Check**:
```java
long emailLifeTime = account.getTwoFactorCodeLifetimeForEmail(); // e.g., 3600000 ms (1 hour)
long emailExpiryTime = timestamp + emailLifeTime;
boolean isExpired = System.currentTimeMillis() > emailExpiryTime;
```

---

### Workflow 4: Recovery với Scratch Code

```
┌─────────┐         ┌──────────────┐         ┌──────────────────┐         ┌──────────┐
│ Client  │         │ Auth Handler │         │ ZetaScratchCodes │         │   LDAP   │
└────┬────┘         └──────┬───────┘         └────────┬─────────┘         └────┬─────┘
     │                     │                          │                        │
     │ 1. Auth(username,   │                          │                        │
     │    password,        │                          │                        │
     │    scratchCode)     │                          │                        │
     ├────────────────────>│                          │                        │
     │                     │                          │                        │
     │                     │ 2. Verify password       │                        │
     │                     ├─────────────────────────────────────────────────>│
     │                     │<─────────────────────────────────────────────────┤
     │                     │                          │                        │
     │                     │ 3. Detect code type      │                        │
     │                     │    (length = 8)          │                        │
     │                     │    → Scratch code        │                        │
     │                     │                          │                        │
     │                     │ 4. Authenticate scratch  │                        │
     │                     ├─────────────────────────>│                        │
     │                     │                          │                        │
     │                     │                          │ 5. Load scratch codes  │
     │                     │                          ├───────────────────────>│
     │                     │                          │<───────────────────────┤
     │                     │                          │   (comma-separated,    │
     │                     │                          │    encrypted)          │
     │                     │                          │                        │
     │                     │                          │ 6. Decrypt codes       │
     │                     │                          │                        │
     │                     │                          │ 7. Iterate through     │
     │                     │                          │    codes with Iterator │
     │                     │                          │                        │
     │                     │                          │ 8. Find matching code  │
     │                     │                          │                        │
     │                     │                          │ 9. Remove code (O(1))  │
     │                     │                          │    via iterator.remove()│
     │                     │                          │                        │
     │                     │                          │ 10. Save updated codes │
     │                     │                          ├───────────────────────>│
     │                     │                          │<───────────────────────┤
     │                     │                          │                        │
     │                     │<─────────────────────────┤                        │
     │                     │   (Auth Success)         │                        │
     │                     │                          │                        │
     │ 11. Login Success   │                          │                        │
     │     + Warning:      │                          │                        │
     │     "X codes left"  │                          │                        │
     │<────────────────────┤                          │                        │
     │                     │                          │                        │
```

**Scratch Code Invalidation** (Optimized):
```java
// Using Iterator for O(1) removal
Iterator<String> iterator = scratchCodes.iterator();
while (iterator.hasNext()) {
    String code = iterator.next();
    if (code.equals(scratchCode)) {
        iterator.remove();  // O(1) removal
        storeCodes();       // Save to LDAP
        return true;
    }
}
return false;
```

**Warning Logic**:
```java
int remainingCodes = scratchCodes.size();
if (remainingCodes <= 3) {
    showWarning("Only " + remainingCodes + " scratch codes remaining. Generate new codes.");
}
```

---

## Chi Tiết Các Use Cases

### Use Case 1: Generate New Scratch Codes

**Trigger**: User runs out of scratch codes hoặc muốn regenerate

**Flow**:
```
1. User requests new scratch codes
2. ZetaScratchCodes.generateNewScratchCodes()
3. CredentialGenerator generates N unique codes (default: 10)
4. Codes encrypted và stored to LDAP
5. Return codes to user (display once, must save)
```

**Code**:
```java
public List<String> generateNewScratchCodes() throws ServiceException {
    ZimbraLog.account.debug("invalidating current scratch codes");
    CredentialConfig config = getCredentialConfig();
    List<String> newCodes = new CredentialGenerator(config).generateScratchCodes();

    scratchCodes.clear();
    scratchCodes.addAll(newCodes);
    storeScratchCodes();

    return scratchCodes;
}
```

---

### Use Case 2: App-Specific Password Authentication

**Scenario**: Mobile email client cần access mailbox

**Flow**:
```
1. User generates app-specific password for "iPhone Mail"
2. ZetaAppSpecificPasswords.generatePassword("iPhone Mail")
3. Random password generated (16 chars)
4. Password hashed + encrypted, stored to LDAP
5. User enters password in iPhone Mail app
6. App connects với IMAP username + app password
7. ZetaAppSpecificPasswords.authenticate(password)
8. If valid → Grant access (skip 2FA)
```

**Security**:
- Password shown only once
- Each app has separate password
- Can revoke individual passwords
- Expires based on policy

---

### Use Case 3: Trusted Device

**Scenario**: User wants to skip 2FA on work laptop

**Flow**:
```
1. User logs in với 2FA
2. Checkbox "Trust this device for 30 days"
3. ZetaTrustedDevices.registerTrustedDevice(deviceAttrs)
4. Generate device token (UUID)
5. Store device fingerprint + token in LDAP
6. Set cookie "ZM_TRUST_TOKEN" with token
7. Next login: Check cookie
8. ZetaTrustedDevices.verifyTrustedDevice(token, attrs)
9. If valid → Skip 2FA prompt
```

**Device Attributes**:
```java
Map<String, Object> deviceAttrs = {
    "userAgent": "Mozilla/5.0...",
    "ipAddress": "192.168.1.100",
    "fingerprint": "hash-of-browser-features"
}
```

**Expiration**:
```java
long expiryTime = registrationTime + (30 * 24 * 60 * 60 * 1000); // 30 days
if (System.currentTimeMillis() > expiryTime) {
    device.revoke();
}
```

---

### Use Case 4: Disable 2FA

**Scenario**: Admin hoặc user muốn disable 2FA

**Flow**:
```
1. User/Admin requests disable 2FA
2. Verify current 2FA code (security check)
3. ZetaTwoFactorAuth.clearData()
   ├─ Delete TOTP secret
   ├─ Delete scratch codes
   ├─ Revoke all app-specific passwords
   └─ Revoke all trusted devices
4. Set zimbraTwoFactorAuthEnabled = false
5. Clear all method enablement flags
6. Success → 2FA disabled
```

---

## Data Flow

### Data Storage in LDAP

```
Account DN: uid=user@example.com,ou=people,dc=example,dc=com

Attributes:
├─ zimbraTwoFactorAuthEnabled: TRUE
├─ zimbraTwoFactorAuthSecret: {encrypted}ABCDEFGH1234567890|20231120120000
│   Format: BASE32_SECRET|TIMESTAMP
│   Encryption: AES-256 with account ID as key
│
├─ zimbraTwoFactorAuthScratchCodes: {encrypted}CODE1,CODE2,CODE3,...
│   Format: BASE32_CODE1,BASE32_CODE2,...
│   One-time use, removed after validation
│
├─ zimbraTwoFactorAuthMethodEnabled: app
│   Values: "app" | "email" | "app,email"
│
├─ zimbraTwoFactorAuthMethodAllowed: app,email
│   Admin-controlled: which methods user can enable
│
├─ zimbraTwoFactorCodeForEmail: {encrypted}123456789012::1700000000000
│   Format: CODE:RESERVED:TIMESTAMP
│   Expires after configurable time (default: 1 hour)
│
├─ zimbraAppSpecificPassword: {hashed}name=iPhone|hash=...|created=...|...
│   Multiple values, one per app
│
└─ zimbraTwoFactorAuthTrustedDevices: tokenId=UUID|expires=timestamp|...
    Multiple values, one per trusted device
```

### Encryption Flow

```
┌──────────────────┐
│  Plain Text Data │
│  "MYSECRETCODE"  │
└────────┬─────────┘
         │
         │ 1. Get Account ID
         ▼
   ┌──────────────┐
   │  Account ID  │
   │ "account-123"│
   └──────┬───────┘
          │
          │ 2. Derive encryption key
          ▼
   ┌─────────────────┐
   │ Encryption Key  │
   │ (AES-256)       │
   └────────┬────────┘
            │
            │ 3. Encrypt
            ▼
   ┌────────────────────────┐
   │ Encrypted Data         │
   │ "kJ8$mN#pQ2..."       │
   └───────────┬────────────┘
               │
               │ 4. Store in LDAP
               ▼
        ┌──────────────┐
        │     LDAP     │
        └──────────────┘
```

**Decryption** (reverse process):
```java
String encrypted = account.getTwoFactorAuthSecret();
String decrypted = DataSource.decryptData(account.getId(), encrypted);
```

---

## Tích Hợp Với Zimbra

### Integration Points

#### 1. Authentication Flow

```
Zimbra Auth Flow:
┌─────────────────────────────────────────────────────────────┐
│  1. Username + Password Check                               │
│     └─> Zimbra's standard LDAP authentication              │
│                                                              │
│  2. Check if 2FA Required                                   │
│     └─> ZetaTwoFactorAuth.twoFactorAuthRequired()          │
│                                                              │
│  3. If 2FA Required:                                        │
│     ├─> Check for app-specific password                    │
│     │   └─> ZetaAppSpecificPasswords.authenticate()        │
│     │                                                        │
│     ├─> Check for trusted device token                     │
│     │   └─> ZetaTrustedDevices.verifyTrustedDevice()       │
│     │                                                        │
│     └─> Prompt for 2FA code                                │
│         └─> ZetaTwoFactorAuth.authenticate(code)           │
│                                                              │
│  4. Generate Auth Token + Session                           │
│     └─> Zimbra's standard session management               │
└─────────────────────────────────────────────────────────────┘
```

#### 2. SOAP API Integration

**Endpoints**:
```xml
<!-- Enable 2FA -->
<EnableTwoFactorAuthRequest xmlns="urn:zimbraAccount">
    <name>user@example.com</name>
    <password>userpassword</password>
    <method>app</method>  <!-- or "email" -->
</EnableTwoFactorAuthRequest>

<!-- Response (Phase 1) -->
<EnableTwoFactorAuthResponse>
    <secret>ABCDEFGH12345678</secret>
    <authToken>temp-token-for-verification</authToken>
</EnableTwoFactorAuthResponse>

<!-- Enable 2FA (Phase 2 - Verification) -->
<EnableTwoFactorAuthRequest xmlns="urn:zimbraAccount">
    <name>user@example.com</name>
    <authToken>temp-token-from-phase1</authToken>
    <twoFactorCode>123456</twoFactorCode>
</EnableTwoFactorAuthRequest>

<!-- Response (Phase 2) -->
<EnableTwoFactorAuthResponse>
    <scratchCodes>
        <code>12345678</code>
        <code>87654321</code>
        ...
    </scratchCodes>
    <authToken>final-auth-token</authToken>
</EnableTwoFactorAuthResponse>
```

#### 3. Extension Loading

**Extension Configuration** (`zimbraAdmin` console):
```xml
<zimlet name="com_btactic_twofactorauth">
    <serverExtension hasKeyword="2fa" extensionClass="com.btactic.twofactorauth.ZetaTwoFactorAuth"/>
</zimlet>
```

**Factory Pattern**:
```java
public static class AuthFactory implements Factory {
    @Override
    public TwoFactorAuth getTwoFactorAuth(Account account) {
        return new ZetaTwoFactorAuth(account);
    }

    @Override
    public ScratchCodes getScratchCodes(Account account) {
        return new ZetaScratchCodes(account);
    }

    @Override
    public AppSpecificPasswords getAppSpecificPasswords(Account account) {
        return new ZetaAppSpecificPasswords(account);
    }

    @Override
    public TrustedDevices getTrustedDevices(Account account) {
        return new ZetaTrustedDevices(account);
    }
}
```

---

## Security Model

### Defense in Depth

```
┌─────────────────────────────────────────────────────────────┐
│ Layer 1: Transport Security                                 │
│  • HTTPS/TLS for all communication                          │
│  • Certificate validation                                   │
└─────────────────────────────────────────────────────────────┘
                             │
┌─────────────────────────────────────────────────────────────┐
│ Layer 2: Authentication                                     │
│  • Username + Password (first factor)                       │
│  • Rate limiting on login attempts                          │
│  • Account lockout after N failures                         │
└─────────────────────────────────────────────────────────────┘
                             │
┌─────────────────────────────────────────────────────────────┐
│ Layer 3: Two-Factor Authentication                          │
│  • TOTP (Time-based One-Time Password)                      │
│  • Email-based codes                                        │
│  • Scratch codes (backup)                                   │
│  • Time window validation                                   │
└─────────────────────────────────────────────────────────────┘
                             │
┌─────────────────────────────────────────────────────────────┐
│ Layer 4: Data Protection                                    │
│  • AES-256 encryption for secrets                           │
│  • Bcrypt hashing for app passwords                         │
│  • Secure key derivation                                    │
└─────────────────────────────────────────────────────────────┘
                             │
┌─────────────────────────────────────────────────────────────┐
│ Layer 5: Session Management                                 │
│  • Secure session tokens                                    │
│  • HttpOnly cookies                                         │
│  • Session expiration                                       │
│  • Trusted device tokens                                    │
└─────────────────────────────────────────────────────────────┘
```

### Cryptographic Security

**Random Number Generation**:
```java
// SECURE (after Phase 1 optimization)
private final SecureRandom secureRandom = new SecureRandom();
secureRandom.nextBytes(bytes);

// INSECURE (before optimization - FIXED)
// SecureRandom.getInstance("SHA1PRNG").nextBytes(bytes);
```

**TOTP Algorithm** (RFC 6238):
```
TOTP = HOTP(K, T)

Where:
  K = Shared secret (BASE32 encoded)
  T = (Current Unix time) / 30  (time step = 30 seconds)

HOTP(K, T) = Truncate(HMAC-SHA1(K, T))
```

**Time Window**:
```java
// Allow ±1 time step (±30 seconds) for clock skew
int windowOffset = 1;
for (int i = -windowOffset; i <= windowOffset; i++) {
    if (verifyCode(secret, timeWindow + i, code)) {
        return true;
    }
}
```

---

## Troubleshooting Guide

### Common Issues

#### Issue 1: "Invalid two-factor code"

**Causes**:
1. Clock skew between server and client device
2. Wrong secret stored
3. Code expired (TOTP is time-based)

**Debug**:
```bash
# Check server time
date

# Check TOTP window configuration
zmprov gcf zimbraTwoFactorTimeWindowLength
zmprov gcf zimbraTwoFactorTimeWindowOffset

# Check account 2FA status
zmprov ga user@example.com zimbraTwoFactorAuthEnabled
zmprov ga user@example.com zimbraTwoFactorAuthSecret
```

**Solution**:
- Increase time window offset
- Sync server time with NTP
- Regenerate TOTP secret

---

#### Issue 2: "Email code not found"

**Causes**:
1. Email code never generated
2. Code expired
3. LDAP attribute empty

**Debug**:
```bash
# Check email code in LDAP
zmprov ga user@example.com zimbraTwoFactorCodeForEmail

# Check email code lifetime
zmprov ga user@example.com zimbraTwoFactorCodeLifetimeForEmail
```

**Solution**:
- Request new email code
- Check email delivery
- Verify SMTP settings

---

#### Issue 3: Scratch codes not working

**Causes**:
1. Code already used (one-time use)
2. Codes not generated
3. Typo in code

**Debug**:
```bash
# Check scratch codes
zmprov ga user@example.com zimbraTwoFactorAuthScratchCodes

# Count remaining codes
echo "encrypted-codes" | openssl enc -d -aes-256-cbc -k account-id | tr ',' '\n' | wc -l
```

**Solution**:
- Generate new scratch codes
- Verify code carefully (no spaces)
- Use different scratch code

---

#### Issue 4: App-specific password rejected

**Causes**:
1. Password expired
2. Password revoked
3. Feature not enabled

**Debug**:
```bash
# Check feature enabled
zmprov ga user@example.com zimbraFeatureAppSpecificPasswordsEnabled

# List app passwords
zmprov ga user@example.com zimbraAppSpecificPassword
```

**Solution**:
- Generate new app password
- Enable feature if disabled
- Check app password expiration policy

---

### Logging

**Enable Debug Logging**:
```bash
# Enable 2FA debug logging
zmlocalconfig -e zimbra_2fa_debug=true

# View logs
tail -f /opt/zimbra/log/mailbox.log | grep -i "twoFactor\|2FA"
```

**Log Locations**:
- `/opt/zimbra/log/mailbox.log` - Main application log
- `/opt/zimbra/log/audit.log` - Authentication audit log
- `/var/log/zimbra.log` - General Zimbra log

---

## Performance Considerations

### Caching Strategy

**Config Caching**:
```java
// Cached in BaseTwoFactorAuthComponent
private Config globalConfig;
private Encoding secretEncoding;
private Encoding scratchEncoding;

// Loaded once per instance
protected Config getGlobalConfig() throws ServiceException {
    if (globalConfig == null) {
        globalConfig = Provisioning.getInstance().getConfig();
    }
    return globalConfig;
}
```

**Benefits**:
- Reduces LDAP queries
- Improves authentication speed
- Lower server load

### Optimization Tips

1. **Use Trusted Devices**: Reduce 2FA prompts for known devices
2. **App-Specific Passwords**: Avoid 2FA for non-interactive apps
3. **Increase Time Window**: Balance security vs usability
4. **Batch Operations**: Generate all scratch codes at once

---

## Conclusion

Zimbra 2FA Extension cung cấp comprehensive security layer với:

- ✅ **Multiple Authentication Methods**: TOTP, Email, Scratch codes
- ✅ **Flexible Configuration**: Per-user, per-method settings
- ✅ **Recovery Options**: Scratch codes, app passwords
- ✅ **Trusted Devices**: Reduce 2FA friction
- ✅ **Strong Security**: AES-256 encryption, SecureRandom, time-based codes
- ✅ **Audit Trail**: Comprehensive logging
- ✅ **User-Friendly**: Clear error messages, documentation

**For more information**:
- Technical Documentation: `/docs`
- API Reference: `OPTIMIZATION_SUMMARY.md`
- Test Guide: `extension/test/TEST_README.md`
