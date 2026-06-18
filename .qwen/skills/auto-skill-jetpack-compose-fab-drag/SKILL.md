---
name: jetpack-compose-fab-drag
description: How to make a FloatingActionButton movable by drag in Jetpack Compose, with menu icons that adapt to the FAB's quadrant and never leave the visible screen area. Applies whenever adding drag-to-move behavior to any Compose UI element with associated popups.
source: auto-skill
extracted_at: '2026-06-18T17:51:31.335Z'
---

# Jetpack Compose: movable FAB with adaptive popup menu

## When to use

Any time you need a Compose UI element (FAB, button, icon) that:
- Can be **dragged freely around the screen** with a finger
- Has an **associated popup/menu** that should open in a direction that keeps it visible
- Should **persist its position** across app restarts
- Should **disable drag while its popup is open** (avoid accidental grabs of menu items)

## Critical prerequisite: free positioning

The draggable element **cannot live inside a parent that uses static alignment** like `Box(contentAlignment = Alignment.BottomEnd)`. Static alignment re-anchors the child on every layout pass and will fight the drag.

Use this pattern instead:
```kotlin
BoxWithConstraints(modifier = Modifier.fillMaxSize()) {  // full-bleed, absolute positioning
    val maxX = (constraints.maxWidth.toFloat()  - fabSizePx).coerceAtLeast(0f)
    val maxY = (constraints.maxHeight.toFloat() - fabSizePx).coerceAtLeast(0f)
    // popup items with Modifier.offset { IntOffset(x, y) }
    Box(
        modifier = Modifier
            .offset { IntOffset(fabX.toInt(), fabY.toInt()) }   // <-- moves the element
            .size(56.dp)
            .pointerInput(showPopup) { ... }
            .combinedClickable(...)
    ) { Icon(...) }
}
```

**Why `BoxWithConstraints` and not `Box(fillMaxSize)`**: `Scaffold.floatingActionButton` provides a container whose size is `(screen - topBar - bottomBar - status bar)`. `LocalConfiguration.current.screenHeightDp` returns the **full** screen size (deprecated, also wrong on edge-to-edge layouts). `BoxWithConstraints` gives the real `constraints.maxWidth/maxHeight` of the container the FAB actually lives in — clamping is correct without guessing status bar / nav bar heights.

The `offset { ... }` lambda variant is preferred over the `offset(x.dp, y.dp)` variant because it accepts `IntOffset` (pixels) which is what drag deltas work with.

## Drag gesture handling — the right way

```kotlin
.pointerInput(showPopup) {                    // re-key on popup state
    if (!showPopup) {
        detectDragGestures(
            onDragEnd = { viewModel.savePosition(fabX.toInt(), fabY.toInt()) }
        ) { change, dragAmount ->
            change.consume()                  // <-- required: prevents click/drag conflict
            fabX = (fabX + dragAmount.x).coerceIn(0f, maxX)
            fabY = (fabY + dragAmount.y).coerceIn(0f, maxY)
        }
    }
}
```

### Three things that go wrong and how to fix each

1. **`pointerInput(Unit)` captures stale state**: the `Unit` key never changes, so the lambda runs once. If you read a state variable that updates later, you read the original captured value. Either re-key the `pointerInput` (`pointerInput(showPopup)` — restarts the detector when popup toggles) or use **mutable state inside** the lambda.
2. **Reading a derived val like `effectiveX` inside the drag callback**: the derived val is recalculated on each recomposition, but **within a single recomposition** it's frozen. If you do `fabX = effectiveX + dragAmount.x`, every drag event resets from the same starting point — element stutters or snaps back. Solution: read and write the **mutable state directly** (`fabX = fabX + dragAmount.x`).
3. **Missing `change.consume()`**: the click and drag gestures fight, the element stutters and may register false clicks. Always consume.

### State type: `Float`, not `Int`

Use `mutableFloatStateOf` for `fabX/fabY`, and convert to `IntOffset` only at the offset modifier. Integer state loses sub-pixel precision and makes the element visibly snap to a pixel grid while dragging.

```kotlin
var fabX by remember { mutableFloatStateOf(initialX) }   // Float
var fabY by remember { mutableFloatStateOf(initialY) }
// ... later
.offset { IntOffset(fabX.toInt(), fabY.toInt()) }       // convert at the edge
```

## Clamping to the screen

```kotlin
BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val maxX = (constraints.maxWidth.toFloat()  - fabSizePx).coerceAtLeast(0f)
    val maxY = (constraints.maxHeight.toFloat() - fabSizePx).coerceAtLeast(0f)
}
```

The element's `Modifier.offset` uses **top-left** of the element as the origin, so clamping `0f..maxX` is correct (the right/bottom edge of the element sits at `parentWidth/Height`).

## Adaptive popup direction (quadrant logic)

The popup should always open in a direction that keeps it on-screen. The standard pattern is to flip the offsets based on which half of the screen the anchor's center is in:

```kotlin
val centerX = effectiveX + elementSizePx / 2
val centerY = effectiveY + elementSizePx / 2
val signX = if (centerX >= parentWidthPx  / 2)  1 else -1
val signY = if (centerY >= parentHeightPx / 2)  1 else -1

// Popup items sit at 90° arc around the anchor:
val radius  = 80.dp  in px
val diagX   = (radius * 0.5f).toInt()    // ~28% of radius
val diagY   = (radius * 0.5f).toInt()

// Each item's offset = anchor + signX * X * signY * Y direction
Item("right-most")  at offset(signX * radius, 0)
Item("top-right")   at offset(signX * diagX,  -signY * diagY)
Item("top-left")    at offset(-signX * diagX, -signY * diagY)
Item("top-most")    at offset(0, -signY * radius)
```

This way:
- FAB in **bottom-right** quadrant → menu opens **top-left** (default behavior)
- FAB in **top-left** quadrant → menu opens **bottom-right**
- And so on for all four corners.

The `signX/signY` recomputation must happen on every recomposition (no `remember` wrapping) because it depends on the live `effectiveX/effectiveY`.

## Persistence in SharedPreferences

For this project's pattern: **reuse the existing `cx_call_notes_prefs` file** defined in `MainViewModel`'s `prefs` field. Do not create a new `getSharedPreferences(...)` call. Add fields like:

```kotlin
val fabX: Int = -1,
val fabY: Int = -1
```

in `MainUiState`, read them in `loadSettings()`, write them in a dedicated `saveFabPosition(x, y)` method that updates both prefs and state.

**Use `-1` as the sentinel** for "never been moved" — the UI then falls back to the default position (bottom-right in this case) by checking `if (fabX < 0) maxX else fabX.coerceIn(0, maxX)`.

## Re-keying for state restoration

When state values feed into `remember` blocks (e.g. `var x by remember(state.fabX) { mutableStateOf(state.fabX) }`), the `state.fabX` as the key means: every time the persisted value changes (e.g. after `saveFabPosition`), the local `mutableStateOf` resets to that new value. This avoids drift between the VM state and the local drag state.

## Don't forget

- Add `import androidx.compose.foundation.gestures.detectDragGestures`
- Add `import androidx.compose.ui.input.pointer.pointerInput`
- Add `import androidx.compose.ui.platform.LocalConfiguration`
- Add `import androidx.compose.ui.platform.LocalDensity`
- Add `import androidx.compose.ui.unit.IntOffset`
- `combinedClickable` (with `onClick` + `onLongClick`) still works alongside `pointerInput` — the gestures are layered. `onClick` triggers on a quick tap that the drag system did not consume.
- Verify with `gradlew compileDebugKotlin` (or `assembleDebug`) before committing.
