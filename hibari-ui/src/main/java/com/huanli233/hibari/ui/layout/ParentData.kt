package com.huanli233.hibari.ui.layout

import com.huanli233.hibari.ui.Modifier
import com.huanli233.hibari.ui.unit.Density

interface ParentDataModifier : Modifier.Element {
    fun modifyParentData(parentData: Any?): Any
}