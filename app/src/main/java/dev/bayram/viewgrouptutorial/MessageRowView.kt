package dev.bayram.viewgrouptutorial

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView

/**
 * Reserves timestamp space before measuring the subject, as in Yumi's message row.
 * Requires exact width, unbounded height, and four visible XML children without margins.
 */
class MessageRowView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ViewGroup(context, attrs, defStyleAttr) {

    private lateinit var icon: ImageView
    private lateinit var subject: TextView
    private lateinit var time: TextView
    private lateinit var body: TextView
    private val iconSize = resources.getDimensionPixelSize(R.dimen.message_icon_size)
    private val iconGap = resources.getDimensionPixelSize(R.dimen.message_icon_gap)
    private val headerGap = resources.getDimensionPixelSize(R.dimen.message_header_gap)
    private val bodyGap = resources.getDimensionPixelSize(R.dimen.message_body_gap)

    override fun onFinishInflate() {
        super.onFinishInflate()
        require(childCount == 4) { "MessageRowView requires exactly four children" }
        icon = requireNotNull(findViewById<ImageView>(R.id.message_icon))
        subject = requireNotNull(findViewById<TextView>(R.id.message_subject))
        time = requireNotNull(findViewById<TextView>(R.id.message_time))
        body = requireNotNull(findViewById<TextView>(R.id.message_body))
        require(listOf(icon, subject, time, body).all { it.parent === this }) {
            "All four views must be direct children"
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        require(MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.EXACTLY) {
            "MessageRowView requires exact width"
        }
        require(MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.UNSPECIFIED) {
            "MessageRowView requires unbounded height; use the sample ScrollView"
        }
        require(listOf(icon, subject, time, body).all { it.visibility == VISIBLE }) {
            "This example requires all four children to be visible"
        }
        val contentWidth = MeasureSpec.getSize(widthMeasureSpec) - paddingLeft - paddingRight
        val textAreaWidth = contentWidth - iconSize - iconGap
        require(textAreaWidth > headerGap) { "Not enough width for the message row" }

        val unlimitedHeight = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        val iconSpec = MeasureSpec.makeMeasureSpec(iconSize, MeasureSpec.EXACTLY)
        icon.measure(iconSpec, iconSpec)
        time.measure(
            MeasureSpec.makeMeasureSpec(textAreaWidth, MeasureSpec.AT_MOST),
            unlimitedHeight
        )

        val subjectWidth = textAreaWidth - time.measuredWidth - headerGap
        require(subjectWidth > 0) { "Timestamp leaves no space for the subject" }
        subject.measure(
            MeasureSpec.makeMeasureSpec(subjectWidth, MeasureSpec.AT_MOST),
            unlimitedHeight
        )
        body.measure(
            MeasureSpec.makeMeasureSpec(textAreaWidth, MeasureSpec.AT_MOST),
            unlimitedHeight
        )

        val headerHeight = maxOf(subject.measuredHeight, time.measuredHeight)
        val contentHeight = maxOf(
            icon.measuredHeight,
            headerHeight + bodyGap + body.measuredHeight
        )
        val childState = listOf(icon, subject, time, body).fold(0) { state, child ->
            combineMeasuredStates(state, child.measuredState)
        }
        setMeasuredDimension(
            resolveSizeAndState(MeasureSpec.getSize(widthMeasureSpec), widthMeasureSpec, childState),
            resolveSizeAndState(
                maxOf(suggestedMinimumHeight, paddingTop + contentHeight + paddingBottom),
                heightMeasureSpec,
                childState shl MEASURED_HEIGHT_STATE_SHIFT
            )
        )
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        val rtl = layoutDirection == LAYOUT_DIRECTION_RTL
        val contentRight = right - left - paddingRight
        val textStart = if (rtl) contentRight - icon.measuredWidth - iconGap
            else paddingLeft + icon.measuredWidth + iconGap
        val iconLeft = if (rtl) contentRight - icon.measuredWidth else paddingLeft
        val subjectLeft = if (rtl) textStart - subject.measuredWidth else textStart
        val timeLeft = if (rtl) paddingLeft else contentRight - time.measuredWidth
        val bodyLeft = if (rtl) textStart - body.measuredWidth else textStart

        icon.layout(iconLeft, paddingTop, iconLeft + icon.measuredWidth, paddingTop + icon.measuredHeight)
        subject.layout(
            subjectLeft, paddingTop,
            subjectLeft + subject.measuredWidth, paddingTop + subject.measuredHeight
        )
        time.layout(timeLeft, paddingTop, timeLeft + time.measuredWidth, paddingTop + time.measuredHeight)
        val bodyTop = maxOf(subject.bottom, time.bottom) + bodyGap
        body.layout(bodyLeft, bodyTop, bodyLeft + body.measuredWidth, bodyTop + body.measuredHeight)
    }
}
