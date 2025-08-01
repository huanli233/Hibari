package com.huanli233.hibari.material.bottomsheet

import com.google.android.material.bottomsheet.BottomSheetDragHandleView
import com.huanli233.hibari.foundation.Node
import com.huanli233.hibari.runtime.Tunable
import com.huanli233.hibari.ui.Modifier
import com.huanli233.hibari.ui.viewClass

@Tunable
fun BottomSheetDragHandle(
    modifier: Modifier = Modifier
) {
    Node(
        modifier = modifier
            .viewClass(BottomSheetDragHandleView::class.java)
    )
}