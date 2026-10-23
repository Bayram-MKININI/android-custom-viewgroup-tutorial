package dev.bayram.viewgrouptutorial

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView

/**
 * Adds a corner badge to Yumi's message-row rules, reserving its full bounds.
 * Requires exact width, unbounded height, and five visible XML children without margins.
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
    private lateinit var badge: TextView
    private val iconGroup = Rect()
    private val iconSize = resources.getDimensionPixelSize(R.dimen.message_icon_size)
    private val iconGap = resources.getDimensionPixelSize(R.dimen.message_icon_gap)
    private val headerGap = resources.getDimensionPixelSize(R.dimen.message_header_gap)
    private val bodyGap = resources.getDimensionPixelSize(R.dimen.message_body_gap)

    override fun onFinishInflate() {
        super.onFinishInflate()
        require(childCount == 5) { "MessageRowView requires exactly five children" }
        icon = requireNotNull(findViewById<ImageView>(R.id.message_icon))
        subject = requireNotNull(findViewById<TextView>(R.id.message_subject))
        time = requireNotNull(findViewById<TextView>(R.id.message_time))
        body = requireNotNull(findViewById<TextView>(R.id.message_body))
        badge = requireNotNull(findViewById<TextView>(R.id.message_badge))
        require(listOf(icon, subject, time, body, badge).all { it.parent === this }) {
            "All five views must be direct children"
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        require(MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.EXACTLY) {
            "MessageRowView requires exact width"
        }
        require(MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.UNSPECIFIED) {
            "MessageRowView requires unbounded height; use the sample ScrollView"
        }
        require(listOf(icon, subject, time, body, badge).all { it.visibility == VISIBLE }) {
            "This example requires all five children to be visible"
        }
        val contentWidth = MeasureSpec.getSize(widthMeasureSpec) - paddingLeft - paddingRight
        require(contentWidth > 0) { "Not enough width for the message row" }

        val unlimitedHeight = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        val iconSpec = MeasureSpec.makeMeasureSpec(iconSize, MeasureSpec.EXACTLY)
        icon.measure(iconSpec, iconSpec)
        badge.measure(
            MeasureSpec.makeMeasureSpec(contentWidth, MeasureSpec.AT_MOST),
            unlimitedHeight
        )
        val badgeLeft = icon.measuredWidth - badge.measuredWidth / 2
        val badgeTop = -badge.measuredHeight / 2
        iconGroup.set(0, 0, icon.measuredWidth, icon.measuredHeight)
        iconGroup.union(
            badgeLeft, badgeTop,
            badgeLeft + badge.measuredWidth, badgeTop + badge.measuredHeight
        )
        val textAreaWidth = contentWidth - iconGroup.width() - iconGap
        require(textAreaWidth > headerGap) { "Not enough width for the message row" }

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
            iconGroup.height(),
            headerHeight + bodyGap + body.measuredHeight
        )
        val childState = listOf(icon, subject, time, body, badge).fold(0) { state, child ->
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
        val groupLeft = if (rtl) contentRight - iconGroup.width() else paddingLeft
        val textStart = if (rtl) groupLeft - iconGap
            else groupLeft + iconGroup.width() + iconGap
        val iconLeft = groupLeft - iconGroup.left
        val iconTop = paddingTop - iconGroup.top
        val badgeLeft = iconLeft + icon.measuredWidth - badge.measuredWidth / 2
        val badgeTop = iconTop - badge.measuredHeight / 2
        val subjectLeft = if (rtl) textStart - subject.measuredWidth else textStart
        val timeLeft = if (rtl) paddingLeft else contentRight - time.measuredWidth
        val bodyLeft = if (rtl) textStart - body.measuredWidth else textStart

        icon.layout(iconLeft, iconTop, iconLeft + icon.measuredWidth, iconTop + icon.measuredHeight)
        badge.layout(
            badgeLeft, badgeTop,
            badgeLeft + badge.measuredWidth, badgeTop + badge.measuredHeight
        )
        subject.layout(
            subjectLeft, paddingTop,
            subjectLeft + subject.measuredWidth, paddingTop + subject.measuredHeight
        )
        time.layout(timeLeft, paddingTop, timeLeft + time.measuredWidth, paddingTop + time.measuredHeight)
        val bodyTop = maxOf(subject.bottom, time.bottom) + bodyGap
        body.layout(bodyLeft, bodyTop, bodyLeft + body.measuredWidth, bodyTop + body.measuredHeight)
    }
}
