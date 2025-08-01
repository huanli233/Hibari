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
import androidx.transition.Fade
import com.google.android.material.appbar.AppBarLayout
import com.huanli233.hibari.R
import com.huanli233.hibari.foundation.Box
import com.huanli233.hibari.foundation.Column
import com.huanli233.hibari.foundation.attributes.ellipsis
import com.huanli233.hibari.foundation.attributes.fillMaxSize
import com.huanli233.hibari.foundation.attributes.fillMaxWidth
import com.huanli233.hibari.foundation.attributes.fitsSystemWindows
import com.huanli233.hibari.foundation.attributes.margin
import com.huanli233.hibari.foundation.attributes.marginRelative
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
import com.huanli233.hibari.runtime.getValue
import com.huanli233.hibari.runtime.livedata.observeAsState
import com.huanli233.hibari.ui.Modifier
import com.huanli233.hibari.ui.text.TextTruncateAt
import com.huanli233.hibari.ui.unit.dp

class NoteListFragment: Fragment() {

    private val viewModel: MainViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        exitTransition = Fade()
        reenterTransition = Fade()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return HibariView(requireContext()) {
            val notes by viewModel.notes.observeAsState(emptyList())
            Box(Modifier.fillMaxSize()) {
                CoordinatorLayout(Modifier.fillMaxSize().fitsSystemWindows(true)) {
                    AppBarLayout(Modifier.fillMaxWidth()) {
                        MaterialToolbar(Modifier.fillMaxWidth().scrollFlags(AppBarLayout.LayoutParams.SCROLL_FLAG_NO_SCROLL), title = "Notes")
                    }
                    LazyColumn(Modifier.fillMaxSize().layoutBehavior(
                        AppBarLayout.ScrollingViewBehavior()
                    )) {
                        items(notes) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .marginRelative(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)
                                    .onClick {
                                        findNavController().navigate(R.id.action_noteList_to_noteDetail, bundleOf("note" to it))
                                    }
                            ) {
                                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                                    Text(it.title, Modifier.fillMaxWidth().textAppearance(attrIdRes(com.google.android.material.R.attr.textAppearanceTitleMedium)))
                                    Text(
                                        text = it.content,
                                        modifier = Modifier
                                            .textAppearance(attrIdRes(com.google.android.material.R.attr.textAppearanceBodyMedium))
                                            .ellipsis(TextTruncateAt.END)
                                            .marginRelative(top = 8.dp),
                                        color = attrColorRes(com.google.android.material.R.attr.colorOnSurfaceVariant),
                                        maxLines = 3
                                    )
                                }
                            }
                        }
                    }
                    FloatingActionButton(
                        modifier = Modifier.gravity(Gravity.BOTTOM or Gravity.END)
                            .margin(16.dp)
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