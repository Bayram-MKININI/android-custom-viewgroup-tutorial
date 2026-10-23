# XML for content. Kotlin for geometry.

A simple guide to custom Android `ViewGroup`s, inspired by
[Yumi](https://github.com/Bayram-MKININI/Yumi).

## 1. Start with the problem

We need a title with a summary below it.

![Two text boxes inside a parent, showing padding and the gap between them.](docs/images/headline-layout.svg)

An existing layout already solves this. A vertical `LinearLayout` is enough:

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

For this design, use it. There is no reason to write a custom layout yet.

## 2. Add requirements that the simple layout cannot express

Now the design changes:

1. Put an image to the left of the text.
2. Make it square, with each side equal to **50% of the measured text width**.
3. Leave a gap equal to **30% of that same text width**.
4. Center the image and text vertically.
5. Let long text wrap and short text use less space.

![A 100px square image, a 60px gap, and a 200px text box, centered vertically.](docs/images/image-text-ratios.svg)

A horizontal `LinearLayout` can place an image beside text. But it cannot
express these exact sibling-dependent size and gap rules using its XML
attributes alone. Weights distribute available space; they do not calculate
the image size from the text's final measured width.

ConstraintLayout supports many relationships through constraints, ratios,
guidelines, barriers, and chains. It is worth considering before writing
anything custom. But it does not provide a general XML formula for arbitrary
arithmetic on another child's measured dimensions.

The problem is not "standard layouts are bad." It is that a specialized
geometry rule may be clearer as code.

## 3. Why not keep embedding layouts?

As requirements grow, one common response is to add containers:

```text
Outer container
    -> vertical container
        -> horizontal container
            -> image wrapper
                -> image
            -> text wrapper
                -> text
```

Nesting can combine standard layout behaviors, but **nesting alone does not
solve our percentage formulas**. Extra code may still be needed.

Each extra container also participates in measurement and placement. A child's
size change can request layout through its ancestors. If parent sizes or
constraints change, more descendants may need to be measured or laid out.
Some arrangements, such as weighted layouts, can measure children more than
once.

This can make deep or repeatedly measured hierarchies expensive, especially
during frequent updates.

However, a change does **not always remeasure the entire hierarchy**. Android
can reuse measurements, and drawing-only changes may not need layout at all.
Depth is a reason to inspect the work, not proof of a performance problem.

For our specialized rule, consider a simpler hierarchy:

```text
ImageTextView
    -> ImageView
    -> TextView
```

One parent calculates the sizes and positions directly. The children remain
ordinary Android Views.

## 4. The three methods: size, position, drawing

![Measurement determines sizes, layout assigns rectangles, and drawing paints the content.](docs/images/viewgroup-phases.svg)

### onMeasure: how big?

`onMeasure()` measures the children and reports the parent's measured size.

```kotlin
child.measure(childWidthSpec, childHeightSpec)
val width = child.measuredWidth
val height = child.measuredHeight
```

The specs above must be calculated first. Each `MeasureSpec` contains a size
and a rule:

| Mode | Meaning |
| --- | --- |
| `EXACTLY` | Use this size |
| `AT_MOST` | Choose a size no larger than this limit |
| `UNSPECIFIED` | No limit on this axis |

For example, `AT_MOST 200px` lets short text be narrower than 200px, while long
text wraps within that width.

After measuring children, call `setMeasuredDimension()` to report the parent's
size. Use `resolveSizeAndState()` to respect the incoming specs.

**Measurement determines size, not position.** Call a child's `measure()`, not
its `onMeasure()` directly.

### onLayout: where?

`onLayout()` gives each child a rectangle inside the parent:

```kotlin
child.layout(24, 40, 124, 140)
```

The arguments are **left, top, right, bottom**, not x, y, width, height. This
places a 100 by 100 pixel box at x=24, y=40 in the parent's coordinates.

Normally, use the dimensions calculated during measurement:

```kotlin
child.layout(
    childLeft,
    childTop,
    childLeft + child.measuredWidth,
    childTop + child.measuredHeight
)
```

**Layout determines position, not pixels.** Call a child's `layout()`, not its
`onLayout()` directly.

### onDraw: what should this View paint?

`onDraw(canvas)` paints a View's own custom content, such as chart lines or a
decoration. It does not arrange children.

We do not need it here:

- `TextView` draws its text.
- `ImageView` draws its image.
- The parent's drawing pipeline draws children through `dispatchDraw()`.

The simplified drawing order is:

```text
background -> parent's own onDraw, if enabled -> children -> foreground
```

If a plain custom `ViewGroup` needs its own `onDraw()`, enable that drawing
with `setWillNotDraw(false)`. Do not do this just to display children.

When your own geometry changes, use `requestLayout()`. When only custom paint
changes, use `invalidate()`. These schedule work; the three callbacks are not
guaranteed to run once together on every frame.

## 5. Keep the children in XML

The second sample uses this structure inside a `ScrollView`. This is an
excerpt; the [complete XML](app/src/main/res/layout/image_text_example.xml)
includes the parent declaration and all attributes.

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

XML owns the children and their appearance. Kotlin owns their geometry.

The image is decorative. Give meaningful images a localized
`contentDescription` instead of hiding them from accessibility.

`centerInside` controls how the drawable fits **inside** the image box. It
does not determine the box's size.

Bind the children after external XML inflation finishes:

```kotlin
override fun onFinishInflate() {
    super.onFinishInflate()
    image = requireNotNull(findViewById<ImageView>(R.id.example_image))
    text = requireNotNull(findViewById<TextView>(R.id.example_text))
}
```

This excerpt assumes `image` and `text` are fields. The
[complete class](app/src/main/java/dev/bayram/viewgrouptutorial/ImageTextView.kt)
also checks that these are its two direct children.

## 6. Calculate the sizes in onMeasure

Let `T` be the measured text width:

```text
image + gap + text = total row width
0.5T  + 0.3T + T   = 1.8T

Therefore: text width must be at most available width / 1.8.
```

Measure text first, then use its actual width for the image and gap:

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

With 360px available, the text budget is 200px:

| Actual measured text width | Image side | Gap | Total row width |
| --- | --- | --- | --- |
| 200px | 100px | 60px | 360px |
| 100px | 50px | 30px | 180px |

Short text does not have to fill its budget. The image follows the **measured
text box**, not the maximum width or the width of individual letters.

All these calculations use pixels. Resources such as `24dp` padding are
converted by Android; percentage results are rounded down to whole pixels.

The parent's desired dimensions are:

```kotlin
val desiredWidth =
    paddingLeft + imageSide + gap + text.measuredWidth + paddingRight
val desiredHeight =
    paddingTop + maxOf(imageSide, text.measuredHeight) + paddingBottom
```

The full implementation resolves these against the incoming specs and minimum
dimensions, then reports them with `setMeasuredDimension()`. It also propagates
child measurement states.

**Sample contract:** bounded width, unbounded height, and both children visible.
The surrounding `ScrollView` allows the row to grow vertically. The class
rejects unsupported measurement modes rather than silently breaking the rules.
Margins are not supported.

For a fixed-height design, decide what should happen if the content cannot fit:
scroll, truncate text, shrink the image, or change the arrangement.

## 7. Place the boxes in onLayout

The taller child determines the row height. Center both children within it:

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

Here, "left" means physical left, including in RTL. If the design means
"before the text in reading order," mirror the horizontal placement for RTL.

This reserves real space for the image and gap. A visual `translationX` alone
would move the drawing without changing the row's measurement.

## 8. Other relationships follow the same idea

These are possible formulas to adapt, not extra features of the sample:

| Requirement | Rule |
| --- | --- |
| Square image follows text height | `imageSide = text.measuredHeight` |
| Image keeps its aspect ratio | `imageHeight = (imageWidth / aspectRatio).toInt()` |
| Image uses remaining width | `imageWidth = (availableWidth - text.measuredWidth - gap).coerceAtLeast(0)` |
| Image starts 30% across text | `imageLeft = text.left + (text.measuredWidth * 0.3).toInt()` |
| Image overlaps half of text's right edge | `imageLeft = text.right - image.measuredWidth / 2` |

Define the anchor carefully. "30% across the text" is different from "outside
its left edge with a 30% gap":

![Two different positioning rules: a gap outside the text versus an offset across the text box.](docs/images/position-relative-to-text.svg)

Always establish the dependency order:

```text
available width -> text budget -> measured text -> image size -> positions
```

If text width depends on image width, while image width depends on wrapped
text height, you have a circular dependency. Solve the relationship or choose
an independent limit first; repeatedly measuring is not a guaranteed solution.

## 9. Run the examples

Use Android Studio, JDK 17, and Android SDK 36:

```sh
git clone https://github.com/Bayram-MKININI/android-custom-viewgroup-tutorial.git
cd android-custom-viewgroup-tutorial
./gradlew :app:assembleDebug
```

Set `ANDROID_HOME` to your SDK, or let Android Studio create `local.properties`.
The first run needs network access for dependencies.

The app initially shows the simpler title-and-summary example. To show the
image example, replace the `setContentView()` line in
[MainActivity](app/src/main/java/dev/bayram/viewgrouptutorial/MainActivity.kt):

```kotlin
setContentView(R.layout.image_text_example)
```

Keep its existing inset handling. Both screens have the same `scroll` ID.

With a connected device or emulator running API 24 or newer:

```sh
./gradlew :app:installDebug
```

Launch **Custom ViewGroup Tutorial**. Run the included Robolectric tests and
Android lint without a device:

```sh
./gradlew :app:testDebugUnitTest :app:lintDebug
```

The tests cover sizing, placement, percentage relationships, and text updates.
Also check long translations, large fonts, narrow windows, and accessibility
on a device.

## Is the custom layout faster?

It avoids extra layout containers and writes the geometry directly. That can
reduce work, but **a flatter hierarchy is not a performance measurement**.
ConstraintLayout can also flatten nested layouts.

This repository has no comparative benchmark. Before claiming a speedup,
compare equivalent implementations on the same devices and content using
Macrobenchmark and Perfetto. Include initial display and repeated updates.

Use standard layouts when they express the design clearly. Use a custom
`ViewGroup` when the explicit sizing and placement rules are worth owning.

## Source and references

- [Simple text layout](app/src/main/java/dev/bayram/viewgrouptutorial/HeadlineView.kt)
- [Image/text layout](app/src/main/java/dev/bayram/viewgrouptutorial/ImageTextView.kt)
- [Tests](app/src/test/java/dev/bayram/viewgrouptutorial)
- [Custom Android components](https://developer.android.com/develop/ui/views/layout/custom-views/custom-components)
- [MeasureSpec](https://developer.android.com/reference/android/view/View.MeasureSpec)
- [Macrobenchmark](https://developer.android.com/topic/performance/benchmarking/macrobenchmark-overview)
