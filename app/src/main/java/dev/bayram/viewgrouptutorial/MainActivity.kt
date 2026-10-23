package dev.bayram.viewgrouptutorial

import android.app.Activity
import android.os.Bundle
import android.view.WindowInsets
import android.widget.ScrollView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<ScrollView>(R.id.scroll).setOnApplyWindowInsetsListener { view, insets ->
            @Suppress("DEPRECATION")
            if (android.os.Build.VERSION.SDK_INT >= 30) {
                val safeArea = insets.getInsets(
                    WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout()
                )
                view.setPadding(safeArea.left, safeArea.top, safeArea.right, safeArea.bottom)
            } else {
                view.setPadding(
                    insets.systemWindowInsetLeft,
                    insets.systemWindowInsetTop,
                    insets.systemWindowInsetRight,
                    insets.systemWindowInsetBottom
                )
            }
            insets
        }
    }
}
