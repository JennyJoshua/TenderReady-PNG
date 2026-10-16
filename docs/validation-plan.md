# Validation plan

## Automated (JVM unit tests, `app/src/test`)
- Calculator, money parsing and formatting, rounding, overflow, negative inputs.
- Checklist percentages including not-applicable and empty lists.
- Validation of profile, quotation form, opportunity, requirement, dates.
- PDF layout: wrapping, page breaks, no truncation, long unbroken words, table rows, business name, disclaimer, kina totals.
- Robolectric: persistence, cascades, duplicates, profile edit keeps links, delete-all, PdfDocument smoke test.

## Pipeline
`compileDebugSources`, `testDebugUnitTest`, `assembleDebug` in GitHub Actions.

## Manual on a phone
Use the 12-step list in `REVIEW.md`. Also check: large text size setting, a small low-end phone, rotation, airplane mode, sharing to WhatsApp and email, saving to Downloads.

## Exit criteria
Green build, all unit tests green, manual list passed, no screen claims compliance or eligibility.
