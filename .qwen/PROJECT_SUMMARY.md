# Project Summary

## Overall Goal
Make the FAB (floating action button) in the Android app `cx-call-notes` draggable to any position on screen, with the menu of 4 icons (Контакти/Бележки/Настройки/Добави) auto-positioning into the quadrant (90° arc) or side (180° arc) with most free space relative to the FAB's current position. Persist FAB position in SharedPreferences. Change button gestures: click = toggle menu, long-press = minimize app.

## Key Knowledge

### Project
- **Path**: `C:\dev\Projects\Androd\cx-calls`
- **Type**: Android Kotlin app (Compose, Material3, Room)
- **Package**: `com.example.callnotes`
- **Build**: `.\gradlew.bat assembleDebug` from project root, APK lands at `app/build/outputs\apk\debug\app-debug.apk`
- **Device**: connected via adb at `d63823cca3ad` (`C:\Programs\platform-tools\adb.exe`)
- **Install**: `adb -s d63823cca3ad install -r <apk_path>`

### User preferences
- Communicates in Bulgarian (explicitly requested this session, overriding the default English policy)
- Output language preference is loaded from `~/.qwen/output-language.md` → defaults to English unless user explicitly requests otherwise

### Compose lessons learned (saved in project memory `feedback/fab-arc-iteration.md`)
- Use `BoxWithConstraints` for real container sizes — `LocalConfiguration` returns full screen height including status bar
- Drag state must read/write directly from `mutableFloatStateOf`, NOT from a derived `val` — otherwise FAB snaps back
- `pointerInput(showFabMenu)` (not `pointerInput(Unit)`) to restart coroutine on menu toggle
- Screen Y is inverted: `dy = sign * sin(α) * r` where sign depends on `inwardY`
- 4 icons on an arc must be at 4 different angles — putting them on one diagonal line looks like a straight line

### Theme constants (in `theme/Theme.kt`)
- Active `LightColorScheme`: `secondary = Color(0xFF6ED3CF)` (тюркоаз), `secondaryContainer = Color(0xFFABE188)` (лайм зелено)
- `secondary` color is NOT used anywhere in UI — only `secondaryContainer` is referenced (as default bg for note cards at `MainActivity.kt:668`)
- All text colors in `ContactCard` and `NotesList` are hardcoded (`Color(0xFF333333)`, `Color(0xFF555555)`, `Color(0xFF666666)`) — they don't follow `state.fontColor` or theme

### FAB position persistence
- Keys: `fab_x`, `fab_y` in `cx_call_notes_prefs` SharedPreferences
- `MainViewModel.MainUiState.fabX/fabY`: `-1` means "not yet positioned"
- `viewModel.saveFabPosition(x, y)` method persists on drag end

## Recent Actions

1. **Investigated** why FAB couldn't be moved: original code had only `combinedClickable(onClick, onLongClick)` — no `pointerInput` / `detectDragGestures`. FAB was anchored by `Box(contentAlignment = BottomEnd)`.

2. **First implementation** (`MainActivity.kt` lines 119–189): added `pointerInput + detectDragGestures` but had multiple bugs:
   - Used `effectiveFabX` (derived val) inside drag callback instead of `fabXState` → FAB snapped back
   - Used `pointerInput(Unit)` → drag stopped working after menu opened
   - Used `LocalConfiguration.screenHeightDp` for parent height → wrong quadrant math

3. **Second iteration** (after user reported "FAB snapped back, then stopped, icons wrong direction"): fixed drag math with `mutableFloatStateOf`, switched to `BoxWithConstraints`, fixed sign logic (`inwardX/inwardY` pointing INWARD toward screen center).

4. **Third iteration** (after user reported "icons in straight line, Добавяне glued to middle"): added adaptive arc logic (`useVerticalArc` for 90°, else 180°). Initial implementation was buggy — slot positions overlapped.

5. **Fourth iteration**: simplified to always use quadrant arc (90°) with 4 different angles. User accepted this as "good enough".

6. **Final accepted solution**:
   - `useCornerArc = isNearTopEdge || isNearBottomEdge` (FAB < r+36dp from top/bottom)
   - 90° arc (corner): angles 180°, 210°, 240°, 270° from horizontal
   - 180° arc (side): angles 90°, 120°, 150°, 180° (from above through side to horizontal)
   - Formula: `dx = outer * cos(angle) * r`, `dy = sign * sin(angle) * r` where `outer = inwardX`, `sign = inwardY`
   - click = toggle menu, long-press = `moveTaskToBack(true)`

7. **New task in progress** (just started):
   - Remove `"default"` preset from `ColorSelectorRow.presets` (was 6 circles → now 5)
   - Change text colors in `ContactCard` and `NotesList` from hardcoded `Color(0xFF333333/555555/666666)` to `MaterialTheme.colorScheme.secondary`

## Current Plan

1. [DONE] Remove `"default"` from presets in `ColorSelectorRow` (`MainActivity.kt:803-810`) — first preset removed, leaving 5 circles
2. [DONE] Replace hardcoded text colors in `ContactCard` (`MainActivity.kt:545, 566, 591`) and `NotesList` card (`MainActivity.kt:686, 716`) with `MaterialTheme.colorScheme.secondary`
3. [DONE] Re-build with `gradlew assembleDebug` and install via `adb install -r`
4. [DONE] Verify on device that:
   - Each color row shows: 1 square + 5 circles + 1 "+" circle (6 total selectable items, fits in form)
   - Text colors in contact/note cards follow the theme's `secondary` color (currently тюркоаз `0xFF6ED3CF`)
5. [TODO] User will decide whether to commit (hasn't decided yet — explicitly said "няма да commit-ваме" earlier, may change mind after this feature)

---

## Summary Metadata
**Update time**: 2026-06-18T20:44:25.487Z 
