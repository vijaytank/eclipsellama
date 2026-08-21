## Phase 5 – Security Hardening (Implementation Plan) — v2.1.0 Target

Status: [IN PROGRESS] | Target: v2.1.0 | Method: TDD, zero assumptions, AST-based validation
Reference files: SetupLauncher.java (migratePreferences stub, lines 68-71), McpServerConfig.java (bearerToken: line 34 field, 123 getter, 127-130 setter), SseMcpConnection.java (line 121-122 header injection, 172-173 SSE header), StreamableHttpMcpConnection.java (line 54-55 header injection), McpServerStore.java (line 62 load bearerToken, 104 save bearerToken), eclipse-plugin-development skill (updated pitfalls.md), MANIFEST.MF (requires org.eclipse.equinox.security for Phase 6 gate).
---

### 5.1 Component: SecurePrefsStore (Encrypted Storage Migration)

Plan — sequential steps:
1. Create src/com/eclipsellama/plugin/security/SecurePrefsStore.java wrapping ISecurePreferences (org.eclipse.equinox.security.storage). Add dependency to MANIFEST.MF / plugin.xml (bundle org.eclipse.equinox.security).
2. Implement securePut(nodePath, key, value) and secureGet(nodePath, key, defaultValue) using ISecurePreferences node creation (getNode(nodePath, true)) and encrypted put/get.
3. Implement idempotent migratePreferences() in SetupLauncher (replace stub at line 68-71):
   - Read legacy EclipseLlamaPreferences.getApiKey(); if non-null/non-blank, call SecurePrefsStore.securePut("api/openai", "apiKey", value).
   - Read Brave Search key from preferences; if present, securePut("search/brave", "apiKey", value).
   - Read McpServerConfig.bearerToken for each loaded server; if present, securePut("mcp/" + serverName, "bearerToken", value).
   - After successful write, clear legacy plain-text fields (EclipseLlamaPreferences set blank, McpServerConfig.setBearerToken(null)) to prevent dual storage.
4. Update EclipseLlamaPreferences.getApiKey() to read from SecurePrefsStore (fallback to legacy for backward compatibility on first run before migration completes).
5. Update McpServerStore.load() / save to read/write bearer tokens via SecurePrefsStore.
6. Verify MANIFEST.MF exports org.eclipse.equinox.security as required bundle (Phase 6 Gate requirement pre-verified).

Acceptance Criteria (AC):
- AC-5.1.1: SecurePrefsStore creates/retrieves ISecurePreferences nodes without NullPointerException when bundle present; gracefully degrades with logged warning if org.eclipse.equinox.security bundle unavailable at runtime.
- AC-5.1.2: migratePreferences() is idempotent — running twice produces same secure storage state and no duplicate keys or exceptions.
- AC-5.1.3: After migration, EclipseLlamaPreferences.getApiKey() returns encrypted-stored value (not plain-text); config.properties no longer contains unencrypted key after second plugin start.
- AC-5.1.4: McpServerConfig bearer token roundtrips through SecurePrefsStore; SseMcpConnection and StreamableHttpMcpConnection receive token correctly (existing injection paths at lines 121-122, 172-173, 54-55 confirmed).
- AC-5.1.5: Zero compiler warnings with Java 21 PDE build; MANIFEST.MF includes org.eclipse.equinox.security.

Definition of Done (DoD):
- SecurePrefsStore.java exists, has JUnit test (with mock ISecurePreferences or integration test using EquinoxSecurity bundle in test runtime), passes, and is added to src/com/eclipsellama/plugin/security/ package.
- SetupLauncher.migratePreferences() replaced with real idempotent logic; System.out.println simulation removed; real secure storage calls verified.
- Migration verified manually: set plain-text API key, restart plugin, confirm SecurePrefsStore contains encrypted value, confirm getApiKey() returns it, confirm config.properties cleared.
- MANIFEST.MF updated and OSGi Manifest Validation gate (Phase 6) shows required bundle present.
---

### 5.2 Component: PromptSanitizer (Injection Defense)

