package com.huanli233.hibari.sample

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.huanli233.hibari.foundation.Column
import com.huanli233.hibari.foundation.attributes.fillMaxSize
import com.huanli233.hibari.foundation.attributes.fillMaxWidth
import com.huanli233.hibari.foundation.attributes.fitsSystemWindows
import com.huanli233.hibari.foundation.attributes.margins
import com.huanli233.hibari.foundation.attributes.onClick
import com.huanli233.hibari.foundation.attributes.padding
import com.huanli233.hibari.foundation.attributes.subcompose
import com.huanli233.hibari.material.Button
import com.huanli233.hibari.material.Card
import com.huanli233.hibari.material.NestedScrollView
import com.huanli233.hibari.material.Switch
import com.huanli233.hibari.material.Text
import com.huanli233.hibari.material.TextField
import com.huanli233.hibari.material.appbar.AppBarLayout
import com.huanli233.hibari.material.appbar.MaterialToolbar
import com.huanli233.hibari.material.chip.Chip
import com.huanli233.hibari.material.chip.ChipGroup
import com.huanli233.hibari.material.coordinatorlayout.CoordinatorLayout
import com.huanli233.hibari.runtime.HibariApplier
import com.huanli233.hibari.runtime.currentContext
import com.huanli233.hibari.runtime.effects.LaunchedEffect
import com.huanli233.hibari.runtime.getValue
import com.huanli233.hibari.runtime.id
import com.huanli233.hibari.runtime.mutableStateOf
import com.huanli233.hibari.runtime.setValue
import com.huanli233.hibari.ui.Modifier
import com.huanli233.hibari.ui.unit.dp

class TestActivity: AppCompatActivity() {

    @SuppressLint("PrivateResource")
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val view = FrameLayout(this)
        var username by mutableStateOf("")
        var password by mutableStateOf("")
        HibariApplier.applyTo(view) {
            val context = currentContext
            LaunchedEffect {
                Toast.makeText(context, "Hello, World!", Toast.LENGTH_SHORT).show()
            }
            CoordinatorLayout(
                Modifier
                    .fillMaxSize()
                    .fitsSystemWindows(true)
            ) {
                AppBarLayout {
                    MaterialToolbar(title = "Hibari")
                }
                NestedScrollView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .layoutBehavior(AppBarLayout.ScrollingViewBehavior())
                ) {
                    Column(Modifier.fillMaxWidth().padding(16.dp).subcompose()) {
                        TextField(modifier = Modifier.id("username")
                            .fillMaxWidth(), label = "Username", onValueChange = { username = it })
                        TextField(modifier = Modifier.id("password").margins(top = 12.dp)
                            .fillMaxWidth(), label = "Password", onValueChange = { password = it })
                        ChipGroup(modifier = Modifier.margins(top = 16.dp), isSingleSelection = true) {
                            repeat(2) { index ->
                                Chip(
                                    checkable = true,
                                    checkedIconVisible = true,
                                    checkedIcon = com.google.android.material.R.drawable.ic_m3_chip_check,
                                    text = when (index) {
                                        0 -> "Man"
                                        else -> "Woman"
                                    }
                                )
                            }
                        }
                        Switch(text = "Enable Notification", modifier = Modifier.fillMaxWidth())
                        Card(
                            modifier = Modifier
                                .margins(top = 16.dp)
                                .fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Welcome to use Hibari!")
                                Text("As you can see, all layouts are created by Kotlin DSL.", Modifier.margins(top = 8.dp))
                            }
                        }
                        Button("Submit", modifier = Modifier
                            .fillMaxWidth()
                            .margins(top = 20.dp)
                            .onClick {
                                if (username.isNotEmpty() && password.isNotEmpty()) {
                                    MaterialAlertDialogBuilder(context)
                                        .setTitle("Login Info")
                                        .setMessage("Username: ${username}, Password: $password")
                                        .setPositiveButton("OK", null)
                                        .show()
                                } else Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                            })
                    }
                }
            }
        }
        setContentView(view)
    }

}