package com.huanli233.hibari.sample

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.Gravity
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.huanli233.hibari.animation.AnimatedVisibility
import com.huanli233.hibari.animation.animateFloat
import com.huanli233.hibari.animation.updateTransition
import com.huanli233.hibari.foundation.Box
import com.huanli233.hibari.foundation.Column
import com.huanli233.hibari.foundation.attributes.alpha
import com.huanli233.hibari.foundation.attributes.matchParentSize
import com.huanli233.hibari.foundation.attributes.matchParentWidth
import com.huanli233.hibari.foundation.attributes.onClick
import com.huanli233.hibari.foundation.attributes.scaleX
import com.huanli233.hibari.foundation.attributes.scaleY
import com.huanli233.hibari.material.Text
import com.huanli233.hibari.runtime.HibariView
import com.huanli233.hibari.runtime.currentContext
import com.huanli233.hibari.runtime.effects.LaunchedEffect
import com.huanli233.hibari.runtime.getValue
import com.huanli233.hibari.runtime.mutableStateOf
import com.huanli233.hibari.runtime.setValue
import com.huanli233.hibari.ui.Modifier

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
                }
                val transition = updateTransition(showContent)
                val scale by transition.animateFloat {
                    if (it) 3f else 1.5f
                }
                val alpha by transition.animateFloat {
                    if (it) 1f else 0.5f
                }
                Box(modifier = Modifier.matchParentSize().onClick { showContent = !showContent }) {
                    val textModifier = Modifier.gravity(Gravity.CENTER).scaleX(scale).scaleY(scale).alpha(alpha)
                    if (!showContent) {
                        Text(
                            "Hello, World!",
                            textModifier
                        )
                    } else {
                        Text(
                            "World, Hello!",
                            textModifier
                        )
                    }
                    Column(matchParentWidth().gravity(Gravity.CENTER)) {
                        AnimatedVisibility(showContent, matchParentSize()) {
                            Text("test")
                        }
                    }
                }
            }
        )
    }

}