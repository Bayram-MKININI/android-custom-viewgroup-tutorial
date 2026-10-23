package dev.bayram.viewgrouptutorial

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView

/**
 * Sizes the image and gap from the measured text width, not the parent width.
 * Expects bounded width and unbounded height, as supplied by the sample ScrollView.
 * Image-left is physical left, including in RTL; margins and hidden children are not supported.
 */
class ImageTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ViewGroup(context, attrs, defStyleAttr) {

    private lateinit var image: ImageView
    private lateinit var text: TextView
    private var gap = 0

    override fun onFinishInflate() {
        super.onFinishInflate()
        require(childCount == 2) { "ImageTextView requires exactly two children" }
        image = requireNotNull(findViewById<ImageView>(R.id.example_image))
        text = requireNotNull(findViewById<TextView>(R.id.example_text))
        require(image.parent === this && text.parent === this) {
            "Image and text must be direct children"
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        require(MeasureSpec.getMode(widthMeasureSpec) != MeasureSpec.UNSPECIFIED) {
            "ImageTextView requires a bounded width"
        }
        require(MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.UNSPECIFIED) {
            "ImageTextView requires unbounded height; place it inside a ScrollView"
        }
        require(image.visibility == VISIBLE && text.visibility == VISIBLE) {
            "This example requires both children to be visible"
        }
        val availableWidth = (
            MeasureSpec.getSize(widthMeasureSpec) - paddingLeft - paddingRight
        ).coerceAtLeast(0)

        val textWidthLimit = (availableWidth / 1.8).toInt()
        text.measure(
            MeasureSpec.makeMeasureSpec(textWidthLimit, MeasureSpec.AT_MOST),
            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        )

        val imageSide = (text.measuredWidth * 0.5).toInt()
        gap = (text.measuredWidth * 0.3).toInt()
        image.measure(
            MeasureSpec.makeMeasureSpec(imageSide, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(imageSide, MeasureSpec.EXACTLY)
        )

        val childState = combineMeasuredStates(text.measuredState, image.measuredState)
        setMeasuredDimension(
            resolveSizeAndState(
                maxOf(
                    suggestedMinimumWidth,
                    paddingLeft + imageSide + gap + text.measuredWidth + paddingRight
                ),
                widthMeasureSpec,
                childState
            ),
            resolveSizeAndState(
                maxOf(
                    suggestedMinimumHeight,
                    paddingTop + maxOf(imageSide, text.measuredHeight) + paddingBottom
                ),
                heightMeasureSpec,
                childState shl MEASURED_HEIGHT_STATE_SHIFT
            )
        )
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        val rowHeight = maxOf(image.measuredHeight, text.measuredHeight)
        val imageLeft = paddingLeft
        val imageTop = paddingTop + (rowHeight - image.measuredHeight) / 2
        image.layout(
            imageLeft,
            imageTop,
            imageLeft + image.measuredWidth,
            imageTop + image.measuredHeight
        )

        val textLeft = image.right + gap
        val textTop = paddingTop + (rowHeight - text.measuredHeight) / 2
        text.layout(
            textLeft,
            textTop,
            textLeft + text.measuredWidth,
            textTop + text.measuredHeight
        )
    }
}
