# Decisions

## Versions
Kept the proven template: Gradle 8.9, AGP 8.7.3, Kotlin 2.0.21, JDK 17, compileSdk/targetSdk 34, minSdk 24.
minSdk 24 covers older, low-cost phones. Dependency versions were chosen from memory of what is compatible with
compileSdk 34 and Kotlin 2.0.21 and could **not** be checked against Maven from the build sandbox, so the first
GitHub Actions run is the real compatibility test. Most likely trouble spots: a library requiring compileSdk 35.

## Dependencies (all free, public, pinned)
- Compose BOM 2024.09.03 (Compose 1.7, Material 3 1.3), Compose compiler via the Kotlin Compose plugin 2.0.21.
- Activity 1.9.2, Lifecycle 2.8.6, Navigation 2.8.2, Core KTX 1.13.1.
- Room 2.6.1 + room-ktx, processed with KSP 2.0.21-1.0.28 (Room needs kapt or KSP; KSP is faster).
- Tests: JUnit 4.13.2, Robolectric 4.13 (runs Room and PdfDocument on a plain JVM), AndroidX Test Core 1.6.1.
- `settings.gradle.kts` adds `gradlePluginPortal()` to pluginManagement only, so plugins resolve. Project repositories stay google() + mavenCentral(). If you want it removed, tell me and I will test without it.
- No icons-extended, no image libraries, no PDF library: Android's built-in PdfDocument is used.

## Architecture
- MVVM-light: `MainViewModel` (flows and deletes) and `QuotationEditorViewModel` (editor state). Simple screens keep form state locally with rememberSaveable and call the repository directly.
- Room is the source of truth. PDFs are generated from saved records only (save first, then export).
- Pure logic (`core`, `PdfLayout`) has no Android dependencies so it is unit-testable on the JVM.
- Capability statements and quotations keep their own copy (snapshot) of business details, so editing the profile later does not alter old documents. `profileId` is a nullable link (SET_NULL).
- Profile uses `@Upsert`, not REPLACE, because REPLACE deletes the row and would trigger SET_NULL on documents (covered by a test).
- Quotation totals are never stored; they are recalculated from items, so they cannot go out of sync.
- No destructive migration. `MIGRATIONS` is empty at version 1 with `exportSchema = false`.

## Money
Integer toea, quantity in thousandths, tax in basis points, BigDecimal HALF_UP only for the multiplication step. Overflow is reported as an error, never wrapped. Discount is a fixed amount. Tax is off by default and the user enters the name and rate; the app never assumes a rate.

## Files and privacy
No permissions. Save uses the system `CreateDocument` picker; share uses FileProvider on a cache copy. `allowBackup=false` so data is not copied to cloud backup. Data is not encrypted and the app says so.

## Trade-offs
- Dates are typed text (YYYY-MM-DD) to avoid experimental date-picker APIs.
- Preview opens an external PDF viewer instead of an in-app renderer.
- PDF layout is deliberately plain (single typeface, rules, table) for reliability.
- Light theme only. Strings are inline in screens; moving them to resources is the Tok Pisin extension point.
- Unsaved edits are lost when leaving a form; screens say so.
- Readiness defaults are re-added if the user deletes every item.

## Future
Tok Pisin via string resources; date picker; in-app preview; local backup/restore file; instrumented UI tests (the pipeline does not run them today).
