# TenderReady PNG

*Get your PNG business ready to quote, bid, and win contracts.*

An offline-first native Android app that helps micro and small businesses in Papua New Guinea prepare
professional documents and organise tender requirements. It needs no account, no internet and no paid service.

**Status: MVP, version 0.1.0. It compiles in GitHub Actions and its logic and database tests pass. The debug APK build and phone testing are still pending.**
See `REVIEW.md` for exactly what was and was not checked.

The app helps you organise and prepare documents. It does not confirm legal compliance or tender eligibility,
and it cannot promise you will win any contract.

## Features
- **Home**: branding, profile status, shortcuts, empty states.
- **Business profile**: reusable details, can be saved incomplete, basic format checks.
- **Capability statement**: pre-filled from the profile, fully editable, multi-page PDF. Labelled as based on information provided by the user. Nothing is invented.
- **Quotations**: line items with decimal quantities, additional charges, discount, optional tax, validity, payment terms, notes. Kina (PGK) formatting. Unique quotation numbers. Save drafts, edit later, export PDF.
- **Tender checklist**: opportunities with your own requirements (Outstanding / Complete / Not applicable), counts and percentage.
- **Readiness checklist**: customisable general checklist (Not started / In progress / Complete / Not applicable).
- **Saved documents**: reopen, edit, delete (with confirmation).
- **Export**: Share PDF (Android Sharesheet), Save PDF (system file picker), Preview in a PDF viewer app.
- **Settings**: version, privacy notes, language note, delete all data (with confirmation).

## Screens
Home, Business profile, Capability statement, Quotation, Tender list, Tender opportunity (with requirements),
Readiness checklist, Saved documents, Settings and privacy. Sketches are in `docs/wireframes.md`.

## Architecture
Kotlin, Jetpack Compose (Material 3), simple MVVM with one repository, Room as the single source of truth.
```
app/src/main/java/com/tenderready/png/
  core/   pure logic: Money, QuotationCalculator, Checklist, DateUtil, Validation   (no Android)
  data/   Room entities, DAOs, AppDatabase, AppRepository
  pdf/    PdfLayout (pure, testable), PdfWriter (Android PdfDocument), DocumentBuilders, PdfExporter
  ui/     Compose screens, MainViewModel, QuotationEditorViewModel, navigation
```
Money is stored as integer toea (1 kina = 100 toea), quantities as thousandths, tax rates as basis points.
Rounding rules are in `docs/data-model.md`.

## Build setup (unchanged from the template unless noted)
Gradle 8.9, Android Gradle Plugin 8.7.3, Kotlin 2.0.21, JDK 17, compileSdk/targetSdk 34, minSdk 24.
Added for this app: Compose BOM 2024.09.03, Navigation 2.8.2, Lifecycle 2.8.6, Activity 1.9.2, Core KTX 1.13.1,
Room 2.6.1 with KSP 2.0.21-1.0.28, and (tests only) JUnit 4.13.2, Robolectric 4.13, AndroidX Test Core 1.6.1.
`settings.gradle.kts` also lists `gradlePluginPortal()` for plugin resolution (the KSP plugin). See `DECISIONS.md`.

## Building with GitHub Actions
1. Upload the ZIP to `_inbox/` on `main` (it is extracted automatically; extraction does not start a build).
2. Run **Android build and test** manually from the Actions tab.
3. Open the finished run, scroll to **Artifacts**, download the debug APK.
Free-tier minutes are limited (private repositories get a monthly allowance; public repositories are free but
rules can change). A build with Compose, KSP and Robolectric will likely take several minutes. Artifacts count
towards storage and expire after the repository's retention period (a shorter period can be set in the workflow).
`docs/android-build.yml` is an optional reference workflow.

## Install on your phone
Download the APK artifact (it arrives as a ZIP, unzip it), open the `.apk`, and allow "install unknown apps"
for your browser or file manager when asked. It is a debug build, signed with a debug key, not for Play Store use.

## Known limitations
See `REVIEW.md`. In short: dates are typed as YYYY-MM-DD; no unsaved-changes warning; light theme only;
English only; no encryption of stored data; PDF preview opens an external viewer app; not yet built or run.
