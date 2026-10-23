package dev.bayram.viewgrouptutorial

import android.view.LayoutInflater
import android.view.View
import android.view.View.MeasureSpec
import android.widget.ImageView
import android.widget.TextView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ImageTextViewTest {
    private fun inflate(): ImageTextView =
        LayoutInflater.from(RuntimeEnvironment.getApplication())
            .inflate(R.layout.image_text_example, null)
            .findViewById(R.id.image_text)

    @Test
    fun `given different widths and text lengths - on laying out - it should preserve the size and gap ratios`() {
        // SETUP
        val view = inflate()
        val image = view.findViewById<ImageView>(R.id.example_image)
        val text = view.findViewById<TextView>(R.id.example_text)
        val longText = text.text

        for (width in listOf(180, 360, 900)) {
            for (content in listOf("Hi", longText)) {
                text.text = content

                // RUN
                view.measure(
                    MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
                )
                view.layout(0, 0, view.measuredWidth, view.measuredHeight)

                // ASSERT
                assertEquals(width, view.measuredWidth)
                assertEquals((text.measuredWidth * 0.5).toInt(), image.width)
                assertEquals(image.width, image.height)
                assertEquals((text.measuredWidth * 0.3).toInt(), text.left - image.right)
                assertEquals(view.paddingLeft, image.left)
                assertTrue(text.right <= width - view.paddingRight)
                assertTrue(image.bottom <= view.height - view.paddingBottom)
                assertTrue(text.bottom <= view.height - view.paddingBottom)
                assertTrue(kotlin.math.abs((image.top + image.bottom) - (text.top + text.bottom)) <= 1)
            }
        }
    }

    @Test
    fun `given right to left direction - on laying out - it should keep the image physically left of the text`() {
        // SETUP
        val view = inflate()
        view.layoutDirection = View.LAYOUT_DIRECTION_RTL

        // RUN
        view.measure(
            MeasureSpec.makeMeasureSpec(600, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        )
        view.layout(0, 0, view.measuredWidth, view.measuredHeight)

        // ASSERT
        assertTrue(
            view.findViewById<ImageView>(R.id.example_image).right <
                view.findViewById<TextView>(R.id.example_text).left
        )
    }

    @Test
    fun `given a width limit - on measuring - it should fit the row and padding within the limit`() {
        // SETUP
        val view = inflate()

        // RUN
        view.measure(
            MeasureSpec.makeMeasureSpec(600, MeasureSpec.AT_MOST),
            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        )
        view.layout(0, 0, view.measuredWidth, view.measuredHeight)

        // ASSERT
        assertTrue(view.measuredWidth <= 600)
        assertTrue(view.findViewById<TextView>(R.id.example_text).right <= view.width - view.paddingRight)
    }
}
