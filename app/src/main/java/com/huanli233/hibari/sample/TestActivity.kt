package com.huanli233.hibari.sample

import android.os.Bundle
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.huanli233.hibari.foundation.Node
import com.huanli233.hibari.runtime.GlobalRecomposer
import com.huanli233.hibari.runtime.HibariApplier
import com.huanli233.hibari.runtime.Tunation
import com.huanli233.hibari.runtime.getValue
import com.huanli233.hibari.runtime.mutableStateOf
import com.huanli233.hibari.runtime.setValue
import com.huanli233.hibari.ui.Modifier
import com.huanli233.hibari.ui.text
import com.huanli233.hibari.ui.viewClass
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class TestActivity: AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val view = FrameLayout(this)
        var text by mutableStateOf("Hello, World!")
        HibariApplier.applyTo(view) {
            Node(
                modifier = Modifier
                    .viewClass(TextView::class.java)
                    .text(text)
            )
        }
        setContentView(view)
        lifecycleScope.launch {
            delay(5000)
            text = "Hello, World!1231111111111"
        }
    }

}