# XML for content. Kotlin for size and position.

You need an image beside a short text.
Then the design adds a rule: the image size must follow the text width.
How do you build that?

This guide follows one example from a standard layout to a custom `ViewGroup`.
You need basic Kotlin and Android XML knowledge.
The idea comes from the layouts in [Yumi](https://github.com/Bayram-MKININI/Yumi).

## 1. Start with an image and text

The first design is simple:

```text
+---------+    +----------------------+
|  Image  |    | Some text            |
+---------+    +----------------------+
```

You choose a horizontal `LinearLayout`.
The image has a fixed size.
The text uses the space that remains.

```xml
<LinearLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="horizontal"
    android:gravity="center_vertical"
    android:padding="24dp">

    <ImageView
        android:layout_width="80dp"
        android:layout_height="80dp"
        android:importantForAccessibility="no"
        android:scaleType="centerInside"
        android:src="@drawable/example_landscape" />

    <TextView
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:layout_weight="1"
        android:layout_marginLeft="16dp"
        android:text="@string/summary" />
</LinearLayout>
```

This works.
If the design stays like this, keep the standard layout.

## 2. The image must now follow the text

The design adds three rules:

1. Keep the image square.
2. Make each image side **50% of the measured text width**.
3. Make the gap **30% of that same width**.

Keep both children centered vertically.
Long text can use several lines.
Short text should use less space.

![A 100px square image, a 60px gap, and a 200px text box. All percentages use the measured text width.](docs/images/image-text-ratios.svg)

Now the fixed `80dp` image size is wrong.
You cannot choose the size before you know the text width.

Could weights solve it?
Weights share the space offered by the parent.
But our rule depends on the text's **actual measured width**.
Short text may use less space than it is offered.

`LinearLayout` XML cannot describe this exact rule.

Before writing a custom layout, also consider `ConstraintLayout`.
Its ratios, guidelines, and barriers solve many problems.
But it has no general XML expression for any calculation using another child's
measured size.

Adding wrappers does not add that calculation either.
You need code somewhere.
Here, we will put the rule in the parent itself.

## 3. Give one parent the rule

A `ViewGroup` is a View that contains other Views.
It is the **parent**.
The Views inside it are its **children**.

Our parent will contain only two children:

```text
ImageTextView
    -> ImageView
    -> TextView
```

The children still draw normal Android text and images.
Our parent only controls their sizes and positions.

The plan is:

```text
Find the available width
    -> measure the text within a limit
        -> calculate the image size and gap
            -> place both children
```

There is one problem to solve first.
How much width should we offer the text?

## 4. Leave room for the image and gap

Do not give the text the full row width.
That would leave no room for the image and gap.

Let `T` be the measured text width:

```text
image width = 0.5T
gap         = 0.3T
text width  =    T
-----------------
row width   = 1.8T
```

So the text can use at most:

```text
available width / 1.8
```

**Available width means parent width minus left and right padding.**
It is not the full screen width.

For example, say the available width is 360 pixels:

| Text measures | Image side | Gap | Row uses |
| --- | --- | --- | --- |
| 200px | 100px | 60px | 360px |
| 100px | 50px | 30px | 180px |

The second row is important.
Short text can be narrower than its limit.
The image and gap then become smaller too.

We use the width of the **TextView box**.
We do not measure the width of each visible letter.

## 5. Measure the children in onMeasure

This calculation belongs in `onMeasure()`.
This method measures children and reports the parent's own size.
It does not place or draw anything.

Android gives it a `MeasureSpec` for width and another for height.
Each spec has a size and a rule:

| Mode | Meaning |
| --- | --- |
| `EXACTLY` | Use this size |
| `AT_MOST` | Use up to this size |
| `UNSPECIFIED` | No limit for this width or height |

We give the text an `AT_MOST` width.
We give it an `UNSPECIFIED` height so it can grow:

```kotlin
val textWidthLimit = (availableWidth / 1.8).toInt()
text.measure(
    MeasureSpec.makeMeasureSpec(textWidthLimit, MeasureSpec.AT_MOST),
    MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
)
```

Now `text.measuredWidth` is known.
We can calculate the image size and gap:

```kotlin
val imageSide = (text.measuredWidth * 0.5).toInt()
gap = (text.measuredWidth * 0.3).toInt()
image.measure(
    MeasureSpec.makeMeasureSpec(imageSide, MeasureSpec.EXACTLY),
    MeasureSpec.makeMeasureSpec(imageSide, MeasureSpec.EXACTLY)
)
```

These are parts of the method.
The complete class is below.

The parent also needs a size:

```text
width  = left padding + image + gap + text + right padding
height = top padding + taller child + bottom padding
```

Use `resolveSizeAndState()` to respect the parent's limits.
Then pass the results to `setMeasuredDimension()`.
Do not pass `MeasureSpec` values to `setMeasuredDimension()`.

All these calculations use pixels.
Android converts XML values such as `24dp` to pixels.
`toInt()` rounds our positive percentage results down to whole pixels.

## 6. Place the children in onLayout

Now we know both sizes.
We still need positions.
That is the job of `onLayout()`.

Place the image at the left padding.
Place the text after the image and gap:

```kotlin
val imageLeft = paddingLeft
val textLeft = imageLeft + image.measuredWidth + gap
```

Use the taller child as the row height.
Center each child inside that height:

```kotlin
val rowHeight = maxOf(image.measuredHeight, text.measuredHeight)
val imageTop = paddingTop + (rowHeight - image.measuredHeight) / 2
val textTop = paddingTop + (rowHeight - text.measuredHeight) / 2
```

Then call each child's `layout()`:

```kotlin
image.layout(
    imageLeft,
    imageTop,
    imageLeft + image.measuredWidth,
    imageTop + image.measuredHeight
)
```

The arguments are **left, top, right, bottom**.
They are not x, y, width, height.
The positions are inside the parent, not the screen.

For example, `layout(24, 40, 124, 140)` places a 100 by 100 pixel box.
Its top-left corner is at x=24 and y=40.

### Do we need onDraw?

No. `onDraw()` is for a View's own custom drawing.
For example, a chart could draw lines there.
It does not measure or place children.

Our `TextView` draws its text.
Our `ImageView` draws its image.
Android draws children through `dispatchDraw()`.
So our parent does not need an `onDraw()` override.

![The three jobs: measure sizes, assign rectangles, and draw content.](docs/images/viewgroup-phases.svg)

Use `requestLayout()` when your own size or position rules change.
Use `invalidate()` when only your custom drawing changes.
Android does not need to run all three methods on every frame.

## 7. Put it together

This sample expects a width limit and no height limit.
We place it inside a `ScrollView` to allow vertical growth.
Both children must stay visible.
Child margins are not supported.

The image stays physically left, even in right-to-left languages.
If your design follows reading order, reverse the positions for those languages.

### The XML

Save this as `app/src/main/res/layout/image_text_example.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<ScrollView xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/scroll"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <dev.bayram.viewgrouptutorial.ImageTextView
        android:id="@+id/image_text"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:padding="@dimen/content_padding">

        <ImageView
            android:id="@+id/example_image"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:importantForAccessibility="no"
            android:scaleType="centerInside"
            android:src="@drawable/example_landscape" />

        <TextView
            android:id="@+id/example_text"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="@string/summary"
            android:textColor="?android:attr/textColorPrimary"
            android:textSize="18sp" />
    </dev.bayram.viewgrouptutorial.ImageTextView>
</ScrollView>
```

The repository includes the referenced text, padding, and image:

- [Text and padding resources](app/src/main/res/values/strings.xml)
- [Example image](app/src/main/res/drawable/example_landscape.xml)

The image is decorative, so it is hidden from screen readers.
For a meaningful image, add a translated `contentDescription` instead.
`centerInside` fits the picture inside the image box.
It does not set the box's size.

### The complete Kotlin class

Save this as
`app/src/main/java/dev/bayram/viewgrouptutorial/ImageTextView.kt`.
It is the same class used by the app and tests.

Android creates the parent before it finishes creating the XML children.
We find the children in `onFinishInflate()`, after they exist.

```kotlin
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
```

The `require()` checks explain unsupported uses.
`resolveSizeAndState()` applies size limits and keeps size-warning flags.
`combineMeasuredStates()` collects those flags from the children.
The height flags need the `MEASURED_HEIGHT_STATE_SHIFT` shift.

These details help the class follow Android's measurement rules.
The main idea is still small: **measure text, calculate image, place both**.

## 8. Run the row you just built

Use Android Studio, JDK 17, and Android SDK 36:

```sh
git clone https://github.com/Bayram-MKININI/android-custom-viewgroup-tutorial.git
cd android-custom-viewgroup-tutorial
```

Set `ANDROID_HOME` to your SDK.
Or open the project in Android Studio to create `local.properties`.
The first build downloads the tools and libraries it needs.

The repository also has an earlier title-and-summary demo.
To show this article's image row, change the `setContentView()` line in
[MainActivity](app/src/main/java/dev/bayram/viewgrouptutorial/MainActivity.kt):

```kotlin
setContentView(R.layout.image_text_example)
```

Keep the activity's system-bar handling.
It keeps the content away from those bars.

Build the app:

```sh
./gradlew :app:assembleDebug
```

With a connected device or emulator running API 24 or newer:

```sh
./gradlew :app:installDebug
```

Open **Custom ViewGroup Tutorial**.
Try a short text, then a long text.
The image and gap should follow the measured text width in both cases.

The included tests check the percentages, positions, and size limits.
They run without a device:

```sh
./gradlew :app:testDebugUnitTest :app:lintDebug
```

Also try large fonts, a narrow window, and a screen reader on a device.

## A short note about performance

Nested layouts can add measurement and placement work.
A size change can affect parents and other children.
But Android does not always measure the whole tree again.

A custom parent may remove extra layers.
That does not prove it is faster.
This project has no performance comparison.
Measure the same screen both ways before claiming a speedup.

Use a standard layout when it fits.
Use a custom `ViewGroup` when it makes a special rule clearer.

## References

- [Image/text source](app/src/main/java/dev/bayram/viewgrouptutorial/ImageTextView.kt)
- [Image/text tests](app/src/test/java/dev/bayram/viewgrouptutorial/ImageTextViewTest.kt)
- [Custom Android components](https://developer.android.com/develop/ui/views/layout/custom-views/custom-components)
- [MeasureSpec](https://developer.android.com/reference/android/view/View.MeasureSpec)
- [Macrobenchmark](https://developer.android.com/topic/performance/benchmarking/macrobenchmark-overview)
