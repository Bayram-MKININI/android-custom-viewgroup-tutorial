package dev.bayram.viewgrouptutorial

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView

/**
 * Keeps the image and title top-aligned, with a text gap based on title height.
 * Requires exact width, unbounded height, and three visible XML children without margins.
 */
class ArticlePreviewView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ViewGroup(context, attrs, defStyleAttr) {

    private lateinit var image: ImageView
    private lateinit var title: TextView
    private lateinit var description: TextView
    private val columnGap = resources.getDimensionPixelSize(R.dimen.article_column_gap)
    private var textGap = 0

    override fun onFinishInflate() {
        super.onFinishInflate()
        require(childCount == 3) { "ArticlePreviewView requires exactly three children" }
        image = requireNotNull(findViewById<ImageView>(R.id.article_image))
        title = requireNotNull(findViewById<TextView>(R.id.article_title))
        description = requireNotNull(findViewById<TextView>(R.id.article_description))
        require(image.parent === this && title.parent === this && description.parent === this) {
            "Image, title, and description must be direct children"
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        require(MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.EXACTLY) {
            "ArticlePreviewView requires exact width"
        }
        require(MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.UNSPECIFIED) {
            "ArticlePreviewView requires unbounded height; use the sample ScrollView"
        }
        require(image.visibility == VISIBLE && title.visibility == VISIBLE && description.visibility == VISIBLE) {
            "This example requires all three children to be visible"
        }
        val contentWidth = MeasureSpec.getSize(widthMeasureSpec) - paddingLeft - paddingRight
        val imageSide = (contentWidth * 0.3).toInt()
        val textWidth = contentWidth - imageSide - columnGap
        require(textWidth > 0) { "Not enough width for padding, image, and column gap" }

        val imageSpec = MeasureSpec.makeMeasureSpec(imageSide, MeasureSpec.EXACTLY)
        image.measure(imageSpec, imageSpec)

        val textWidthSpec = MeasureSpec.makeMeasureSpec(textWidth, MeasureSpec.EXACTLY)
        val textHeightSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        title.measure(textWidthSpec, textHeightSpec)
        textGap = (title.measuredHeight * 0.25).toInt()
        description.measure(textWidthSpec, textHeightSpec)

        val textColumnHeight = title.measuredHeight + textGap + description.measuredHeight
        val childState = combineMeasuredStates(
            image.measuredState,
            combineMeasuredStates(title.measuredState, description.measuredState)
        )
        setMeasuredDimension(
            resolveSizeAndState(
                MeasureSpec.getSize(widthMeasureSpec),
                widthMeasureSpec,
                childState
            ),
            resolveSizeAndState(
                maxOf(
                    suggestedMinimumHeight,
                    paddingTop + maxOf(imageSide, textColumnHeight) + paddingBottom
                ),
                heightMeasureSpec,
                childState shl MEASURED_HEIGHT_STATE_SHIFT
            )
        )
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        val rtl = layoutDirection == LAYOUT_DIRECTION_RTL
        val imageLeft = if (rtl) right - left - paddingRight - image.measuredWidth else paddingLeft
        val textLeft = if (rtl) paddingLeft else imageLeft + image.measuredWidth + columnGap

        image.layout(
            imageLeft,
            paddingTop,
            imageLeft + image.measuredWidth,
            paddingTop + image.measuredHeight
        )
        title.layout(
            textLeft,
            paddingTop,
            textLeft + title.measuredWidth,
            paddingTop + title.measuredHeight
        )
        val descriptionTop = title.bottom + textGap
        description.layout(
            textLeft,
            descriptionTop,
            textLeft + description.measuredWidth,
            descriptionTop + description.measuredHeight
        )
    }
}
