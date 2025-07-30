package com.huanli233.hibari.material

import com.google.android.material.textview.MaterialTextView
import com.huanli233.hibari.foundation.Node
import com.huanli233.hibari.runtime.Tunable
import com.huanli233.hibari.ui.Modifier
import com.huanli233.hibari.ui.attributes.maxLines
import com.huanli233.hibari.ui.attributes.minLines
import com.huanli233.hibari.ui.attributes.text
import com.huanli233.hibari.ui.attributes.textAlign
import com.huanli233.hibari.ui.attributes.textColor
import com.huanli233.hibari.ui.attributes.textSize
import com.huanli233.hibari.ui.text.TextAlign
import com.huanli233.hibari.ui.unit.TextUnit
import com.huanli233.hibari.ui.viewClass

@Tunable
fun Text(
    text: CharSequence,
    modifier: Modifier = Modifier,
    color: Int? = null,
    textSize: TextUnit = TextUnit.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    textAlign: TextAlign? = null,
) {
    Node(
        modifier = modifier
            .viewClass(MaterialTextView::class.java)
            .text(text)
            .textSize(textSize)
            .apply {
                if (color != null) {
                    textColor(color)
                }
                if (textAlign != null) {
                    textAlign(textAlign)
                }
            }
            .minLines(minLines)
            .maxLines(maxLines)
    )
}