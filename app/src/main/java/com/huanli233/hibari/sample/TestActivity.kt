package com.huanli233.hibari.sample

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.Gravity
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import com.huanli233.hibari.R
import com.huanli233.hibari.animation.Crossfade
import com.huanli233.hibari.animation.animateFloatAsState
import com.huanli233.hibari.foundation.Box
import com.huanli233.hibari.foundation.attributes.matchParentSize
import com.huanli233.hibari.foundation.attributes.onClick
import com.huanli233.hibari.foundation.attributes.scaleX
import com.huanli233.hibari.foundation.attributes.scaleY
import com.huanli233.hibari.material.NavHost
import com.huanli233.hibari.material.Text
import com.huanli233.hibari.runtime.HibariView
import com.huanli233.hibari.runtime.currentContext
import com.huanli233.hibari.runtime.effects.LaunchedEffect
import com.huanli233.hibari.runtime.getValue
import com.huanli233.hibari.runtime.mutableStateOf
import com.huanli233.hibari.runtime.remember
import com.huanli233.hibari.runtime.setValue
import com.huanli233.hibari.ui.Modifier
import kotlinx.coroutines.delay

class TestActivity: AppCompatActivity() {

    @SuppressLint("PrivateResource")
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        var showContent by mutableStateOf(true)
        setContentView(
            HibariView(this) {
                val context = currentContext
                LaunchedEffect {
                    Toast.makeText(context, "Hello, World!", Toast.LENGTH_SHORT).show()
                }
                LaunchedEffect {
                    showContent = false
                    delay(3000)
                    showContent = true
                }
                val scale by animateFloatAsState(if (showContent) 1.5f else 3f)
                Box(modifier = Modifier.matchParentSize().onClick { showContent = !showContent }) {
                    Crossfade(showContent, Modifier.matchParentSize()) {
                        if (!it) {
                            Text(
                                "Hello, World!",
                                Modifier.gravity(Gravity.CENTER).scaleX(scale).scaleY(scale)
                            )
                        } else {
                            Text(
                                "World, Hello!",
                                Modifier.gravity(Gravity.CENTER).scaleX(scale).scaleY(scale)
                            )
                        }
                    }
                }
            }
        )
    }

}