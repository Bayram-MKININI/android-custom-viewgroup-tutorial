# Let the text choose the layout

You are building a news preview.
It has a photo, a title, and a description.
A short title looks good beside the photo.
A long title becomes hard to read in that narrow column.

What if the preview could switch layouts based on the actual text?

This guide builds a custom Android `ViewGroup` for that rule.
You need basic Kotlin and Android XML knowledge.
The idea comes from the custom layouts in [Yumi](https://github.com/Bayram-MKININI/Yumi).

## Start here

Clone this project.
All examples use its package and resources.
You do not need to create another Android project.

Use Android Studio, JDK 17, and Android SDK 36.

```sh
git clone https://github.com/Bayram-MKININI/android-custom-viewgroup-tutorial.git
cd android-custom-viewgroup-tutorial
```

Open it in Android Studio to set the SDK location.
Or set `ANDROID_HOME` to your SDK.

The finished preview is already the default screen:

```sh
./gradlew :app:assembleDebug
./gradlew :app:installDebug
```

The install command needs a device or emulator running API 24 or newer.
Open **Custom ViewGroup Tutorial**.
The first build needs internet access.

## 1. Start with a normal article row

The photo sits beside the title and description:

```text
+---------+    +------------------------+
|         |    | Title                  |
|  Photo  |    +------------------------+
|         |    | Description            |
+---------+    +------------------------+
```

A horizontal `LinearLayout` can hold the photo and a vertical text column.
`ConstraintLayout` can also place all three children in one parent.
Use those layouts when this is all you need.

Our row has simple spacing rules:

- The photo is square. Its side is 30% of the content width.
- The photo and title share the same top edge.
- The title and description have the same width.
- There is 16dp between the columns and 8dp between the texts.

**Content width** is the parent width minus its left and right padding.
The description may extend below the photo.

None of this alone requires a custom layout.

## 2. A longer title changes the problem

Now try a long headline.
It may need four or five lines beside the photo.
This creates a tall, narrow block of text.

The design team asks for a clear rule:

> Try the title beside the photo.
> If it needs more than two lines, put the photo above both texts.
> Give the texts the full content width.

![The same news preview in a row and a stacked arrangement, chosen from the title's trial line count.](docs/images/article-preview.svg)

In the stacked version, the photo becomes a full-width 16:9 image.
Keep the 16dp gap below it.
Keep the 8dp gap between the texts.
Do not cut or hide title lines in either version.

This is a real readability rule.
It reacts to the title, translation, font size, and available width.

## 3. A screen-width breakpoint is not enough

You could switch layouts below a fixed screen width.
But two titles can need different layouts at the **same width**.
A larger font can change the result too.

You need to know how the title actually wraps.
Counting characters is not enough.
Different letters and words take different space.

Standard `LinearLayout` and `ConstraintLayout` attributes cannot say:

```text
Choose the arrangement from this TextView's measured line count.
```

Adding nested layouts does not add that decision.
You need code.

**This is not impossible with standard layouts plus code.**
You could measure text and switch between two standard layouts.
Here, a custom parent keeps the decision and placement in one place.
It also avoids waiting for a displayed row before changing it.

## 4. Measure a candidate before choosing

A `ViewGroup` is a View that contains child Views.
Our parent has three direct children:

```text
ArticlePreviewView
    -> ImageView
    -> Title TextView
    -> Description TextView
```

Its measurement plan is:

```text
Calculate the row's text width
    -> measure the title at that width
        -> title needs at most 2 lines: keep the row
        -> title needs more than 2 lines: choose the stack
            -> measure the title again at full width
```

The decision always uses the **trial row width**.
Do not check the full-width title and switch back.
That could make the decision alternate between layouts.

## 5. Put the size decision in onMeasure

`onMeasure()` measures the children and reports the parent's size.
It does not place or draw them.

Android provides a `MeasureSpec` for each direction.
A spec contains a size and a rule:

| Mode | Meaning |
| --- | --- |
| `EXACTLY` | Use this size |
| `AT_MOST` | Use up to this size |
| `UNSPECIFIED` | No limit for this width or height |

Our sample receives an exact width.
A surrounding `ScrollView` lets it grow in height.

First, calculate the candidate row width:

```kotlin
val contentWidth = MeasureSpec.getSize(widthMeasureSpec) - paddingLeft - paddingRight
val thumbnailSide = (contentWidth * 0.3).toInt()
val rowTextWidth = contentWidth - thumbnailSide - columnGap
```

For example, with 400px of content width and a 16px column gap:

```text
thumbnail side = 120px
text width     = 400 - 120 - 16 = 264px
```

The actual column gap is a 16dp resource.
Android converts it to pixels using device density.
All our size calculations use pixels.

Now measure the title at that text width:

```kotlin
val textHeightSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
title.measure(
    MeasureSpec.makeMeasureSpec(rowTextWidth, MeasureSpec.EXACTLY),
    textHeightSpec
)
arrangement = if (title.lineCount <= 2) Arrangement.ROW else Arrangement.STACKED
```

After `measure()`, `TextView` knows its line count at that width.
There is no `maxLines` or ellipsis here.
We need the real number of lines, not a count already cut to two.

For the row, keep the measured title and square thumbnail.
For the stack, remeasure the title at the full content width.
Use that same width for the description.
The complete code below handles both cases.

The parent height follows the chosen arrangement:

```text
ROW:
    max(image height, title height + 8dp + description height)

STACKED:
    image height + 16dp + title height + 8dp + description height
```

Add top and bottom padding to either result.
Use `resolveSizeAndState()` and `setMeasuredDimension()` to report the size.

The stacked case measures the title twice.
That is deliberate: once to choose the layout, then once at its final width.
Custom layouts are not always single-pass layouts.

## 6. Place the chosen arrangement in onLayout

Now the child sizes are known.
`onLayout()` assigns their rectangles.
It uses the arrangement already chosen during measurement.

For the row:

```text
Image and title start at the top padding.
Text starts after the image and column gap.
Description starts below the title and text gap.
```

For the stack:

```text
Image starts at the top padding.
Title starts below the image and column gap.
Description starts below the title and text gap.
```

Each child receives `layout(left, top, right, bottom)`.
For example:

```kotlin
title.layout(
    textLeft,
    titleTop,
    textLeft + title.measuredWidth,
    titleTop + title.measuredHeight
)
```

These are edges, not x, y, width, height.
Coordinates are inside the parent, not the screen.
The row also mirrors for right-to-left layouts.

### Do we need onDraw?

No. `onDraw()` paints a View's own custom content.
It does not measure or place children.
The two `TextView`s draw their text.
The `ImageView` draws its image.
Android draws the children through `dispatchDraw()`.

![The three jobs: measure sizes, assign rectangles, and draw content.](docs/images/viewgroup-phases.svg)

Use `requestLayout()` when your own size or position rules change.
Use `invalidate()` when only your custom drawing changes.
Android does not have to run all three methods on every frame.

## 7. The complete example

### XML

The [layout file](app/src/main/res/layout/article_preview_example.xml) contains
the three children inside our parent.
That parent sits inside a `ScrollView`.

```xml
<?xml version="1.0" encoding="utf-8"?>
<ScrollView xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/scroll"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <dev.bayram.viewgrouptutorial.ArticlePreviewView
        android:id="@+id/article_preview"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:padding="@dimen/content_padding">

        <ImageView
            android:id="@+id/article_image"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:importantForAccessibility="no"
            android:scaleType="centerCrop"
            android:src="@drawable/example_landscape" />

        <TextView
            android:id="@+id/article_title"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="@string/article_title"
            android:textColor="?android:attr/textColorPrimary"
            android:textSize="22sp"
            android:textStyle="bold" />

        <TextView
            android:id="@+id/article_description"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="@string/article_description"
            android:textColor="?android:attr/textColorSecondary"
            android:textSize="16sp" />
    </dev.bayram.viewgrouptutorial.ArticlePreviewView>
</ScrollView>
```

Our parent supplies the final child widths.
Those calculations replace the XML `wrap_content` widths here.
`centerCrop` fills the image box and crops extra picture area.
It does not set the box size.

The photo is decorative in this example.
For a photo that adds useful information, give it a translated
`contentDescription` instead of hiding it from screen readers.

The project includes the [text and spacing resources](app/src/main/res/values/strings.xml)
and [image](app/src/main/res/drawable/example_landscape.xml).

### Kotlin

This is the complete [ArticlePreviewView.kt](app/src/main/java/dev/bayram/viewgrouptutorial/ArticlePreviewView.kt).
`onFinishInflate()` finds the children after Android creates them from XML.

```kotlin
package dev.bayram.viewgrouptutorial

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView

/**
 * Stacks the preview when the title needs more than two lines beside the image.
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
    private val textGap = resources.getDimensionPixelSize(R.dimen.article_text_gap)
    private var arrangement = Arrangement.ROW

    private enum class Arrangement { ROW, STACKED }

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
        val thumbnailSide = (contentWidth * 0.3).toInt()
        val rowTextWidth = contentWidth - thumbnailSide - columnGap
        require(rowTextWidth > 0) { "Not enough width for padding, image, and column gap" }

        val textHeightSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        title.measure(
            MeasureSpec.makeMeasureSpec(rowTextWidth, MeasureSpec.EXACTLY),
            textHeightSpec
        )
        arrangement = if (title.lineCount <= 2) Arrangement.ROW else Arrangement.STACKED

        val imageWidth: Int
        val imageHeight: Int
        val textWidth: Int
        when (arrangement) {
            Arrangement.ROW -> {
                imageWidth = thumbnailSide
                imageHeight = thumbnailSide
                textWidth = rowTextWidth
            }
            Arrangement.STACKED -> {
                imageWidth = contentWidth
                imageHeight = (contentWidth * 9.0 / 16).toInt()
                textWidth = contentWidth
                title.measure(
                    MeasureSpec.makeMeasureSpec(textWidth, MeasureSpec.EXACTLY),
                    textHeightSpec
                )
            }
        }
        image.measure(
            MeasureSpec.makeMeasureSpec(imageWidth, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(imageHeight, MeasureSpec.EXACTLY)
        )
        val textWidthSpec = MeasureSpec.makeMeasureSpec(textWidth, MeasureSpec.EXACTLY)
        description.measure(textWidthSpec, textHeightSpec)

        val textColumnHeight = title.measuredHeight + textGap + description.measuredHeight
        val contentHeight = when (arrangement) {
            Arrangement.ROW -> maxOf(imageHeight, textColumnHeight)
            Arrangement.STACKED -> imageHeight + columnGap + textColumnHeight
        }
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
                    paddingTop + contentHeight + paddingBottom
                ),
                heightMeasureSpec,
                childState shl MEASURED_HEIGHT_STATE_SHIFT
            )
        )
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        val rtl = layoutDirection == LAYOUT_DIRECTION_RTL
        val imageLeft = if (rtl) right - left - paddingRight - image.measuredWidth else paddingLeft
        val textLeft = when (arrangement) {
            Arrangement.ROW -> if (rtl) paddingLeft else imageLeft + image.measuredWidth + columnGap
            Arrangement.STACKED -> paddingLeft
        }
        val titleTop = when (arrangement) {
            Arrangement.ROW -> paddingTop
            Arrangement.STACKED -> paddingTop + image.measuredHeight + columnGap
        }

        image.layout(
            imageLeft,
            paddingTop,
            imageLeft + image.measuredWidth,
            paddingTop + image.measuredHeight
        )
        title.layout(
            textLeft,
            titleTop,
            textLeft + title.measuredWidth,
            titleTop + title.measuredHeight
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
```

`arrangement` stores the measurement decision for `onLayout()`.
Every measurement starts by trying the row again.
This lets the preview switch back after the title becomes shorter.

`combineMeasuredStates()` collects child size-warning flags.
`resolveSizeAndState()` keeps those flags and applies parent limits.
The height flags need `MEASURED_HEIGHT_STATE_SHIFT`.
These are Android details around the main layout rule.

**Scope of this sample:** exact width, unbounded height, three visible children,
and no child margins.
The `require()` checks report unsupported uses.
Keep the `ScrollView` setup shown above, without `fillViewport`.
This is a teaching component, not a general layout container.

## 8. Try the rule

Change `article_title` in [strings.xml](app/src/main/res/values/strings.xml).
Try `"Walk"` first, then a long headline.
Build and run again.

At the same width, the short title should keep the row.
A title needing more than two lines **in the trial row** should choose the stack.
It may use fewer lines after it gets the full width.
That does not change the decision for that measurement.

Also try a larger font, a narrow window, and right-to-left layout.
The layout should respond to actual text measurement, not just window width.

Run the tests and Android lint:

```sh
./gradlew :app:testDebugUnitTest :app:lintDebug
```

The [tests](app/src/test/java/dev/bayram/viewgrouptutorial/ArticlePreviewViewTest.kt)
check the exact two-line boundary, row and stacked geometry, RTL, width changes,
and switching in both directions after text changes.
They use native text rendering in Robolectric so wrapping is measured.
Also check readability and accessibility on a device.

## A short note about performance

Nested layouts can add measurement and placement work.
But Android does not always measure the whole tree again.
Our custom parent removes the need for a nested text column.
It also measures the title twice when choosing the stack.

This example teaches a content-based layout decision.
It does not prove a speedup.
Measure equivalent implementations before making that claim.

## References

- [Preview source](app/src/main/java/dev/bayram/viewgrouptutorial/ArticlePreviewView.kt)
- [Preview XML](app/src/main/res/layout/article_preview_example.xml)
- [Custom Android components](https://developer.android.com/develop/ui/views/layout/custom-views/custom-components)
- [MeasureSpec](https://developer.android.com/reference/android/view/View.MeasureSpec)
- [Macrobenchmark](https://developer.android.com/topic/performance/benchmarking/macrobenchmark-overview)
