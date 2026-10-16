# Review

Honest status as of version 0.1.0. Nothing here claims the app works on a phone.

## Testing actually performed
| Check | Result |
|-------|--------|
| Pure Kotlin logic (`core/*`, `data/Entities`, `Mapping`, `ReadinessDefaults`, `pdf/PdfLayout`, `DocumentBuilders`) compiled with the real Kotlin 2.0.21 compiler | Compiled, no errors |
| 58 unit tests (MoneyTest, QuotationCalculatorTest, ChecklistTest, ValidationTest, PdfLayoutTest) run on JDK 21 | 58 passed, 0 failed |

Caveat: those 58 were run with a small stand-in for JUnit and with stand-ins for the Room annotations, because Maven
repositories were not reachable from the build sandbox. The test code itself is the real code in `app/src/test`.

## NOT performed
- No Gradle build, no `compileDebugSources`, no `assembleDebug`. **Build status: unknown until your GitHub Actions run.**
- Android-dependent code was never compiled: Compose screens, ViewModels, Room DAOs/database/repository, PdfWriter, PdfExporter, MainActivity. It was desk-checked only, so a small compile error is possible.
- `RepositoryTest` (persistence, duplicate numbers, cascades, delete-all) and `PdfWriterTest` need Robolectric and were never run.
- No UI tests (the pipeline does not run instrumented tests).
- Dependency version compatibility was not verified against Maven.
- Nothing was tried on a physical phone.

## Test coverage map
A quotation maths: 20 calculator tests + 5 money tests (run). B checklist: 8 (run). C persistence: 16 (Robolectric, not run). D documents: 12 layout/content tests (run) + 2 PdfDocument smoke tests (not run). E validation: 13 (run).

## Known bugs and risks
- Possible first-build errors (see above). Likeliest: a dependency needing compileSdk 35, or a Compose API mismatch.
- Robolectric's PdfDocument is a simplified stand-in, so real PDF look must be checked on a phone.
- Re-opening the Readiness checklist re-adds starter items if you deleted all of them.
- Quotation numbers are saved upper-case (qt-1 becomes QT-1) so duplicates cannot differ by case.
- A save that fails because a record was deleted elsewhere shows a generic message.

## Incomplete or simplified
Typed dates (no picker); no unsaved-changes warning; in-app PDF preview replaced by "open in viewer app"; English only (Tok Pisin is an extension point, text is not yet in string resources); light theme only; no data backup/restore file; no dedicated profile delete (use Settings > Delete all).

## Security and privacy review
- No permissions, no INTERNET permission, no analytics, no ads, no keys, no network code.
- `allowBackup=false`. Data sits in app-private Room storage, **not encrypted** (stated in the app).
- Export only through the user-chosen file picker or the share sheet (FileProvider, cache copy, read grant for the chosen target only). Cache copies are left for the system to clear.
- No sensitive values are logged (the app contains no logging).
- Delete all data clears every table; the app states that exported PDFs are not removed.
- The app makes no compliance, eligibility, tax or legal claims; disclaimers appear on home, checklists, settings and PDFs.

## Manual testing still required (on your phone)
1. Install, open, no login. 2. Create profile with some fields empty, save, reopen. 3. Quotation: 2 items, decimal quantity, charge, discount, tax on and off; compare screen totals with PDF. 4. Create a second quotation: number is suggested and different; try a duplicate. 5. Capability statement: edit, export; add very long text and check page 2. 6. Share PDF to WhatsApp or email; Save PDF to Downloads; Preview. 7. Tender: add opportunity, requirements, complete, reopen, delete. 8. Readiness: change statuses, add an item. 9. Saved documents: open, delete with confirm. 10. Rotate the screen mid-edit. 11. Settings > Delete all data. 12. Airplane mode: everything still works.
