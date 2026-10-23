# Build a real message row with a custom ViewGroup

A message has a priority icon, a subject, a time, and a body.
The subject must leave room for the time.
The body can use more width.
Its position depends on the height of both header texts.

This is a real layout from [Yumi](https://github.com/Bayram-MKININI/Yumi).
We will use its rules to learn custom measurement and placement.
You need basic Kotlin and Android XML knowledge.

## Start here

Clone the project.
All examples use its package and resources.

Use Android Studio, JDK 17, and Android SDK 36:

```sh
git clone https://github.com/Bayram-MKININI/android-custom-viewgroup-tutorial.git
cd android-custom-viewgroup-tutorial
```

Open the project in Android Studio to set the SDK location.
Or set `ANDROID_HOME` to your SDK.

The message row is the default screen:

```sh
./gradlew :app:assembleDebug
./gradlew :app:installDebug
```

The install command needs a device or emulator running API 24 or newer.
Open **Custom ViewGroup Tutorial**.
The first build needs internet access.

## 1. Look at the real app

Read Yumi's original files:

- [MessageItemView.kt](https://github.com/Bayram-MKININI/Yumi/blob/5b4032fbfc7bdf707d5f597b137aeff3db819515/app/src/main/java/net/noliaware/yumi/feature_message/presentation/views/MessageItemView.kt)
- [message_item_layout.xml](https://github.com/Bayram-MKININI/Yumi/blob/5b4032fbfc7bdf707d5f597b137aeff3db819515/app/src/main/res/layout/message_item_layout.xml)

The original parent measures and places four children.
It measures the icon and time before the subject.
It gives the body more width than the subject.
Then it puts the body below the taller header text.
The body preview has a five-line limit.

Our sample keeps those rules.
It uses new teaching code, simpler styling, and different spacing.
It also adds RTL support and explicit size limits.
It does not copy the app's implementation or reproduce its exact appearance.

![Message row with priority icon, subject, timestamp, and body. The body starts below both header texts and extends below the timestamp.](docs/images/message-row.svg)

## 2. Start with a simple arrangement

You could put the icon beside a text column.
Inside that column, put a header row above the body:

```text
Horizontal layout
    -> Icon
    -> Vertical layout
        -> Horizontal header
            -> Subject
            -> Time
        -> Body
```

This is a valid solution.
Nested layouts are not wrong.

But the sizes are not all independent.
The time must keep its space.
The subject must wrap before it reaches the time.
The body must start below both texts, not only below the subject.

Try a longer timestamp, such as "Yesterday 17:17".
It needs more width.
That leaves less width for the subject.
But it must not reduce the body's width.

These are the rules we will write directly in Kotlin.

### Do we need a custom ViewGroup?

Not strictly.
**ConstraintLayout can also express this design.**
It can constrain the subject before the time and use a bottom barrier for the body.

This example does not prove that standard layouts are unable to do the job.
It shows another way to express a real app layout.
The benefit is direct size calculations in one parent.
Whether that is clearer is a choice for your project.

## 3. Give each method one job

A `ViewGroup` is a View that contains other Views.
It is the **parent**.
The Views inside it are its **children**.

Our parent has four direct children:

```text
MessageRowView
    -> Priority ImageView
    -> Subject TextView
    -> Time TextView
    -> Body TextView
```

Android separates three jobs:

| Method | Job |
| --- | --- |
| `onMeasure()` | Measure children and report the parent's size |
| `onLayout()` | Assign each child a rectangle |
| `onDraw()` | Paint this View's own custom content |

We only need the first two.
The children already know how to draw text and images.
Android draws them through `dispatchDraw()`.
Our parent has no extra content to paint in `onDraw()`.

![The three jobs: measure sizes, assign rectangles, and draw content.](docs/images/viewgroup-phases.svg)

## 4. Measure the time before the subject

First, find the width available after padding and the icon:

```kotlin
val contentWidth = MeasureSpec.getSize(widthMeasureSpec) - paddingLeft - paddingRight
val textAreaWidth = contentWidth - iconSize - iconGap
```

All measurement code uses pixels.
The resource values, such as `16dp`, are converted by Android.
The width comes from the parent, not the full screen.

Android describes each size with a `MeasureSpec`:

| Mode | Meaning |
| --- | --- |
| `EXACTLY` | Use this size |
| `AT_MOST` | Use up to this size |
| `UNSPECIFIED` | No limit for this width or height |

The icon gets an exact size.
The timestamp gets up to the full text area:

```kotlin
val unlimitedHeight = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
val iconSpec = MeasureSpec.makeMeasureSpec(iconSize, MeasureSpec.EXACTLY)
icon.measure(iconSpec, iconSpec)
time.measure(
    MeasureSpec.makeMeasureSpec(textAreaWidth, MeasureSpec.AT_MOST),
    unlimitedHeight
)
```

Now we know `time.measuredWidth`.
Remove it, plus the header gap, from the subject's width limit:

```kotlin
val subjectWidth = textAreaWidth - time.measuredWidth - headerGap
subject.measure(
    MeasureSpec.makeMeasureSpec(subjectWidth, MeasureSpec.AT_MOST),
    unlimitedHeight
)
```

For example:

```text
text area width      300px
timestamp width       50px
header gap            12px
--------------------------
subject width limit  238px
```

A wider time means a smaller subject limit.
The sample reports an error if the timestamp leaves no subject space.
A production design should define a fallback for that case.

## 5. Give the body its own width

The timestamp only reserves space on the header row.
Do not remove its width when measuring the body:

```kotlin
body.measure(
    MeasureSpec.makeMeasureSpec(textAreaWidth, MeasureSpec.AT_MOST),
    unlimitedHeight
)
```

Now calculate the height:

```kotlin
val headerHeight = maxOf(subject.measuredHeight, time.measuredHeight)
val contentHeight = maxOf(
    icon.measuredHeight,
    headerHeight + bodyGap + body.measuredHeight
)
```

Why use `maxOf()`?
The subject may wrap onto several lines.
The time could also be taller, for example with a different text size.
The body must start below whichever is taller.
The row must also contain the icon.

Add top and bottom padding to get the parent's desired height.
Use `resolveSizeAndState()` and `setMeasuredDimension()` to report it.
Do not pass `MeasureSpec` values to `setMeasuredDimension()`.

## 6. Place the children in onLayout

Measurement gives sizes, not positions.
For left-to-right reading, place the children like this:

```text
Icon:    at the start padding
Subject: after the icon and icon gap
Time:    against the end padding
Body:    aligned with the subject, below both header texts
```

The key body rule is:

```kotlin
val bodyTop = maxOf(subject.bottom, time.bottom) + bodyGap
```

Do not use `subject.bottom` alone.
That could overlap a taller timestamp.

Each child receives `layout(left, top, right, bottom)`:

```kotlin
body.layout(
    bodyLeft,
    bodyTop,
    bodyLeft + body.measuredWidth,
    bodyTop + body.measuredHeight
)
```

These arguments are edges, not x, y, width, height.
Coordinates are inside this parent, not the screen.
The full class mirrors placement for right-to-left layouts.

## 7. The complete example

The row sits inside a `ScrollView`.
It requires an exact width and no height limit.
All four children must stay visible.
Child margins are not supported.
The parent owns the spacing.

### XML

Save this as `app/src/main/res/layout/message_row_example.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<ScrollView xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/scroll"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <dev.bayram.viewgrouptutorial.MessageRowView
        android:id="@+id/message_row"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:padding="@dimen/message_padding">

        <ImageView
            android:id="@+id/message_icon"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:contentDescription="@string/message_priority"
            android:src="@drawable/message_priority" />

        <TextView
            android:id="@+id/message_subject"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="@string/message_subject"
            android:textColor="?android:attr/textColorPrimary"
            android:textSize="16sp"
            android:textStyle="bold" />

        <TextView
            android:id="@+id/message_time"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="@string/message_time"
            android:textColor="?android:attr/textColorSecondary"
            android:textSize="14sp" />

        <TextView
            android:id="@+id/message_body"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:maxLines="5"
            android:text="@string/message_body"
            android:textColor="?android:attr/textColorPrimary"
            android:textSize="16sp" />
    </dev.bayram.viewgrouptutorial.MessageRowView>
</ScrollView>
```

The project includes the [strings and dimensions](app/src/main/res/values/strings.xml)
and [priority icon](app/src/main/res/drawable/message_priority.xml).
The icon has a translated description because it communicates priority.
The body is a five-line preview, as in Yumi.
This sample does not implement opening the full message.

The custom parent supplies child size rules itself.
Here, XML `wrap_content` does not choose the icon size or available text widths.

### Kotlin

This is the complete `MessageRowView.kt`:

```kotlin
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
```

`onFinishInflate()` finds the children after Android creates them from XML.
`require()` explains unsupported uses.
`combineMeasuredStates()` collects child size warnings.
`resolveSizeAndState()` passes them on under the parent's size rules.
Height warning bits need `MEASURED_HEIGHT_STATE_SHIFT`.

These are Android details around a short idea:
**reserve time space, measure the subject, place the body below both**.

## 8. Try changing the content

Edit the [string resources](app/src/main/res/values/strings.xml).

First, make the subject longer.
It should wrap without covering the time.
Then change the time from `"17:17"` to `"Yesterday 17:17"`.
The subject should get less room.
The body should keep its width limit.

Try a larger time text size too.
The body must stay below both header texts.
Also check large fonts, narrow windows, RTL, and screen-reader order.

Run the tests and Android lint:

```sh
./gradlew :app:testDebugUnitTest :app:lintDebug
```

The [tests](app/src/test/java/dev/bayram/viewgrouptutorial/MessageRowViewTest.kt)
check spacing, bounds, RTL, a wider timestamp, and either header text being taller.

## A short note about performance

Nested layouts can add measurement and placement work.
But Android does not always measure the whole tree again.
Our sample keeps four children under one parent.
ConstraintLayout can do that too.

This tutorial has no performance comparison.
It teaches direct control, not a proven speedup.

## References

- [Original Yumi MessageItemView](https://github.com/Bayram-MKININI/Yumi/blob/5b4032fbfc7bdf707d5f597b137aeff3db819515/app/src/main/java/net/noliaware/yumi/feature_message/presentation/views/MessageItemView.kt)
- [Teaching implementation](app/src/main/java/dev/bayram/viewgrouptutorial/MessageRowView.kt)
- [Teaching XML](app/src/main/res/layout/message_row_example.xml)
- [Custom Android components](https://developer.android.com/develop/ui/views/layout/custom-views/custom-components)
- [MeasureSpec](https://developer.android.com/reference/android/view/View.MeasureSpec)
