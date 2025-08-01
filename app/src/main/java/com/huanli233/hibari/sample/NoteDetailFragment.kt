package com.huanli233.hibari.sample

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.transition.MaterialContainerTransform
import com.google.android.material.transition.MaterialSharedAxis
import com.huanli233.hibari.R
import com.huanli233.hibari.foundation.Column
import com.huanli233.hibari.foundation.attributes.fillMaxSize
import com.huanli233.hibari.foundation.attributes.fillMaxWidth
import com.huanli233.hibari.foundation.attributes.fitsSystemWindows
import com.huanli233.hibari.foundation.attributes.margin
import com.huanli233.hibari.foundation.attributes.onClick
import com.huanli233.hibari.foundation.attributes.padding
import com.huanli233.hibari.foundation.attributes.systemBarsPadding
import com.huanli233.hibari.material.TextField
import com.huanli233.hibari.material.appbar.AppBarLayout
import com.huanli233.hibari.material.appbar.MaterialToolbar
import com.huanli233.hibari.material.coordinatorlayout.CoordinatorLayout
import com.huanli233.hibari.material.floatingactionbutton.FloatingActionButton
import com.huanli233.hibari.runtime.HibariView
import com.huanli233.hibari.runtime.currentView
import com.huanli233.hibari.runtime.getValue
import com.huanli233.hibari.runtime.mutableStateOf
import com.huanli233.hibari.runtime.remember
import com.huanli233.hibari.runtime.setValue
import com.huanli233.hibari.runtime.viewId
import com.huanli233.hibari.ui.Modifier
import com.huanli233.hibari.ui.ref
import com.huanli233.hibari.ui.unit.dp

class NoteDetailFragment : Fragment() {

    private val viewModel: MainViewModel by activityViewModels()

    private val currentNote: Note? by lazy { @Suppress("DEPRECATION") arguments?.getParcelable("note") }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.X, false)
        enterTransition = MaterialSharedAxis(MaterialSharedAxis.X, true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return HibariView(requireContext()) {
            var title by remember { mutableStateOf(currentNote?.title.orEmpty()) }
            var content by remember { mutableStateOf(currentNote?.content.orEmpty()) }
            val view = currentView
            CoordinatorLayout(Modifier.fillMaxSize().fitsSystemWindows(true)) {
                AppBarLayout(Modifier.fillMaxWidth()) {
                    MaterialToolbar(Modifier.fillMaxWidth().ref {
                        it as MaterialToolbar
                        it.setupWithNavController(findNavController())
                    }, title = "Note Detail")
                }
                Column(Modifier.fillMaxWidth().padding(16.dp).layoutBehavior(AppBarLayout.ScrollingViewBehavior())) {
                    TextField(title, modifier = Modifier.fillMaxWidth(), label = "Title", onValueChange = { title = it })
                    TextField(
                        content,
                        modifier = Modifier.fillMaxWidth()
                            .weight(1f)
                            .margin(top = 16.dp),
                        label = "Content",
                        onValueChange = { content = it }
                    )
                }
                FloatingActionButton(
                    modifier = Modifier.margin(16.dp)
                        .gravity(Gravity.END or Gravity.BOTTOM)
                        .layoutAnchorGravity(Gravity.END or Gravity.BOTTOM)
                        .onClick {
                            if (title.isEmpty()) {
                                Snackbar.make(view, "Title cannot be empty", Snackbar.LENGTH_SHORT).show()
                            } else {
                                val noteToSave = currentNote?.copy(
                                    title = title,
                                    content = content
                                ) ?: Note(title = title, content = content)
                                viewModel.saveNote(noteToSave)
                                findNavController().popBackStack()
                            }
                        },
                    icon = R.drawable.outline_save_24
                )
            }
        }
    }

}