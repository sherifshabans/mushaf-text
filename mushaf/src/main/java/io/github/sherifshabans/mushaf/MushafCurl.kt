package io.github.sherifshabans.mushaf

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.Modifier
import kotlin.math.abs

/*
 * تقليب ورقة واحدة (Fore-edge peel) — الصفحة الحالية بترتفع من حافتها
 * اليسرى (في RTL) وتتكشّف الصفحة اللي تحتها، مع ظهر الورقة على الحافة
 * وظلّ على المكشوف. الهندسة دوال نقية، والقصّ بس على طبقات مرتّبة مسبقًا.
 */

/** How [MushafPager] turns a leaf. */
enum class PageTurn {
    /** Today's right-to-left slide. `currentPageOffsetFraction` is the drag. */
    Slide,
    /**
     * A full-height fore-edge peel driven by an internal animation.
     * `state.currentPageOffsetFraction` stays `0` during a peel — it is **not**
     * the peel progress.
     */
    Curl,
}

/**
 * One peel frame.
 *
 * @property boundaryX where the lifted sheet meets the flat page.
 * @property backLeft/backRight the strip showing the sheet's back face
 *   (zero width at the settled ends).
 * @property shadowLeft/shadowRight the gradient band cast on the revealed page.
 */
data class CurlSlice(
    val boundaryX: Float,
    val backLeft: Float,
    val backRight: Float,
    val shadowLeft: Float,
    val shadowRight: Float,
)

/**
 * Pure peel geometry.
 *
 * Forward (toward the next page — the RTL reader lifts the **left** fore-edge
 * to the right): the boundary travels left→right, the current page stays
 * clipped to `x >= boundaryX`, the back strip lies left of the boundary, and
 * the shadow falls on the revealed page further left. Backward mirrors all of
 * it. At `progress` 0 and 1 there is exactly one full page and **no strip**.
 */
fun curlSlice(
    widthPx: Float,
    progress: Float,
    forward: Boolean,
    backWidthPx: Float,
    shadowWidthPx: Float,
): CurlSlice {
    val p = progress.coerceIn(0f, 1f)
    val settled = p <= 0f || p >= 1f
    val back = if (settled) 0f else backWidthPx.coerceAtLeast(0f)
    val shadow = shadowWidthPx.coerceAtLeast(0f)
    val boundary = if (forward) p * widthPx else (1f - p) * widthPx
    return if (forward) {
        CurlSlice(
            boundaryX = boundary,
            backLeft = boundary - back,
            backRight = boundary,
            shadowLeft = boundary - shadow,
            shadowRight = boundary,
        )
    } else {
        CurlSlice(
            boundaryX = boundary,
            backLeft = boundary,
            backRight = boundary + back,
            shadowLeft = boundary,
            shadowRight = boundary + shadow,
        )
    }
}

/**
 * Test hook: the page indexes the Curl pager composed. Slide mode never
 * touches it, and Curl mode only ever composes the settled page and one
 * neighbour — never the other 602.
 */
internal object MushafCurlProbe {
    val composedPages: MutableSet<Int> = java.util.Collections.synchronizedSet(HashSet())

    fun reset() {
        composedPages.clear()
    }
}

/**
 * Curl gestures on the page stack.
 *
 * Runs on [PointerEventPass.Initial] so the container sees events before
 * [MushafPage]: it consumes only horizontal drags past touch slop and taps in
 * the frame margins. A tap inside the text column is left untouched and stays
 * an ayah tap or long-press. Vertical movement past slop cancels.
 */
internal fun Modifier.curlGestures(
    touchSlop: Float,
    longPressTimeoutMs: Long,
    marginTurn: (Offset) -> Boolean,
    onDragStart: (forward: Boolean) -> Unit,
    onDrag: (deltaX: Float) -> Unit,
    onDragEnd: () -> Unit,
    onTapMargin: (forward: Boolean) -> Unit,
): Modifier = pointerInput(touchSlop, longPressTimeoutMs) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        val pointerId = down.id
        var dragging = false
        var cancelled = false
        var totalDx = 0f
        var totalDy = 0f
        var lastX = down.position.x
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            val change = event.changes.firstOrNull { it.id == pointerId } ?: break
            if (!change.pressed) {
                // up — a short, still press in the margin turns the leaf.
                if (!dragging && !cancelled &&
                    change.uptimeMillis - down.uptimeMillis < longPressTimeoutMs &&
                    abs(totalDx) < touchSlop && abs(totalDy) < touchSlop
                ) {
                    if (marginTurn(change.position)) {
                        change.consume()
                        onTapMargin(change.position.x < size.width / 2f)
                    }
                }
                break
            }
            if (dragging) {
                change.consume()
                onDrag(change.position.x - lastX)
                lastX = change.position.x
            } else {
                totalDx = change.position.x - down.position.x
                totalDy = change.position.y - down.position.y
                when {
                    abs(totalDx) > touchSlop && abs(totalDx) > abs(totalDy) -> {
                        dragging = true
                        change.consume()
                        onDragStart(totalDx > 0f)
                        onDrag(change.position.x - lastX)
                        lastX = change.position.x
                    }
                    abs(totalDy) > touchSlop -> {
                        cancelled = true
                    }
                }
            }
        }
        if (dragging) onDragEnd()
    }
}
