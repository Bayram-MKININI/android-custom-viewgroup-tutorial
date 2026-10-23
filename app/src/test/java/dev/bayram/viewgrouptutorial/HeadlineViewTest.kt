package dev.bayram.viewgrouptutorial

import android.view.LayoutInflater
import android.view.View
import android.view.View.MeasureSpec
import android.view.ViewGroup
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
class HeadlineViewTest {
    private fun inflate(): HeadlineView =
        LayoutInflater.from(RuntimeEnvironment.getApplication())
            .inflate(R.layout.activity_main, null)
            .findViewById(R.id.headline)

    @Test
    fun `given exact bounds - on measuring - it should keep the requested size`() {
        // SETUP
        val view = inflate()

        // RUN
        view.measure(spec(600, MeasureSpec.EXACTLY), spec(400, MeasureSpec.EXACTLY))

        // ASSERT
        assertEquals(600, view.measuredWidth)
        assertEquals(400, view.measuredHeight)
    }

    @Test
    fun `given unbounded height - on measuring - it should fit both texts and padding`() {
        // SETUP
        val view = inflate()
        val title = view.findViewById<TextView>(R.id.title)
        val summary = view.findViewById<TextView>(R.id.summary)
        val gap = view.resources.getDimensionPixelSize(R.dimen.headline_gap)

        // RUN
        view.measure(spec(600, MeasureSpec.EXACTLY), spec(0, MeasureSpec.UNSPECIFIED))

        // ASSERT
        assertTrue(title.measuredHeight > 0)
        assertTrue(summary.measuredHeight > 0)
        assertEquals(
            view.paddingTop + title.measuredHeight + gap + summary.measuredHeight + view.paddingBottom,
            view.measuredHeight
        )
    }

    @Test
    fun `given a size limit - on measuring - it should stay within the bounds`() {
        // SETUP
        val view = inflate()

        // RUN
        view.measure(spec(180, MeasureSpec.AT_MOST), spec(100, MeasureSpec.AT_MOST))

        // ASSERT
        assertTrue(view.measuredWidth <= 180)
        assertTrue(view.measuredHeight <= 100)
    }

    @Test
    fun `given a hidden summary - on measuring - it should omit the summary and gap`() {
        // SETUP
        val view = inflate()
        val title = view.findViewById<TextView>(R.id.title)
        view.findViewById<TextView>(R.id.summary).visibility = View.GONE

        // RUN
        view.measure(spec(600, MeasureSpec.EXACTLY), spec(0, MeasureSpec.UNSPECIFIED))

        // ASSERT
        assertEquals(
            view.paddingTop + title.measuredHeight + view.paddingBottom,
            view.measuredHeight
        )
    }

    @Test
    fun `given right to left text - on laying out - it should align children to the start edge`() {
        // SETUP
        val view = inflate()
        view.layoutDirection = View.LAYOUT_DIRECTION_RTL
        val title = view.findViewById<TextView>(R.id.title)
        val summary = view.findViewById<TextView>(R.id.summary)
        title.layoutParams.width = ViewGroup.LayoutParams.WRAP_CONTENT
        title.text = "Short"
        val gap = view.resources.getDimensionPixelSize(R.dimen.headline_gap)

        // RUN
        view.measure(spec(600, MeasureSpec.EXACTLY), spec(0, MeasureSpec.UNSPECIFIED))
        view.layout(0, 0, view.measuredWidth, view.measuredHeight)

        // ASSERT
        assertEquals(view.width - view.paddingRight, title.right)
        assertEquals(view.paddingTop, title.top)
        assertEquals(title.bottom + gap, summary.top)
    }

    @Test
    fun `given new content - on updating - it should display both new texts`() {
        // SETUP
        val view = inflate()
        view.measure(spec(600, MeasureSpec.EXACTLY), spec(0, MeasureSpec.UNSPECIFIED))
        view.layout(0, 0, view.measuredWidth, view.measuredHeight)

        // RUN
        view.setContent("A changed headline", "A changed summary")

        // ASSERT
        assertEquals("A changed headline", view.findViewById<TextView>(R.id.title).text.toString())
        assertEquals("A changed summary", view.findViewById<TextView>(R.id.summary).text.toString())
    }

    private fun spec(size: Int, mode: Int): Int = MeasureSpec.makeMeasureSpec(size, mode)
}
