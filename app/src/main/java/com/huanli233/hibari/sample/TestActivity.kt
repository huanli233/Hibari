package com.huanli233.hibari.sample

import android.os.Bundle
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.huanli233.hibari.material.Text
import com.huanli233.hibari.runtime.HibariApplier
import com.huanli233.hibari.runtime.getValue
import com.huanli233.hibari.runtime.mutableStateOf
import com.huanli233.hibari.runtime.setValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class TestActivity: AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val view = FrameLayout(this)
        var text by mutableStateOf("Hello, World!")
        HibariApplier.applyTo(view) {
            Text(text)
        }
        setContentView(view)
        lifecycleScope.launch {
            delay(5000)
            text = "Hello, World!1231111111111"
        }
    }

}