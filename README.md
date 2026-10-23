# XML for content. Kotlin for geometry.

A practical Android tutorial: build a custom `ViewGroup` whose children are
declared in XML, but measured and positioned in Kotlin.

Inspired by the screen layouts in [Yumi](https://github.com/Bayram-MKININI/Yumi).
This is a small, original teaching example, not a copy of Yumi's application
code and not a UI library.

## The idea

In a typical XML screen, a `LinearLayout` or `ConstraintLayout` decides where
children go. Here, a small, purpose-built parent makes that decision instead.

| File | Responsibility |
| --- | --- |
| [activity_main.xml](app/src/main/res/layout/activity_main.xml) | Children, text, typography, and padding |
| [HeadlineView.kt](app/src/main/java/dev/bayram/viewgrouptutorial/HeadlineView.kt) | Measurement and placement |
| [MainActivity.kt](app/src/main/java/dev/bayram/viewgrouptutorial/MainActivity.kt) | Hosting the screen and applying safe-area insets |

The result is still native Android Views. Text selection, text rendering,
accessibility nodes, and font scaling come from the real `TextView` children.
You are replacing their layout container, not drawing text yourself.

## Run the sample

Use Android Studio, JDK 17, and Android SDK 36. Point `ANDROID_HOME` to your SDK
or let Android Studio create `local.properties`.

```sh
./gradlew :app:assembleDebug
./gradlew :app:installDebug
```

The second command needs a connected device or emulator running Android 7.0
(API 24) or newer. Launch **Custom ViewGroup Tutorial** from its launcher.

```sh
./gradlew :app:testDebugUnitTest :app:lintDebug
```

The tests use Robolectric, so no device is required. Gradle, Android build
tools, dependencies, and Robolectric's Android runtime need network access on
the first run.

## 1. Declare the children in XML

The custom class becomes the parent tag:

```xml
<dev.bayram.viewgrouptutorial.HeadlineView
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:padding="24dp">

    <TextView
        android:id="@+id/title"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="@string/headline"
        android:textSize="28sp" />

    <TextView
        android:id="@+id/summary"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="@string/summary"
        android:textSize="18sp" />
</dev.bayram.viewgrouptutorial.HeadlineView>
```

There are no constraints to declare. That does **not** mean there are no layout
rules: those rules live in the Kotlin parent.

The runnable screen wraps this block in a `ScrollView`. Long text or larger
fonts can grow vertically instead of being forced into a fixed screen height.

## 2. Bind after XML inflation

Android creates the parent before it finishes inflating its children. Bind
the child references in `onFinishInflate()`, not in the constructor:

```kotlin
override fun onFinishInflate() {
    super.onFinishInflate()
    // Find and validate the title and summary here.
}
```

The sample requires exactly two direct `TextView` children with the specified
IDs. They use `wrap_content` height. This is a deliberately narrow component:
it does not support margins, arbitrary child structures, or programmatic
construction followed by adding children. Container padding and the
`headline_gap` resource own its spacing.

Another valid pattern is a self-contained widget that inflates its own
`<merge>` layout in its initializer. Choose one owner for inflation; do not
inflate the same children both internally and through the outer XML.

## 3. Measure under the parent's rules

`onMeasure()` answers two questions:

1. How much space can each child use?
2. How much space should this parent report?

An incoming `MeasureSpec` contains **both a size and a mode**:

| Mode | Contract |
| --- | --- |
| `EXACTLY` | Use the requested size |
| `AT_MOST` | Choose a size no larger than the limit |
| `UNSPECIFIED` | No limit is imposed on this axis |

Reading only `MeasureSpec.getSize()` loses that contract. In particular, a
scroll container can measure its content with an unspecified height.

The example uses `getChildMeasureSpec()` to combine the incoming limit, the
container's padding, and each child's XML size:

```kotlin
val childWidthSpec = getChildMeasureSpec(
    widthMeasureSpec,
    paddingLeft + paddingRight,
    child.layoutParams.width
)
```

Each visible child is measured with the available width. This lets text wrap.
For height, the parent also subtracts the vertical space already used by
previous children and the gap.

The desired width is the widest child plus horizontal padding. The desired
height is the sum of visible child heights, the gap if both are present, and
vertical padding.

Finally, `resolveSizeAndState()` reconciles that desired size with the parent's
contract. Child measurement states are combined and passed through, shifting
the state bits for height.

**Do not pass `makeMeasureSpec()` results to `setMeasuredDimension()`.** That
method accepts measured dimensions and measured-state bits, not measure specs.
`resolveSizeAndState()` returns the appropriate representation.

This sample measures each visible child once per `onMeasure()` call. That is
not a claim that Android will invoke `onMeasure()` only once per frame, or
that each child performs only one internal measurement.

## 4. Place the measured children

`onLayout()` assigns rectangles in the parent's local coordinate system:

```kotlin
child.layout(
    childLeft,
    childTop,
    childLeft + child.measuredWidth,
    childTop + child.measuredHeight
)
```

The title starts at the top padding. The summary starts below the title plus
the gap. The start edge is the left padding in left-to-right layouts and the
right padding in right-to-left layouts.

Skip `GONE` children during both measurement and layout, including their gap.
An `INVISIBLE` child still takes space, just as it does in standard containers.

Placement uses the actual assigned width, `right - left`, rather than the
window width. A reusable child does not own the whole screen.

## 5. Update content, not geometry

The activity or controller supplies data through a small API:

```kotlin
findViewById<HeadlineView>(R.id.headline).setContent(
    getString(R.string.headline),
    getString(R.string.summary)
)
```

`TextView.setText()` requests layout when necessary. A custom setter that
changes geometry itself, such as configurable spacing, must call
`requestLayout()`. A drawing-only change usually needs `invalidate()` instead.

The layout does not fetch data, navigate, or hold an activity reference.

## What about performance?

A purpose-built `ViewGroup` can avoid constraint solving and replace some
nested layout containers with direct arithmetic. For a stable arrangement,
that can also make the layout easier to read.

But **fewer constraints are not proof of faster frames**. Text measurement,
repeated measurements, drawing, allocations, and update frequency all matter.
ConstraintLayout can also flatten a hierarchy that would otherwise be deeply
nested.

This repository contains no comparative benchmark and makes no numerical
speedup claim. To compare fairly:

1. Implement the same appearance and behavior using ConstraintLayout.
2. Use identical content, fonts, devices, and release-like build settings.
3. Measure initial display and repeated updates or scrolling separately.
4. Use Macrobenchmark frame-timing metrics and Perfetto traces; inspect layout
   work rather than comparing XML line counts.
5. Repeat runs and report distributions, device details, and build settings.

For just two vertically stacked text fields, `LinearLayout` is normally the
simplest production choice. This tiny example exists to teach the mechanics
before applying them to a layout that actually needs custom placement.

## Where this approach fits

Use it when the arrangement is stable, the geometry is clear, and having an
explicit layout algorithm is worth maintaining.

Prefer standard containers when they already express the design clearly.
Prefer ConstraintLayout when the layout has many changing relationships and
you do not want to maintain a custom sizing algorithm.

Do not copy screen-specific assumptions into reusable components:

- Apply real system-bar and cutout insets at the screen boundary, not a fixed
  "status bar height." The sample activity handles all four edges.
- Test long translations, large fonts, narrow windows, RTL, and hidden children.
- Keep native child views accessible; grouping everything into one spoken
  description can hide useful child semantics.
- If your component saves its own mutable UI state, implement state restoration.
  This layout has no additional mutable state to save.
- Under very small height limits, content can be constrained or clipped.
  Respecting the limit is not the same as keeping every line readable; choose
  scrolling or a different layout at the screen level.

The included tests cover exact sizing, bounded sizing, natural height, hidden
children, RTL placement, and updating the displayed text. They do
not replace visual or accessibility checks on a device.

## Why a tutorial, not a library?

The valuable part is the technique: XML describes the children, Kotlin owns
their geometry, and the controller supplies data.

Start with a layout you can understand completely. Extract a library only
when multiple real consumers need the same stable behavior. A general layout
DSL or framework is not required to benefit from custom Views.

## References

- [Custom ViewGroup](https://developer.android.com/develop/ui/views/layout/custom-views/custom-components#custom)
- [View.MeasureSpec](https://developer.android.com/reference/android/view/View.MeasureSpec)
- [ViewGroup.getChildMeasureSpec](https://developer.android.com/reference/android/view/ViewGroup#getChildMeasureSpec(int,int,int))
- [View.resolveSizeAndState](https://developer.android.com/reference/android/view/View#resolveSizeAndState(int,int,int))
- [Macrobenchmark](https://developer.android.com/topic/performance/benchmarking/macrobenchmark-overview)
