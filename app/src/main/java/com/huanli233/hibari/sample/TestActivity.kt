package com.huanli233.hibari.sample

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import com.huanli233.hibari.R
import com.huanli233.hibari.material.NavHost
import com.huanli233.hibari.runtime.HibariView
import com.huanli233.hibari.runtime.currentContext
import com.huanli233.hibari.runtime.effects.LaunchedEffect

class TestActivity: AppCompatActivity() {

    @SuppressLint("PrivateResource")
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(
            HibariView(this) {
                val context = currentContext
                LaunchedEffect {
                    Toast.makeText(context, "Hello, World!", Toast.LENGTH_SHORT).show()
                }
                NavHost(id = R.id.nav_host, fragmentManager = supportFragmentManager, setupNavController = {
                    it.setGraph(R.navigation.main_graph)
                }) {
                    NavHostFragment()
                }
            }
        )
    }

}