package com.huanli233.hibari.material

import androidx.appcompat.widget.AppCompatCheckedTextView
import com.huanli233.hibari.foundation.Node
import com.huanli233.hibari.runtime.Tunable
import com.huanli233.hibari.ui.Modifier
import com.huanli233.hibari.ui.thenViewAttribute
import com.huanli233.hibari.ui.thenViewAttributeIfNotNull
import com.huanli233.hibari.ui.uniqueKey
import com.huanli233.hibari.ui.viewClass

@Tunable
fun CheckedTextView(
    text: CharSequence,
    checked: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean? = null
) {
    Node(
        modifier = modifier
            .viewClass(AppCompatCheckedTextView::class.java)
            .thenViewAttribute<AppCompatCheckedTextView, CharSequence>(uniqueKey, text) { this.text = it }
            .thenViewAttribute<AppCompatCheckedTextView, Boolean>(uniqueKey, checked) {
                if (this.isChecked != it) {
                    this.isChecked = it
                }
            }
            .thenViewAttributeIfNotNull<AppCompatCheckedTextView, Boolean>(uniqueKey, enabled) { this.isEnabled = it }
    )
}