package dev.bayram.viewgrouptutorial

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

/**
 * Keeps XML responsible for text styling and Kotlin responsible for placement.
 * This layout expects exactly two wrap-content-height TextViews with the sample IDs.
 */
class HeadlineView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ViewGroup(context, attrs, defStyleAttr) {

    private lateinit var title: TextView
    private lateinit var summary: TextView
    private val gap = resources.getDimensionPixelSize(R.dimen.headline_gap)

    override fun onFinishInflate() {
        super.onFinishInflate()
        require(childCount == 2) { "HeadlineView requires exactly two children" }
        title = requireNotNull(findViewById<TextView>(R.id.title))
        summary = requireNotNull(findViewById<TextView>(R.id.summary))
        require(title.parent === this && summary.parent === this) {
            "Title and summary must be direct children"
        }
    }

    /** Changes text through TextView so size changes trigger layout automatically. */
    fun setContent(title: CharSequence, summary: CharSequence) {
        this.title.text = title
        this.summary.text = summary
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var contentHeight = 0
        var contentWidth = 0
        var childState = 0
        var visibleCount = 0

        for (index in 0 until childCount) {
            val child = getChildAt(index)
            if (child.visibility == View.GONE) continue
            if (visibleCount > 0) contentHeight += gap

            val childWidthSpec = getChildMeasureSpec(
                widthMeasureSpec,
                paddingLeft + paddingRight,
                child.layoutParams.width
            )
            val childHeightSpec = getChildMeasureSpec(
                heightMeasureSpec,
                paddingTop + paddingBottom + contentHeight,
                child.layoutParams.height
            )
            child.measure(childWidthSpec, childHeightSpec)
            contentWidth = maxOf(contentWidth, child.measuredWidth)
            contentHeight += child.measuredHeight
            childState = combineMeasuredStates(childState, child.measuredState)
            visibleCount++
        }

        setMeasuredDimension(
            resolveSizeAndState(
                maxOf(suggestedMinimumWidth, contentWidth + paddingLeft + paddingRight),
                widthMeasureSpec,
                childState
            ),
            resolveSizeAndState(
                maxOf(suggestedMinimumHeight, contentHeight + paddingTop + paddingBottom),
                heightMeasureSpec,
                childState shl MEASURED_HEIGHT_STATE_SHIFT
            )
        )
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        var childTop = paddingTop
        var visibleCount = 0

        for (index in 0 until childCount) {
            val child = getChildAt(index)
            if (child.visibility == View.GONE) continue
            if (visibleCount > 0) childTop += gap

            val childLeft = if (layoutDirection == View.LAYOUT_DIRECTION_RTL) {
                right - left - paddingRight - child.measuredWidth
            } else {
                paddingLeft
            }
            child.layout(
                childLeft,
                childTop,
                childLeft + child.measuredWidth,
                childTop + child.measuredHeight
            )
            childTop += child.measuredHeight
            visibleCount++
        }
    }

    override fun generateDefaultLayoutParams(): LayoutParams =
        LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
}
