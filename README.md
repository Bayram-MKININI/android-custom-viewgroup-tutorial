# XML for content. Kotlin for size and position.

A simple guide to custom Android `ViewGroup`s, inspired by
[Yumi](https://github.com/Bayram-MKININI/Yumi).

You need basic Kotlin and Android XML knowledge.

A `ViewGroup` is a View that contains other Views.
We call it the **parent**. The Views inside it are its **children**.

## 1. You start with a simple screen

You open a new design.
There is a title.
There is a summary below it.

You know how to build this.

![Two text boxes inside a parent, showing padding and the gap between them.](docs/images/headline-layout.svg)

A vertical `LinearLayout` is enough:

```xml
<LinearLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="vertical"
    android:padding="24dp">

    <TextView
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="@string/headline" />

    <TextView
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="12dp"
        android:text="@string/summary" />
</LinearLayout>
```

The screen looks right.
The code is short.
You move on.

## 2. Then the design changes

Now you need an image beside the text.

"Easy," you think.
"I can put them in a horizontal `LinearLayout`."

That solves the position.
But then you read the size rules:

> Make the image square.
> Each side must be **50% of the measured text width**.
> The gap must be **30% of that same width**.
> Center the image and text vertically.

The text can change too.
Long text must use several lines.
Short text must use less space.

![A 100px square image, a 60px gap, and a 200px text box, centered vertically.](docs/images/image-text-ratios.svg)

You try a fixed image size.
It looks right with one text.
But the image no longer follows the text width.

You consider layout weights.
They can share the available space.
But that is not the rule you need.
The image must follow the text's **actual measured width**.
Short text may not use all the available space.

Your horizontal layout can place the children.
Its XML cannot describe this exact size formula.

What about `ConstraintLayout`?
It can solve many problems with ratios, guidelines, and barriers.
Check those tools first.
But XML does not let you write any formula using another child's measured size.

You can describe the design in one sentence.
But you cannot give that sentence directly to this layout.

## 3. You add another layout. Does that help?

You may try a wrapper around the image.
Then another around the text.
As more rules arrive, the structure can grow:

```text
Outer container
    -> vertical container
        -> horizontal container
            -> image wrapper
                -> image
            -> text wrapper
                -> text
```

Layouts inside layouts are called **nested layouts**.
They are useful when you need to combine their behavior.
But look at our problem again:

> Where do we calculate 50% of the measured text width?

Another wrapper does not answer that question.
**More layers do not solve the formula by themselves.**
You may still need code.

Each layout must measure and place its children.
When a child's size changes, its parents may need to update too.
Other children may also need new sizes or positions.
Some layouts measure a child more than once.

More layers can mean more work.
This can hurt performance when the screen changes often.

But Android does **not always measure every View again**.
It can reuse earlier measurements.
A color change may only need drawing.
A deep layout is not always slow.

So stop adding wrappers for a moment.
What if one parent knew the rule?

It could measure the text first.
Then it could calculate the image size and the gap.
Finally, it could place both children.

The structure would be:

```text
ImageTextView
    -> ImageView
    -> TextView
```

That parent is our custom `ViewGroup`.
We do not need to build a text renderer.
We do not need to build an image renderer.
The children are still normal Android Views.

We only need to answer two questions:

1. How big should each child be?
2. Where should each child go?

## 4. Give each method one job

Before writing the formula, you need to know where it belongs.
Android separates size, position, and drawing.

![Measurement determines sizes, layout assigns rectangles, and drawing paints the content.](docs/images/viewgroup-phases.svg)

### onMeasure: how big?

In `onMeasure()`, we measure the children.
Then we report the parent's own size.

```kotlin
child.measure(childWidthSpec, childHeightSpec)
val width = child.measuredWidth
val height = child.measuredHeight
```

We must calculate `childWidthSpec` and `childHeightSpec` first.
A **MeasureSpec** is a size with a rule:

| Mode | Meaning |
| --- | --- |
| `EXACTLY` | Use this size |
| `AT_MOST` | Choose a size no larger than this limit |
| `UNSPECIFIED` | No limit for this width or height |

For example, `AT_MOST 200px` means "use up to 200 pixels."
Short text can be narrower.
Long text can use several lines.

Call `setMeasuredDimension()` to report the parent's size.
Use `resolveSizeAndState()` to stay within the parent's limits.

**Measuring gives us a size, not a position.**
Call a child's `measure()`.
Do not call its `onMeasure()` directly.

### onLayout: where?

`onLayout()` gives each child a rectangle inside the parent:

```kotlin
child.layout(24, 40, 124, 140)
```

The arguments are **left, top, right, bottom**.
They are not x, y, width, height.
This box is 100 pixels wide and 100 pixels tall.
It starts 24 pixels from the parent's left edge.
It starts 40 pixels from the parent's top edge.

Use the sizes from measurement:

```kotlin
child.layout(
    childLeft,
    childTop,
    childLeft + child.measuredWidth,
    childTop + child.measuredHeight
)
```

**Layout places the child. It does not draw it.**
Call a child's `layout()`.
Do not call its `onLayout()` directly.

### onDraw: what should this View paint?

`onDraw(canvas)` draws custom content for this View.
For example, it can draw a chart or a line.
It does not place children.

We do not need it here:

- `TextView` draws its text.
- `ImageView` draws its image.
- Android draws the children through `dispatchDraw()`.

The simplified drawing order is:

```text
background -> parent's own onDraw, if enabled -> children -> foreground
```

A plain `ViewGroup` normally skips its own drawing.
Use `setWillNotDraw(false)` if you add custom `onDraw()` code.
We do not need it in this example.

Use `requestLayout()` when your size or position rules change.
Use `invalidate()` when only your custom drawing changes.
These methods ask Android to do work later.
Android does not have to call all three methods on every frame.

## 5. Keep the XML you already know

You do not have to create every child in Kotlin.
Keep the image and text in XML.
Only replace their parent with your own class.

The image example sits inside a `ScrollView`.
This lets the user scroll if the text is tall.
Here is part of the XML.
See the [complete XML](app/src/main/res/layout/image_text_example.xml) for all details.

```xml
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
        android:textSize="18sp" />
</dev.bayram.viewgrouptutorial.ImageTextView>
```

XML describes the children and their look.
Kotlin controls their sizes and positions.

This image is only for decoration.
So we hide it from screen readers.
If an image gives useful information, add a `contentDescription`.
Translate that description like other text.

`centerInside` fits the picture **inside** the image box.
It does not set the box's size.

Android first creates the parent.
Then it creates the children from XML.
This process is called **inflation**.

Find the children after this process ends:

```kotlin
override fun onFinishInflate() {
    super.onFinishInflate()
    image = requireNotNull(findViewById<ImageView>(R.id.example_image))
    text = requireNotNull(findViewById<TextView>(R.id.example_text))
}
```

Here, `image` and `text` are fields in our class.
The
[complete class](app/src/main/java/dev/bayram/viewgrouptutorial/ImageTextView.kt)
also checks the children.
It requires one image and one text directly inside the parent.

## 6. You need the text width. But how much space can it use?

You are ready to measure the text.
Should you give it the full row width?

Not yet.
You still need room for the image and the gap.
If the text takes the whole row, the other boxes may not fit.

Write the rule as a formula first.
Use `T` for the measured text width:

```text
image + gap + text = total row width
0.5T  + 0.3T + T   = 1.8T

Therefore: text width must be at most available width / 1.8.
```

Now you have a width limit for the text.
Measure it within that limit.
Then use its actual width to calculate the image and gap:

```kotlin
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
```

If we have 360 pixels, the text can use up to 200 pixels:

| Actual measured text width | Image side | Gap | Total row width |
| --- | --- | --- | --- |
| 200px | 100px | 60px | 360px |
| 100px | 50px | 30px | 180px |

Short text can use less space.
The image size depends on the **measured text box**.
It does not depend on the width of each letter.

These calculations use pixels.
Android converts values such as `24dp` to pixels.
We round percentage results down to whole pixels.

The parent needs this size:

```kotlin
val desiredWidth =
    paddingLeft + imageSide + gap + text.measuredWidth + paddingRight
val desiredHeight =
    paddingTop + maxOf(imageSide, text.measuredHeight) + paddingBottom
```

The full code checks the parent's size limits.
It also respects its minimum size.
Then it calls `setMeasuredDimension()`.
It passes on any child signal that the available size is too small.

**This sample has limits:**

- The parent must receive a width limit.
- The height must have no limit. The `ScrollView` allows this.
- Both children must be visible.
- Child margins are not supported.

The class reports an error if the measurement rules are not supported.

What if your screen has a fixed height?
Choose what happens when the content is too tall.
You could allow scrolling, cut the text, shrink the image, or change the layout.

## 7. The sizes are known. Now place the boxes.

The text has a size.
The image has a size.
The gap has a size too.

There is no formula left to guess.
Place the image at the left padding.
Place the text after the image and the gap.

Use the taller child as the row height.
Center both children in that height:

```kotlin
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
```

Here, the image stays on the left.
This is also true for right-to-left languages such as Arabic.
If the image should follow reading order, reverse the positions for those languages.

This code leaves real space for the image and gap.
`translationX` only moves how a View appears.
It does not change the space measured for the row.

## 8. Try other size and position rules

You can adapt these formulas.
They are not extra features in the sample:

| Requirement | Rule |
| --- | --- |
| Square image follows text height | `imageSide = text.measuredHeight` |
| Image keeps its aspect ratio | `imageHeight = (imageWidth / aspectRatio).toInt()` |
| Image uses remaining width | `imageWidth = (availableWidth - text.measuredWidth - gap).coerceAtLeast(0)` |
| Image starts 30% across text | `imageLeft = text.left + (text.measuredWidth * 0.3).toInt()` |
| Image overlaps half of text's right edge | `imageLeft = text.right - image.measuredWidth / 2` |

Say where each position starts.
"30% across the text" is not the same as "left of the text with a 30% gap":

![Two different positioning rules: a gap outside the text versus an offset across the text box.](docs/images/position-relative-to-text.svg)

Calculate the values in the right order:

```text
available width -> text width limit -> measured text -> image size -> positions
```

Be careful with rules that depend on each other.
For example, text width may depend on image width.
Image width may then depend on text height.
But text height also depends on text width.
This creates a circle.

Choose one size limit first, or solve the formula.
Measuring again and again may not solve the problem.

## 9. Run the examples

Use Android Studio, JDK 17, and Android SDK 36:

```sh
git clone https://github.com/Bayram-MKININI/android-custom-viewgroup-tutorial.git
cd android-custom-viewgroup-tutorial
./gradlew :app:assembleDebug
```

Set `ANDROID_HOME` to your SDK.
Or let Android Studio create `local.properties`.
The first run downloads the tools and libraries it needs.

The app first shows the title and summary.
To see the image example, change the `setContentView()` line in
[MainActivity](app/src/main/java/dev/bayram/viewgrouptutorial/MainActivity.kt):

```kotlin
setContentView(R.layout.image_text_example)
```

Keep the code that handles system bars.
It keeps the content away from those bars.
Both screens have the same `scroll` ID.

With a connected device or emulator running API 24 or newer:

```sh
./gradlew :app:installDebug
```

Open **Custom ViewGroup Tutorial** on the device.

You can also run the tests and Android lint without a device:

```sh
./gradlew :app:testDebugUnitTest :app:lintDebug
```

The tests check sizes, positions, percentages, and text updates.
Also try the app with long text, large fonts, and a narrow window.
Check it with a screen reader too.

## You solved the layout. Did you also make it faster?

It can remove extra layout layers.
This may reduce work.
But **fewer layers do not prove that a layout is faster**.
`ConstraintLayout` can also remove the need for nested layouts.

This project does not compare performance.
To compare speed, build the same screen both ways.
Use the same content and devices.
Measure the first display and later updates.
Macrobenchmark and Perfetto can help.

Use a standard layout when it fits the design.
Use a custom `ViewGroup` when it makes the rules clearer.
Remember that you must maintain those rules yourself.

## Source and references

- [Simple text layout](app/src/main/java/dev/bayram/viewgrouptutorial/HeadlineView.kt)
- [Image/text layout](app/src/main/java/dev/bayram/viewgrouptutorial/ImageTextView.kt)
- [Tests](app/src/test/java/dev/bayram/viewgrouptutorial)
- [Custom Android components](https://developer.android.com/develop/ui/views/layout/custom-views/custom-components)
- [MeasureSpec](https://developer.android.com/reference/android/view/View.MeasureSpec)
- [Macrobenchmark](https://developer.android.com/topic/performance/benchmarking/macrobenchmark-overview)
