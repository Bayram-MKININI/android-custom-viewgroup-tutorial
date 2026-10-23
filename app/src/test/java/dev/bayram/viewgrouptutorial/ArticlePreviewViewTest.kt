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
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ArticlePreviewViewTest {
    private fun inflate(): ArticlePreviewView =
        LayoutInflater.from(RuntimeEnvironment.getApplication())
            .inflate(R.layout.article_preview_example, null)
            .findViewById(R.id.article_preview)

    @Test
    fun `given different widths and reading directions - on layout - it should preserve all size and alignment rules`() {
        // SETUP
        val view = inflate()
        val image = view.findViewById<ImageView>(R.id.article_image)
        val title = view.findViewById<TextView>(R.id.article_title)
        val description = view.findViewById<TextView>(R.id.article_description)
        val gap = view.resources.getDimensionPixelSize(R.dimen.article_column_gap)

        for (width in listOf(240, 600, 1000)) {
            for (direction in listOf(View.LAYOUT_DIRECTION_LTR, View.LAYOUT_DIRECTION_RTL)) {
                view.layoutDirection = direction

                // RUN
                view.measure(
                    MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
                )
                view.layout(0, 0, view.measuredWidth, view.measuredHeight)

                // ASSERT
                val contentWidth = width - view.paddingLeft - view.paddingRight
                assertEquals(width, view.width)
                assertEquals((contentWidth * 0.3).toInt(), image.width)
                assertEquals(image.width, image.height)
                assertEquals(title.top, image.top)
                assertEquals(view.paddingTop, title.top)
                assertEquals(title.left, description.left)
                assertEquals(title.right, description.right)
                assertEquals((title.height * 0.25).toInt(), description.top - title.bottom)
                assertEquals(contentWidth - image.width - gap, title.width)
                if (direction == View.LAYOUT_DIRECTION_LTR) {
                    assertEquals(gap, title.left - image.right)
                } else {
                    assertEquals(gap, image.left - title.right)
                }
                assertEquals(
                    view.paddingTop + maxOf(image.height, title.height +
                        (title.height * 0.25).toInt() + description.height) + view.paddingBottom,
                    view.height
                )
                assertTrue(description.right <= width - view.paddingRight)
                assertTrue(image.left >= view.paddingLeft)
            }
        }
    }

    @Test
    fun `given a longer title - on measuring again - it should move the description down and increase the gap`() {
        // SETUP
        val view = inflate()
        val title = view.findViewById<TextView>(R.id.article_title)
        val description = view.findViewById<TextView>(R.id.article_description)
        val widthSpec = MeasureSpec.makeMeasureSpec(400, MeasureSpec.EXACTLY)
        val heightSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        title.text = "Walk"
        view.measure(widthSpec, heightSpec)
        view.layout(0, 0, view.measuredWidth, view.measuredHeight)
        val oldTitleHeight = title.height
        val oldDescriptionTop = description.top
        val oldGap = description.top - title.bottom

        // RUN
        title.text = "A long walk through the quiet streets of a city with many places to discover"
        view.measure(widthSpec, heightSpec)
        view.layout(0, 0, view.measuredWidth, view.measuredHeight)

        // ASSERT
        assertTrue(title.height > oldTitleHeight)
        assertTrue(description.top > oldDescriptionTop)
        assertTrue(description.top - title.bottom > oldGap)
        assertEquals((title.height * 0.25).toInt(), description.top - title.bottom)
    }
}
