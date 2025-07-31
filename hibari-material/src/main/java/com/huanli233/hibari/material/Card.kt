package com.huanli233.hibari.material

import android.content.res.ColorStateList
import androidx.annotation.Px
import com.google.android.material.card.MaterialCardView
import com.huanli233.hibari.foundation.Node
import com.huanli233.hibari.foundation.attributes.PaddingValues
import com.huanli233.hibari.runtime.Tunable
import com.huanli233.hibari.ui.Modifier
import com.huanli233.hibari.ui.thenViewAttributeIfNotNull
import com.huanli233.hibari.ui.uniqueKey
import com.huanli233.hibari.ui.unit.Dp
import com.huanli233.hibari.ui.unit.toPx
import com.huanli233.hibari.ui.viewClass

@Tunable
fun Card(
    modifier: Modifier = Modifier,
    cardElevation: Float? = null,
    cardBackgroundColor: Any? = null,
    @Px radius: Float? = null,
    @Px strokeWidth: Int? = null,
    strokeColor: Any? = null,
    contentPadding: PaddingValues? = null,
    isClickable: Boolean? = null,
    isLongClickable: Boolean? = null,
    content: @Tunable CardScope.() -> Unit
) {
    Node(
        modifier = modifier
            .viewClass(MaterialCardView::class.java)
            .thenViewAttributeIfNotNull<MaterialCardView, Float>(uniqueKey, cardElevation) { this.cardElevation = it }
            .thenViewAttributeIfNotNull<MaterialCardView, Any>(uniqueKey, cardBackgroundColor) {
                when (it) {
                    is ColorStateList -> setCardBackgroundColor(it)
                    is Int -> setCardBackgroundColor(it)
                }
            }
            .thenViewAttributeIfNotNull<MaterialCardView, Float>(uniqueKey, radius) { this.radius = it }
            .thenViewAttributeIfNotNull<MaterialCardView, Int>(uniqueKey, strokeWidth) { this.strokeWidth = it }
            .thenViewAttributeIfNotNull<MaterialCardView, Any>(uniqueKey, strokeColor) {
                when (it) {
                    is ColorStateList -> setStrokeColor(it)
                    is Int -> setStrokeColor(it)
                }
            }
            .thenViewAttributeIfNotNull<MaterialCardView, PaddingValues>(uniqueKey, contentPadding) {
                this.setContentPadding(it.left.toPx(this),
                    it.top.toPx(this), it.right.toPx(this), it.bottom.toPx(this)
                )
            }
            .thenViewAttributeIfNotNull<MaterialCardView, Boolean>(uniqueKey, isClickable) {
                this.isClickable = it
            }
            .thenViewAttributeIfNotNull<MaterialCardView, Boolean>(uniqueKey, isLongClickable) {
                this.isLongClickable = it
            },
        content = {
            CardScopeInstance.content()
        }
    )
}

interface CardScope
internal object CardScopeInstance : CardScope