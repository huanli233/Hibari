package com.huanli233.hibari.material

import android.widget.ImageView
import com.huanli233.hibari.foundation.Node
import com.huanli233.hibari.runtime.Tunable
import com.huanli233.hibari.ui.Modifier
import com.huanli233.hibari.ui.viewClass
import com.huanli233.hibari.ui.thenViewAttribute

@Tunable
fun Image(
    src: Int? = null,
    modifier: Modifier = Modifier,
    scaleType: ImageView.ScaleType = ImageView.ScaleType.CENTER_CROP,
    onClick: (() -> Unit)? = null,
) {
    Node(
        modifier = modifier
            .viewClass(ImageView::class.java)
            .thenViewAttribute<ImageView, Int?>(src) { imageRes ->
                if (imageRes != null) {
                    setImageResource(imageRes)
                }
            }
            .thenViewAttribute<ImageView, ImageView.ScaleType>(scaleType) { scale ->
                scaleType = scale
            }
            .thenViewAttribute<ImageView, (() -> Unit)?>(onClick) { clickListener ->
                setOnClickListener { clickListener?.invoke() }
            }
    )
}