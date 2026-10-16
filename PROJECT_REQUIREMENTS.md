# Project requirements: TenderReady PNG

## Product requirements
Help PNG micro and small businesses (builders, cleaners, caterers, security, transport, suppliers) quote and
prepare tender material using only their phone, offline, for free.

## Functional requirements (MVP)
| ID | Requirement |
|----|-------------|
| F1 | Open with no account; home shows branding, profile status, and links to every feature. |
| F2 | Reusable business profile stored in Room; incomplete profiles can be saved; formats validated. |
| F3 | Capability statement pre-filled from profile, editable, exported to multi-page PDF, labelled as user-provided. |
| F4 | Quotations with line items, decimal quantity, additional charges, discount, optional configurable tax, validity, payment terms, notes; drafts; edit; PDF; share. |
| F5 | Quotation totals use integer arithmetic with documented rounding; numbers are unique. |
| F6 | Tender opportunities with manual requirements, three statuses, counts and percentage. |
| F7 | Customisable readiness checklist with notes, four statuses, percentage and disclaimer. |
| F8 | Saved documents screen: reopen, edit, delete with confirmation. |
| F9 | Save PDF (file picker), Share PDF (Sharesheet), preview in a viewer app. |
| F10 | Settings: version, privacy text, language note, delete all data with confirmation. |

## Non-functional requirements
Offline-first; no network permission; no analytics; no secrets; large touch targets and plain language;
works on low-end devices (minSdk 24, no heavy libraries); data survives navigation and rotation;
free tools only; builds with GitHub Actions.

## MVP boundaries (not built)
Online submission, government integration, marketplace, payments, paid APIs, AI legal/tax advice,
automatic eligibility or compliance decisions, accounts, cloud sync, remote databases, SMS, roles, social features.

## Acceptance criteria
1. GitHub Actions run of compileDebugSources, testDebugUnitTest and assembleDebug is green.
2. The debug APK installs on a physical Android phone and opens without an account.
3. A profile, quotation, capability statement, opportunity and checklist can be created, saved, closed, reopened and edited with data intact.
4. Quotation PDF totals match the on-screen totals; kina formatting is correct.
5. Long capability statements and 50+ item quotations produce multi-page PDFs with no clipped text.
6. Share PDF opens the Android share sheet; Save PDF writes a file to the chosen location.
7. Duplicate quotation numbers are refused with a clear message.
8. Delete all data empties the app after confirmation.
9. No screen claims compliance, eligibility or guaranteed success.
Items 2 to 8 can only be confirmed by the owner on a phone (see `REVIEW.md`).
