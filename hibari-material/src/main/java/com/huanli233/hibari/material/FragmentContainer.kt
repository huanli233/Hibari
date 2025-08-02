package com.huanli233.hibari.material

import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentOnAttachListener
import androidx.fragment.app.commit
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.highcapable.betterandroid.ui.extension.view.toast
import com.huanli233.hibari.foundation.Node
import com.huanli233.hibari.runtime.RememberObserver
import com.huanli233.hibari.runtime.Renderer
import com.huanli233.hibari.runtime.Tunable
import com.huanli233.hibari.runtime.effects.DisposableEffect
import com.huanli233.hibari.runtime.id
import com.huanli233.hibari.runtime.intId
import com.huanli233.hibari.runtime.remember
import com.huanli233.hibari.runtime.viewId
import com.huanli233.hibari.ui.Modifier
import com.huanli233.hibari.ui.ref
import com.huanli233.hibari.ui.thenViewAttribute
import com.huanli233.hibari.ui.viewClass

@Tunable
fun FragmentContainer(
    modifier: Modifier = Modifier,
    fragment: Fragment,
    fragmentManager: FragmentManager
) {
    val id = remember { Renderer.generateRandomViewId() }
    Node(
        modifier = modifier
            .viewClass(FragmentContainerView::class.java)
            .id(id)
            .thenViewAttribute<FragmentContainerView, Fragment>(com.huanli233.hibari.ui.uniqueKey, fragment) {
                fragmentManager.commit {
                    add(id.viewId, fragment)
                }
            }
    )
}

@Tunable
fun NavHost(
    id: Int? = null,
    modifier: Modifier = Modifier,
    fragmentManager: FragmentManager,
    setupNavController: (NavController) -> Unit,
    navHostFactory: () -> NavHostFragment
) {
    val id = id ?: remember { ViewCompat.generateViewId() }
    val navHostFragment = remember { navHostFactory() }
    remember {
        val listener = FragmentOnAttachListener { fragmentManager, fragment ->
            if (fragment.id == navHostFragment.id) {
                setupNavController(navHostFragment.navController)
            }
        }
        fragmentManager.addFragmentOnAttachListener(listener)
        object : RememberObserver {
            override fun onRemembered() = Unit

            override fun onForgotten() {
                fragmentManager.removeFragmentOnAttachListener(listener)
            }
        }
    }
    Node(
        modifier = modifier
            .viewClass(FragmentContainerView::class.java)
            .intId(id)
            .thenViewAttribute<FragmentContainerView, NavHostFragment>(com.huanli233.hibari.ui.uniqueKey, navHostFragment) {
                if (fragmentManager.findFragmentById(id) == null) {
                    fragmentManager.commit {
                        add(id, navHostFragment)
                        setPrimaryNavigationFragment(navHostFragment)
                    }
                }
            }
    )
}