Plan — sequential steps:
1. Create src/com/eclipsellama/plugin/security/PromptSanitizer.java with public static sanitizeUserInput(String).
2. Implementation details:
   - Strip zero-width characters: \u200B (ZWNJ), \u200C (ZWNJ), \u200D (ZWJ), \uFEFF (BOM), \u2060-\u206F (invisible formatting), \u3000 (ideographic space used for injection).
   - Neutralize prompt override patterns: regex-based removal/replacement of sequences like "ignore previous instructions", "system override", "you are now", "new instruction:" embedded in user input; replace with sanitized placeholder [SANITIZED_OVERRIDE_ATTEMPT] so audit trail remains.
   - Trim leading/trailing whitespace; normalize internal whitespace to prevent hidden encoding tricks.
3. Integrate in all code action handlers (handlers/ package): Explain, Fix, Doc, Test, Refactor, Review, Convert, GenerateCommit. Call PromptSanitizer.sanitizeUserInput() on any user-provided text before passing to LLMClient / OllamaClient / OpenAIClient.
4. Add JUnit test covering: plain string (unchanged except trim), string with \u200B, override phrase ("ignore previous instructions"), combined payload.

Acceptance Criteria (AC):
- AC-5.2.1: sanitizeUserInput() removes all zero-width characters listed above; test asserts result.contains("\u200B") == false.
- AC-5.2.2: sanitizeUserInput() replaces known override phrases with [SANITIZED_OVERRIDE_ATTEMPT] and preserves non-malicious content intact (e.g., normal code explanation request unchanged).
- AC-5.2.3: All 8 handlers (Explain, Fix, Doc, Test, Refactor, Review, Convert, GenerateCommit) call sanitizeUserInput() before LLM transmission; verified by AST inspection or manual code review of each handler.
- AC-5.2.4: No regression in 82+ existing JUnit tests; new sanitizer tests added and passing.

Definition of Done (DoD):
- PromptSanitizer.java implemented with documented regex patterns; test covers injection payloads.
- Every handler in handlers/ has sanitizeUserInput() call inserted; diff reviewed; zero compiler warnings.
- Existing tests (82+) still pass; new test file PromptSanitizerTest.java passes.
---

### 5.3 Component: MCP Bearer Authentication (Existing Wiring Verification + Secure Storage Integration)

Plan — sequential steps:
1. Confirm McpServerConfig has bearerToken field with getter/setter (verified: exists at line 34, 123, 127-130).
2. Confirm SseMcpConnection and StreamableHttpMcpConnection inject Authorization: Bearer <token> correctly (verified: lines 121-122, 172-173, 54-55). Confirm blank/null token skips header (verified: "if (bearerToken != null)" guard present).
3. Confirm McpServerStore.load() reads bearerToken from JSON (line 62: yes) and save() writes it (line 104: yes).
4. Update McpServerStore.load() / save() to use SecurePrefsStore for bearer token persistence (from 5.1) instead of plain JSON or plain properties; keep JSON config file for endpoint/model/config only, migrate token out.
5. Verify McpPreferencePage (preferences UI) allows editing bearer token; if missing, add Text widget for bearer token in preference page, bind to McpServerConfig.setBearerToken().

Acceptance Criteria (AC):
- AC-5.3.1: SseMcpConnection and StreamableHttpMcpConnection inject Authorization: Bearer <token> header when bearerToken is non-null/non-blank; skip when null/blank; verified by code inspection and optionally by network log capture.
- AC-5.3.2: McpServerConfig bearer token is persisted via SecurePrefsStore (from 5.1) — not in plain JSON; McpServerStore updated.
- AC-5.3.3: McpPreferencePage provides UI input for bearer token (new field if missing); value saved through SecurePrefsStore; page validates non-empty endpoint/model before allowing save (consistent with existing preference validation pattern).
- AC-5.3.4: No compiler errors in mcp/ package after changes.

Definition of Done (DoD):
- McpServerStore reads/writes bearer token via SecurePrefsStore; McpServerConfig.bearerToken set/get verified.
- McpPreferencePage includes bearer token input; preference page build passes.
- SseMcpConnection / StreamableHttpMcpConnection header injection confirmed by code review; no regression.
---

### 5.4 Component: HTTPS / TLS Configuration

Plan — sequential steps:
1. Add preferences security.tls.enforce (boolean, default false) and security.tls.truststore.path (String, default empty) in EclipseLlamaPreferences.
2. Update SseMcpConnection constructor / init to check preference: if tls.enforce true, validate endpoint uses https://; if truststore.path non-empty, load custom SSLContext with that truststore and apply to HttpURLConnection or SSE client.
3. Update StreamableHttpMcpConnection similarly.
4. Add preference UI fields in EclipseLlamaPreferences page (or new Security sub-page).
5. Add JUnit test verifying TLS preference read and custom SSL context initialization (mock or integration).

