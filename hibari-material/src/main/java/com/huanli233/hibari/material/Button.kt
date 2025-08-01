package com.huanli233.hibari.material

import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import androidx.annotation.Px
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.huanli233.hibari.foundation.Node
import com.huanli233.hibari.runtime.Tunable
import com.huanli233.hibari.ui.Modifier
import com.huanli233.hibari.ui.thenViewAttribute
import com.huanli233.hibari.ui.thenViewAttributeIfNotNull
import com.huanli233.hibari.ui.uniqueKey
import com.huanli233.hibari.ui.viewClass

@Tunable
fun Button(
    text: CharSequence,
    modifier: Modifier = Modifier,
    enabled: Boolean? = null,
    icon: Any? = null,
    @MaterialButton.IconGravity iconGravity: Int? = null,
    iconTint: ColorStateList? = null,
    @Px strokeWidth: Int? = null,
    strokeColor: ColorStateList? = null,
    @Px cornerRadius: Int? = null,
) {
    Node(
        modifier = modifier
            .viewClass(MaterialButton::class.java)
            .thenViewAttribute<MaterialButton, CharSequence>(uniqueKey, text) { this.text = it }
            .thenViewAttributeIfNotNull<MaterialButton, Boolean>(uniqueKey, enabled) { this.isEnabled = it }
            .thenViewAttributeIfNotNull<MaterialButton, Any>(uniqueKey, icon) {
                when (it) {
                    is Int -> setIconResource(it)
                    is Drawable -> setIcon(it)
                }
            }
            .thenViewAttributeIfNotNull<MaterialButton, Int>(uniqueKey, iconGravity) { this.iconGravity = it }
            .thenViewAttributeIfNotNull<MaterialButton, ColorStateList>(uniqueKey, iconTint) { this.iconTint = it }
            .thenViewAttributeIfNotNull<MaterialButton, Int>(uniqueKey, strokeWidth) { this.strokeWidth = it }
            .thenViewAttributeIfNotNull<MaterialButton, ColorStateList>(uniqueKey, strokeColor) { this.strokeColor = it }
            .thenViewAttributeIfNotNull<MaterialButton, Int>(uniqueKey, cornerRadius) { this.cornerRadius = it }
    )
}

@Tunable
fun ButtonToggleGroup(
    modifier: Modifier = Modifier,
    checkedIds: List<Int>,
    onButtonChecked: (buttonId: Int, isChecked: Boolean) -> Unit,
    singleSelection: Boolean? = null,
    selectionRequired: Boolean? = null,
    content: @Tunable ButtonToggleGroupScope.() -> Unit
) {
    Node(
        modifier = modifier
            .viewClass(MaterialButtonToggleGroup::class.java)
            .thenViewAttributeIfNotNull<MaterialButtonToggleGroup, Boolean>(uniqueKey, singleSelection) { isSingleSelection = it }
            .thenViewAttributeIfNotNull<MaterialButtonToggleGroup, Boolean>(uniqueKey, selectionRequired) { isSelectionRequired = it }
            .thenViewAttribute<MaterialButtonToggleGroup, List<Int>>(uniqueKey, checkedIds) {
                // Sync the view's state with the provided state list
                val currentChecked = this.checkedButtonIds
                if (currentChecked != it) {
                    // Clear all, then check the ones from the new state.
                    // A more optimized diffing could be done, but this is robust.
                    clearChecked()
                    it.forEach { id -> check(id) }
                }
            }
            .thenViewAttribute<MaterialButtonToggleGroup, (Int, Boolean) -> Unit>(uniqueKey, onButtonChecked) { listener ->
                // Clear existing listeners to prevent duplicates on recomposition
                clearOnButtonCheckedListeners()
                addOnButtonCheckedListener { group, checkedId, isChecked ->
                    listener(checkedId, isChecked)
                }
            },
        content = {
            ButtonToggleGroupScopeInstance.content()
        }
    )
}

interface ButtonToggleGroupScope
internal object ButtonToggleGroupScopeInstance : ButtonToggleGroupScope