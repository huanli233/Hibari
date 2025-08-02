package com.huanli233.hibari.sample

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.transition.MaterialFade
import com.huanli233.hibari.R
import com.huanli233.hibari.foundation.Box
import com.huanli233.hibari.foundation.Column
import com.huanli233.hibari.foundation.attributes.ellipsis
import com.huanli233.hibari.foundation.attributes.matchParentSize
import com.huanli233.hibari.foundation.attributes.matchParentWidth
import com.huanli233.hibari.foundation.attributes.fitsSystemWindows
import com.huanli233.hibari.foundation.attributes.padding
import com.huanli233.hibari.foundation.attributes.paddingRelative
import com.huanli233.hibari.foundation.attributes.onClick
import com.huanli233.hibari.foundation.attributes.padding
import com.huanli233.hibari.foundation.attributes.textAppearance
import com.huanli233.hibari.material.Card
import com.huanli233.hibari.material.Text
import com.huanli233.hibari.material.appbar.AppBarLayout
import com.huanli233.hibari.material.appbar.MaterialToolbar
import com.huanli233.hibari.material.coordinatorlayout.CoordinatorLayout
import com.huanli233.hibari.material.floatingactionbutton.FloatingActionButton
import com.huanli233.hibari.recyclerview.LazyColumn
import com.huanli233.hibari.runtime.HibariView
import com.huanli233.hibari.runtime.attrColorRes
import com.huanli233.hibari.runtime.attrIdRes
import com.huanli233.hibari.runtime.effects.LaunchedEffect
import com.huanli233.hibari.runtime.getValue
import com.huanli233.hibari.runtime.livedata.observeAsState
import com.huanli233.hibari.runtime.mutableStateOf
import com.huanli233.hibari.runtime.remember
import com.huanli233.hibari.runtime.setValue
import com.huanli233.hibari.ui.Modifier
import com.huanli233.hibari.ui.text.TextTruncateAt
import com.huanli233.hibari.ui.unit.dp
import kotlinx.coroutines.delay

class NoteListFragment: Fragment() {

    private val viewModel: MainViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        exitTransition = MaterialFade()
        reenterTransition = MaterialFade()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return HibariView(requireContext()) {
            val notes by viewModel.notes.observeAsState(emptyList())
            Box(Modifier.matchParentSize()) {
                CoordinatorLayout(Modifier.matchParentSize().fitsSystemWindows(true)) {
                    AppBarLayout(Modifier.matchParentWidth()) {
                        MaterialToolbar(Modifier.matchParentWidth().scrollFlags(AppBarLayout.LayoutParams.SCROLL_FLAG_NO_SCROLL), title = "Notes")
                    }
                    LazyColumn(Modifier.matchParentSize().layoutBehavior(
                        AppBarLayout.ScrollingViewBehavior()
                    )) {
                        items(notes) {
                            Card(
                                modifier = Modifier
                                    .matchParentWidth()
                                    .paddingRelative(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)
                                    .onClick {
                                        findNavController().navigate(R.id.action_noteList_to_noteDetail, bundleOf("note" to it))
                                    }
                            ) {
                                Column(Modifier.matchParentWidth().padding(16.dp)) {
                                    Text(it.title, Modifier.matchParentWidth().textAppearance(attrIdRes(com.google.android.material.R.attr.textAppearanceTitleMedium)))
                                    Text(
                                        text = it.content,
                                        modifier = Modifier
                                            .textAppearance(attrIdRes(com.google.android.material.R.attr.textAppearanceBodyMedium))
                                            .ellipsis(TextTruncateAt.END)
                                            .paddingRelative(top = 8.dp),
                                        color = attrColorRes(com.google.android.material.R.attr.colorOnSurfaceVariant),
                                        maxLines = 3
                                    )
                                }
                            }
                        }
                    }
                    FloatingActionButton(
                        modifier = Modifier.gravity(Gravity.BOTTOM or Gravity.END)
                            .padding(16.dp)
                            .onClick {
                                findNavController().navigate(R.id.action_noteList_to_noteDetail)
                            },
                        icon = R.drawable.add_24
                    )
                }
            }
        }
    }

}