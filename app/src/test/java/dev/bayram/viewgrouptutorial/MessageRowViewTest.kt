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
class MessageRowViewTest {
    private fun inflate(): MessageRowView =
        LayoutInflater.from(RuntimeEnvironment.getApplication())
            .inflate(R.layout.message_row_example, null)
            .findViewById(R.id.message_row)

    @Test
    fun `given different widths and reading directions - on layout - it should reserve time space and align the body with the subject`() {
        // SETUP
        val view = inflate()
        val icon = view.findViewById<ImageView>(R.id.message_icon)
        val subject = view.findViewById<TextView>(R.id.message_subject)
        val time = view.findViewById<TextView>(R.id.message_time)
        val body = view.findViewById<TextView>(R.id.message_body)
        val iconGap = view.resources.getDimensionPixelSize(R.dimen.message_icon_gap)
        val headerGap = view.resources.getDimensionPixelSize(R.dimen.message_header_gap)
        val bodyGap = view.resources.getDimensionPixelSize(R.dimen.message_body_gap)

        for (width in listOf(240, 600, 1000)) {
            for (direction in listOf(View.LAYOUT_DIRECTION_LTR, View.LAYOUT_DIRECTION_RTL)) {
                view.layoutDirection = direction

                // RUN
                layout(view, width)

                // ASSERT
                assertEquals(width, view.width)
                assertEquals(view.paddingTop, icon.top)
                assertEquals(view.paddingTop, subject.top)
                assertEquals(subject.top, time.top)
                assertEquals(maxOf(subject.bottom, time.bottom) + bodyGap, body.top)
                if (direction == View.LAYOUT_DIRECTION_LTR) {
                    assertEquals(iconGap, subject.left - icon.right)
                    assertEquals(subject.left, body.left)
                    assertEquals(width - view.paddingRight, time.right)
                    assertTrue(time.left - subject.right >= headerGap)
                } else {
                    assertEquals(iconGap, icon.left - subject.right)
                    assertEquals(subject.right, body.right)
                    assertEquals(view.paddingLeft, time.left)
                    assertTrue(subject.left - time.right >= headerGap)
                }
                assertEquals(maxOf(icon.bottom, body.bottom) + view.paddingBottom, view.height)
                assertTrue(body.left >= view.paddingLeft)
                assertTrue(body.right <= width - view.paddingRight)
            }
        }
    }

    @Test
    fun `given a longer timestamp - on measuring - it should leave less width for the subject but not the body`() {
        // SETUP
        val view = inflate()
        val subject = view.findViewById<TextView>(R.id.message_subject)
        val time = view.findViewById<TextView>(R.id.message_time)
        val body = view.findViewById<TextView>(R.id.message_body)
        subject.text = "A very long subject that needs several lines beside the timestamp"
        time.text = "17:17"
        layout(view, 400)
        val shortTimeWidth = time.width
        val shortSubjectWidth = subject.width
        val originalBodyWidth = body.width

        // RUN
        time.text = "Yesterday 17:17"
        layout(view, 400)

        // ASSERT
        assertTrue(time.width > shortTimeWidth)
        assertTrue(subject.width < shortSubjectWidth)
        assertEquals(originalBodyWidth, body.width)
        assertTrue(body.width > subject.width)
    }

    @Test
    fun `given either header text is taller - on layout - it should put the body below both texts`() {
        // SETUP
        val view = inflate()
        val subject = view.findViewById<TextView>(R.id.message_subject)
        val time = view.findViewById<TextView>(R.id.message_time)
        val body = view.findViewById<TextView>(R.id.message_body)
        val gap = view.resources.getDimensionPixelSize(R.dimen.message_body_gap)

        for ((subjectText, timeText) in listOf("One\nTwo\nThree" to "17:17", "Subject" to "Today\n17:17\nUTC\n+02")) {
            subject.text = subjectText
            time.text = timeText

            // RUN
            layout(view, 600)

            // ASSERT
            assertEquals(maxOf(subject.bottom, time.bottom) + gap, body.top)
            assertEquals(body.bottom + view.paddingBottom, view.height)
        }
    }

    private fun layout(view: MessageRowView, width: Int) {
        view.measure(
            MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        )
        view.layout(0, 0, view.measuredWidth, view.measuredHeight)
    }
}
