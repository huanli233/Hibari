package com.huanli233.hibari.sample

import android.os.Bundle
import android.os.PersistableBundle
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.huanli233.hibari.foundation.Node
import com.huanli233.hibari.runtime.GlobalRecomposer
import com.huanli233.hibari.runtime.Tunable
import com.huanli233.hibari.runtime.Tunation
import com.huanli233.hibari.runtime.getValue
import com.huanli233.hibari.runtime.mutableStateOf
import com.huanli233.hibari.runtime.setValue
import com.huanli233.hibari.ui.Modifier
import com.huanli233.hibari.ui.TextViewAttributeKeys
import com.huanli233.hibari.ui.attribute
import com.huanli233.hibari.ui.viewClass
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class TestActivity: AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val view = FrameLayout(this)
        var text by mutableStateOf("Hello, World!")
        val tunation = Tunation(
            view,
            content = {
                Node(modifier = Modifier
                    .viewClass(TextView::class.java)
                    .attribute(TextViewAttributeKeys.text, text))
            }
        )
        GlobalRecomposer.scheduleTune(tunation)
        setContentView(view)
        lifecycleScope.launch {
            delay(5000)
            text = "Hello, World!1231111111111"
        }
    }

}