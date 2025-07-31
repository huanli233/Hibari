package com.huanli233.hibari.sample

import android.os.Bundle
import android.util.Log
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.OnApplyWindowInsetsListener
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.huanli233.hibari.foundation.Column
import com.huanli233.hibari.foundation.Row
import com.huanli233.hibari.foundation.attributes.fillMaxHeight
import com.huanli233.hibari.foundation.attributes.fillMaxSize
import com.huanli233.hibari.foundation.attributes.fillMaxWidth
import com.huanli233.hibari.foundation.attributes.onClick
import com.huanli233.hibari.foundation.attributes.padding
import com.huanli233.hibari.foundation.attributes.subcompose
import com.huanli233.hibari.material.Button
import com.huanli233.hibari.material.Text
import com.huanli233.hibari.recyclerview.LazyColumn
import com.huanli233.hibari.runtime.HibariApplier
import com.huanli233.hibari.runtime.LocalContext
import com.huanli233.hibari.runtime.TunationLocalProvider
import com.huanli233.hibari.runtime.effects.LaunchedEffect
import com.huanli233.hibari.runtime.getValue
import com.huanli233.hibari.runtime.mutableStateOf
import com.huanli233.hibari.runtime.remember
import com.huanli233.hibari.runtime.setValue
import com.huanli233.hibari.runtime.tunationLocalOf
import com.huanli233.hibari.ui.Modifier
import com.huanli233.hibari.ui.unit.dp
import com.huanli233.hibari.ui.util.toDp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

val LocalManualWindowInsets = tunationLocalOf<WindowInsetsCompat?> { null }

class TestActivity: AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val view = FrameLayout(this)
        var text by mutableStateOf("Hello, World!")
        HibariApplier.applyTo(view) {
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
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .run {
                            if (insets != null) {
                                padding(top = insets.top.toDp(context).dp, bottom = insets.bottom.toDp(context).dp, left = insets.left.toDp(context).dp, right = insets.right.toDp(context).dp)
                            } else this
                        }
                ) {
                    Text("Hello, World!")
                    var items by remember { mutableStateOf(listOf("Test1", "Test2", 123)) }
                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                            .weight(1f)
                    ) {
                        items(items) {
                            if (it is Int) {
                                Button(it.toString(), modifier = Modifier.onClick {
                                    Toast.makeText(context, "Click", Toast.LENGTH_SHORT).show()
                                })
                            } else if (it is String) {
                                Text(it)
                            }
                        }
                    }
                    LaunchedEffect {
                        delay(3000)
                        items = listOf("Test1", 1, 2, 3)
                    }
                }
            }
        }
        setContentView(view)
        lifecycleScope.launch {
            delay(5000)
            text = "Hello, World!1231111111111"
        }
    }

}