package com.huanli233.hibari.sample

import android.os.Bundle
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.OnApplyWindowInsetsListener
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.appbar.AppBarLayout
import com.huanli233.hibari.foundation.Column
import com.huanli233.hibari.foundation.attributes.PaddingValues
import com.huanli233.hibari.foundation.attributes.fillMaxSize
import com.huanli233.hibari.foundation.attributes.fillMaxWidth
import com.huanli233.hibari.foundation.attributes.onClick
import com.huanli233.hibari.foundation.attributes.padding
import com.huanli233.hibari.foundation.attributes.subcompose
import com.huanli233.hibari.material.Button
import com.huanli233.hibari.material.Card
import com.huanli233.hibari.material.NestedScrollView
import com.huanli233.hibari.material.Text
import com.huanli233.hibari.material.TextField
import com.huanli233.hibari.material.appbar.AppBarLayout
import com.huanli233.hibari.material.appbar.MaterialToolbar
import com.huanli233.hibari.material.coordinatorlayout.CoordinatorLayout
import com.huanli233.hibari.runtime.HibariApplier
import com.huanli233.hibari.runtime.LocalContext
import com.huanli233.hibari.runtime.TunationLocalProvider
import com.huanli233.hibari.runtime.effects.LaunchedEffect
import com.huanli233.hibari.runtime.getValue
import com.huanli233.hibari.runtime.id
import com.huanli233.hibari.runtime.mutableStateOf
import com.huanli233.hibari.runtime.remember
import com.huanli233.hibari.runtime.setValue
import com.huanli233.hibari.runtime.tunationLocalOf
import com.huanli233.hibari.ui.Modifier
import com.huanli233.hibari.ui.unit.dp
import com.huanli233.hibari.ui.util.toDp

class TestActivity: AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val view = FrameLayout(this)
        HibariApplier.applyTo(view) {
            var text by remember { mutableStateOf("Hello, World!") }
            TunationLocalProvider(
                LocalContext provides this
            ) {
                var windowInsets by remember { mutableStateOf<WindowInsetsCompat?>(null) }
                var flag by remember { mutableStateOf(false) }
                val context = LocalContext.current
                LaunchedEffect {
                    Toast.makeText(context, "Test", Toast.LENGTH_SHORT).show()
                }
                LaunchedEffect {
                    val listener = OnApplyWindowInsetsListener { _, receivedInsets ->
                        windowInsets = receivedInsets
                        receivedInsets
                    }

                    ViewCompat.setOnApplyWindowInsetsListener(view, listener)
                    ViewCompat.requestApplyInsets(view)
                }
                val insets = windowInsets?.getInsets(WindowInsetsCompat.Type.systemBars())
                CoordinatorLayout(
                    Modifier
                        .fillMaxSize()
                        .run {
                            if (insets != null) {
                                padding(top = insets.top.toDp(context).dp, bottom = insets.bottom.toDp(context).dp, left = insets.left.toDp(context).dp, right = insets.right.toDp(context).dp)
                            } else this
                        }
                ) {
                    AppBarLayout {
                        MaterialToolbar(title = "Hibari")
                    }
                    NestedScrollView(
                        modifier = Modifier
                            .fillMaxWidth()
                            .layoutBehavior(AppBarLayout.ScrollingViewBehavior())
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .subcompose(),
                                contentPadding = PaddingValues(6.dp, 6.dp, 6.dp, 6.dp),
                                isClickable = true
                            ) {
                                Column(Modifier.padding(16.dp)) {
                                    Text(text)
                                    TextField(
                                        modifier = Modifier.fillMaxWidth().id("field"),
                                        onValueChange = {
                                            text = it
                                        }
                                    )
                                    Button("test", modifier = Modifier.onClick {
                                        text = "Hello, World!123"
                                    })
                                }
                            }
                        }
                    }
                }
            }
        }
        setContentView(view)
    }

}