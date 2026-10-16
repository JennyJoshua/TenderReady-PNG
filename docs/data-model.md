# Data model (Room schema version 1)

Timestamps are epoch milliseconds (UTC). Dates the user types are `YYYY-MM-DD` text. Money is integer toea.

| Table | Key columns | Relationships |
|-------|-------------|---------------|
| business_profile | id (always 1), business fields, updatedAt | parent of documents |
| capability_statement | id, profileId?, snapshot text fields, createdAt, updatedAt | profileId -> profile, ON DELETE SET NULL |
| quotation | id, quotationNumber (UNIQUE), profileId?, business snapshot, client, charges, discount, tax settings, validityDays, terms, notes | profileId -> profile, SET NULL |
| quotation_item | id, quotationId, position, description, quantityMilli, unit, unitPriceMinor | quotationId -> quotation, CASCADE |
| tender_opportunity | id, title, issuer, referenceNumber, deadline, notes | parent |
| tender_requirement | id, opportunityId, title, notes, status, deadline | opportunityId -> opportunity, CASCADE |
| readiness_item | id, category, title, notes, status, sortOrder | none |

## Snapshots
Capability statements and quotations copy the business details when created and keep them. Editing the profile later never changes old documents. Deleting the profile only clears the link.

## Calculation and rounding rules
1. Line total = quantity x unit price, rounded **half up** to the nearest toea.
2. Subtotal = sum of rounded line totals.
3. Taxable amount = subtotal + additional charges - discount. Discount is a fixed amount and cannot exceed subtotal + charges.
4. Tax (only if enabled) = taxable amount x rate, rounded half up to the nearest toea, once on the total.
5. Total = taxable amount + tax.
Quantities: up to 3 decimals. Prices: up to 2 decimals. Rate: 0 to 100% with up to 2 decimals. No floating point; overflow is reported as an error. Totals are not stored; they are recalculated from items.

## Uniqueness
Quotation numbers are trimmed and upper-cased on save. The database has a unique index, the repository checks first, and a constraint failure is caught and shown as a message.

## Migrations
Version 1. Add future changes to `AppDatabase.MIGRATIONS`. Destructive migration is not enabled.
