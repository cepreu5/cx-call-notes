---
name: android-build-fix
description: Automated edit→compile→fix cycle for Android/Kotlin projects using Gradle. Runs compilation, parses errors, and iterates fixes until build succeeds.
---

# Android Build & Fix Cycle

Automated workflow for editing Kotlin/Android code with Gradle compilation verification.

## When to Use

- After making code changes that need compilation verification
- When fixing compilation errors in Kotlin/Android files
- For iterative development where multiple edit→compile cycles are expected

## Workflow

### Step 1: Make Code Changes
Edit the relevant Kotlin files using the Edit tool. Focus on one logical change at a time.

### Step 2: Run Compilation Check
```powershell
.\gradlew.bat compileDebugKotlin 2>&1 | Select-String -Pattern "e:|BUILD"
```

### Step 3: Parse Output
- **BUILD SUCCESSFUL** → Done, changes compile correctly
- **e: file:///...** → Compilation errors found, proceed to Step 4

### Step 4: Fix Errors
For each error line:
1. Parse the file path and line number from the error message
2. Read the file at that location
3. Understand the error (unresolved reference, type mismatch, syntax error, etc.)
4. Apply the fix using Edit tool

### Step 5: Repeat
Return to Step 2 and re-run compilation. Continue until BUILD SUCCESSFUL.

## Tips

- **One change at a time**: Make small, focused edits rather than large rewrites
- **Check warnings**: `w:` lines are warnings, not errors — they don't block compilation
- **Common errors**:
  - `Unresolved reference` → Missing import or typo in name
  - `Type mismatch` → Wrong parameter types
  - `Expecting '>'` → Syntax error, check braces/parentheses
- **Stack trace**: If only stack trace appears, the actual error is earlier in the output — scroll up

## Project Context

This project (cx-calls) uses:
- Kotlin with Jetpack Compose
- Gradle with `.\gradlew.bat` wrapper
- Android SDK 33 (Xiaomi test device)
- Room database for persistence

Key files frequently modified:
- `app/src/main/java/com/example/callnotes/MainActivity.kt` (1700+ lines — be careful with edits)
- `app/src/main/java/com/example/callnotes/ui/PostCallNoteActivity.kt`
- `app/src/main/java/com/example/callnotes/service/PhoneStateReceiver.kt`
- `app/src/main/java/com/example/callnotes/ui/MainViewModel.kt`
- `app/src/main/java/com/example/callnotes/ui/PostCallNoteViewModel.kt`
