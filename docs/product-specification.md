# Product specification

Offline-first Android app for PNG micro and small businesses: business profile, capability statement, quotations, tender checklist, readiness checklist, saved documents, PDF export, data control.

## Principles
No account, no network, no paid services, plain language, big buttons, never invent user information, never claim compliance.

## Screens and behaviour
- **Home**: tagline, profile status card (name or "not set up", missing key details), buttons for every feature, saved counts or empty-state hint, disclaimer.
- **Profile**: 13 fields; registration and TIN are optional radio choices stored as the user's own statement. Only badly formatted email, phone or years block saving.
- **Capability statement**: starts from the profile (business type, location, areas served, products, years of experience labelled "as stated by the business", qualifications, references, contact). Registration and TIN are not copied automatically. Business name is required. PDF skips empty sections and ends with a disclaimer.
- **Quotation**: business details copied from the profile and editable per quotation; suggested unique number (QT-0001...); issue date defaults to today; line items; charges, discount, optional tax; live totals; save; PDF.
- **Tender checklist**: list with progress; detail screen with opportunity fields, requirement cards (complete, reopen, edit, delete), progress block.
- **Readiness checklist**: grouped by the 7 categories; tap an item to change status or notes; add or remove items.
- **Saved documents**: five sections with open and delete (confirmation).
- **Settings**: version, privacy, language note, delete all.

## Language extension point
All user-facing text is currently inline in the `ui/` screen files and `pdf/DocumentBuilders.kt`. For Tok Pisin: move strings to `res/values/strings.xml`, add `res/values-tpi/strings.xml`, and read them with `stringResource` / `context.getString`. Android will pick the language from the phone setting; an in-app language switch is a later change.

## Out of scope
See `PROJECT_REQUIREMENTS.md`.
