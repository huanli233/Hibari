package com.huanli233.hibari.material.chip

import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.huanli233.hibari.foundation.Node
import com.huanli233.hibari.runtime.Tunable
import com.huanli233.hibari.ui.Modifier
import com.huanli233.hibari.ui.thenViewAttribute
import com.huanli233.hibari.ui.thenViewAttributeIfNotNull
import com.huanli233.hibari.ui.uniqueKey
import com.huanli233.hibari.ui.viewClass

@Tunable
fun Chip(
    text: CharSequence,
    modifier: Modifier = Modifier,
    enabled: Boolean? = null,
    icon: Any? = null,
    checkable: Boolean = false,
    closeIconVisible: Boolean? = null,
    checkedIconVisible: Boolean? = null,
    checkedIcon: Any? = null,
    onCloseClick: (() -> Unit)? = null
) {
    Node(
        modifier = modifier
            .viewClass(Chip::class.java)
            .thenViewAttribute<Chip, CharSequence>(uniqueKey, text) { this.text = it }
            .thenViewAttributeIfNotNull<Chip, Boolean>(uniqueKey, enabled) { this.isEnabled = it }
            .thenViewAttributeIfNotNull<Chip, Any>(uniqueKey, icon) {
                when (it) {
                    is Int -> chipIcon = ContextCompat.getDrawable(context, it)
                    is Drawable -> chipIcon = it
                }
            }
            .thenViewAttributeIfNotNull<Chip, Boolean>(uniqueKey, checkable) { this.isCheckable = it }
            .thenViewAttributeIfNotNull<Chip, Boolean>(uniqueKey, closeIconVisible) { this.isCloseIconVisible = it }
            .thenViewAttributeIfNotNull<Chip, Boolean>(uniqueKey, checkedIconVisible) { this.isCheckedIconVisible = it }
            .thenViewAttributeIfNotNull<Chip, Any>(uniqueKey, checkedIcon) {
                when (it) {
                    is Int -> this.checkedIcon = ContextCompat.getDrawable(context, it)
                    is Drawable -> this.checkedIcon = it
                }
            }
            .thenViewAttributeIfNotNull<Chip, () -> Unit>(uniqueKey, onCloseClick) { setOnCloseIconClickListener { it() } }
    )
}

@Tunable
fun ChipGroup(
    modifier: Modifier = Modifier,
    isSingleSelection: Boolean = false,
    content: @Tunable () -> Unit
) {
    Node(modifier.viewClass(ChipGroup::class.java)
        .thenViewAttribute<ChipGroup, Boolean>(uniqueKey, isSingleSelection) {
            this.isSingleSelection = it
        }, content)
}