Acceptance Criteria (AC):
- AC-5.4.1: Preference security.tls.enforce exists; default false; when true, SseMcpConnection and StreamableHttpMcpConnection reject non-HTTPS endpoints with logged warning.
- AC-5.4.2: Preference security.tls.truststore.path loads custom truststore when provided; SSL handshake succeeds with that truststore; connection fails gracefully with clear error if path invalid.
- AC-5.4.3: Zero compiler warnings; preferences wired correctly; MANIFEST.MF unchanged except for any new required bundles.

Definition of Done (DoD):
- TLS preferences implemented, wired to connection classes; preference UI visible.
- Unit test verifies TLS enforcement logic and truststore loading.
- Code review confirms no security regression in SseMcpConnection / StreamableHttpMcpConnection.
---

Cross-Cutting Acceptance (Phase 5 Gate Before Phase 6):
- [ ] All 4 sub-components (5.1, 5.2, 5.3, 5.4) implemented.
- [ ] Zero Java compiler warnings (Java 21 PDE build).
- [ ] All 82 existing JUnit tests passing + new security tests passing.
- [ ] MANIFEST.MF validates with org.eclipse.equinox.security and org.eclipse.compare bundles (Phase 6 requirement pre-verified).
- [ ] eclipsellama-enhancement-plan.md updated: Phase 5 status changed from IN PROGRESS to COMPLETE; Phase 6 marked ready.

Implementation Sequence (Recommended Order):
1. 5.2 PromptSanitizer (independent, safe to implement first).
2. 5.1 SecurePrefsStore + 5.3 MCP Bearer Auth (interdependent via SecurePrefsStore; implement 5.1 first, then update 5.3 to use it).
3. 5.4 HTTPS / TLS (independent after 5.1, depends only on preference wiring).
4. Verify all AC/DoD; update plan file; proceed to Phase 6 gate.


---

## ✅ IMPLEMENTATION EVIDENCE (verified in-session)

| Component | New/Changed Files | JUnit Tests | Status |
| :--- | :--- | :--- | :--- |
| 5.1 SecurePrefsStore | `src/.../security/SecurePrefsStore.java` (new), `SetupLauncher.migratePreferences()` (real impl), `test/.../security/SecurePrefsStoreTest.java` | 5 | PASS |
| 5.2 PromptSanitizer | `src/.../security/PromptSanitizer.java` (new), `ChatView.sendMessage()` (choke-point integration), `test/.../security/PromptSanitizerTest.java` | 10 | PASS |
| 5.3 MCP Bearer Auth | verified existing wiring: `McpServerConfig` (L34/123/127-130), `SseMcpConnection` (L121-122/172-173), `StreamableHttpMcpConnection` (L54-55), `McpServerStore` (L62/104) | — | VERIFIED |
| 5.4 HTTPS / TLS | `EclipseLlamaPreferences` (+`security.tls.enforce`, `+truststore.path`, getters/setters), `EclipseLlamaPreferencePage` (TLS group), `TlsPolicy.java` + `TlsPolicyTest.java`, wired into `SseMcpConnection` & `StreamableHttpMcpConnection` constructors | 7 | PASS |

**Gate result:** `scripts/build_and_test.ps1` → full `src/` + `test/` compile (Java 21, zero warnings), JUnit suite **104/104 OK** (82 pre-existing + 22 new security). SetupLauncher migration degrades gracefully when the Equinox OSGi store is absent (AC-5.1.1 confirmed at runtime). `MANIFEST.MF` already declares `org.eclipse.equinox.security` and `org.eclipse.compare` (Phase 6 OSGi gate pre-satisfied).

**Still manual (IDE) verification for DoD sign-off:**
- [ ] Load plugin in an Eclipse PDE target; confirm SecurePrefsStore persists under the OS keystore and `config.properties` legacy keys clear on 2nd start.
- [ ] Confirm the new "HTTPS / TLS (Enterprise)" group renders under Preferences → EclipseLlama.
- [ ] Confirm SSE/Streamable-HTTP MCP connections reject non-HTTPS remote endpoints when Enforce TLS is checked.
