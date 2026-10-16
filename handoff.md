# Handoff for the project owner

## 1. Prepare the repo
- Create the repo `tenderready-png` from your template.
- If it still contains the old reference app sources (for example under `app/src/main/java/...` with a different package, or `app/src/test`), delete them first. The new code uses package `com.tenderready.png` and could clash.

## 2. Upload the ZIP
Upload `tenderready-png-mvp-v1.zip` to `_inbox/` on `main`. It is extracted automatically (no build starts).
The ZIP overwrites `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties` and `app/build.gradle.kts`.

## 3. Run the build
GitHub > Actions > **Android build and test** > Run workflow. Wait for the green tick.
If it is red: open the failing step, copy its last 20 to 30 lines (or the run summary), paste them to me. I will fix the first real error and send a ZIP with only changed files.

## 4. Install the APK
Open the finished run, scroll to **Artifacts**, download the APK artifact, unzip it, open the `.apk`, allow installing from this source.

## 5. Test on your phone (checklist is in `REVIEW.md`)
Create profile, quotation (with and without tax), capability statement, tender and checklist. Export a PDF, share it, save it. Close and reopen the app.

## 6. Reporting bugs
Tell me: the screen, what you tapped, what you expected, what happened, and a screenshot. For crashes, say what you did just before.

## 7. Requesting changes
Start a new chat, paste your Android build handover, attach this repo's `DECISIONS.md` and `REVIEW.md`, and describe the change in one or two sentences. Ask for a ZIP with only new or changed files.

## Manual steps
- Delete old reference sources if present (step 1).
- Nothing needs to be added under `.github/`. Optional: `docs/android-build.yml` can be copied by hand to `.github/workflows/` if you want builds on every push.
- No API keys, secrets or variables are needed.
