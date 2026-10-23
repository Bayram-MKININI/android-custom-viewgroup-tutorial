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
    fun `given a title of at most two lines - on layout - it should keep the thumbnail beside both texts`() {
        // SETUP
        val view = inflate()
        val image = view.findViewById<ImageView>(R.id.article_image)
        val title = view.findViewById<TextView>(R.id.article_title)
        val description = view.findViewById<TextView>(R.id.article_description)
        val columnGap = view.resources.getDimensionPixelSize(R.dimen.article_column_gap)
        val textGap = view.resources.getDimensionPixelSize(R.dimen.article_text_gap)

        for (content in listOf("Walk", "Walk\nToday")) {
            for (direction in listOf(View.LAYOUT_DIRECTION_LTR, View.LAYOUT_DIRECTION_RTL)) {
                title.text = content
                view.layoutDirection = direction

                // RUN
                layout(view, 600)

                // ASSERT
                assertTrue(title.lineCount <= 2)
                assertEquals((content.count { it == '\n' } + 1), title.lineCount)
                val contentWidth = view.width - view.paddingLeft - view.paddingRight
                assertEquals((contentWidth * 0.3).toInt(), image.width)
                assertEquals(image.width, image.height)
                assertEquals(view.paddingTop, image.top)
                assertEquals(image.top, title.top)
                assertEquals(textGap, description.top - title.bottom)
                assertEquals(title.left, description.left)
                assertEquals(title.right, description.right)
                if (direction == View.LAYOUT_DIRECTION_LTR) {
                    assertEquals(columnGap, title.left - image.right)
                } else {
                    assertEquals(columnGap, image.left - title.right)
                }
                assertEquals(
                    view.paddingTop + maxOf(image.height, title.height + textGap + description.height) +
                        view.paddingBottom,
                    view.height
                )
            }
        }
    }

    @Test
    fun `given a three line title - on layout - it should put a full width image above both texts`() {
        // SETUP
        val view = inflate()
        val image = view.findViewById<ImageView>(R.id.article_image)
        val title = view.findViewById<TextView>(R.id.article_title)
        val description = view.findViewById<TextView>(R.id.article_description)
        title.text = "Walk\nThrough\nTown"
        val gap = view.resources.getDimensionPixelSize(R.dimen.article_column_gap)
        val textGap = view.resources.getDimensionPixelSize(R.dimen.article_text_gap)

        for (direction in listOf(View.LAYOUT_DIRECTION_LTR, View.LAYOUT_DIRECTION_RTL)) {
            view.layoutDirection = direction

            // RUN
            layout(view, 600)

            // ASSERT
            assertEquals(3, title.lineCount)
            val contentWidth = view.width - view.paddingLeft - view.paddingRight
            assertEquals(contentWidth, image.width)
            assertEquals((contentWidth * 9.0 / 16).toInt(), image.height)
            assertEquals(view.paddingLeft, image.left)
            assertEquals(view.paddingLeft, title.left)
            assertEquals(contentWidth, title.width)
            assertEquals(gap, title.top - image.bottom)
            assertEquals(textGap, description.top - title.bottom)
            assertEquals(title.left, description.left)
            assertEquals(title.right, description.right)
            assertEquals(description.bottom + view.paddingBottom, view.height)
        }
    }

    @Test
    fun `given changing text at the same width - on layout - it should switch arrangements in both directions`() {
        // SETUP
        val view = inflate()
        val title = view.findViewById<TextView>(R.id.article_title)
        val image = view.findViewById<ImageView>(R.id.article_image)
        val width = 400

        // RUN
        title.text = "Walk"
        layout(view, width)
        val shortTitleTop = title.top
        title.text = "A long walk through the quiet streets of a city with many places to discover"
        layout(view, width)
        val longTitleTop = title.top
        val stackedImageWidth = image.width
        title.text = "Walk"
        layout(view, width)

        // ASSERT
        assertTrue(longTitleTop > shortTitleTop)
        assertEquals(width - view.paddingLeft - view.paddingRight, stackedImageWidth)
        assertEquals(image.top, title.top)
        assertTrue(image.width < stackedImageWidth)
    }

    @Test
    fun `given the same title at different widths - on layout - it should choose based on wrapped text`() {
        // SETUP
        val view = inflate()
        val title = view.findViewById<TextView>(R.id.article_title)
        val image = view.findViewById<ImageView>(R.id.article_image)
        title.text = "A quiet walk through the city"

        // RUN
        layout(view, 240)
        val narrowImageWidth = image.width
        val narrowTitleTop = title.top
        layout(view, 1000)

        // ASSERT
        assertEquals(240 - view.paddingLeft - view.paddingRight, narrowImageWidth)
        assertTrue(narrowTitleTop > view.paddingTop)
        assertEquals(view.paddingTop, title.top)
        assertTrue(title.lineCount <= 2)
    }

    private fun layout(view: ArticlePreviewView, width: Int) {
        view.measure(
            MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        )
        view.layout(0, 0, view.measuredWidth, view.measuredHeight)
    }
}
