package com.huanli233.hibari.material

import com.google.android.material.snackbar.Snackbar
import com.huanli233.hibari.foundation.Node
import com.huanli233.hibari.foundation.attributes.text
import com.huanli233.hibari.runtime.Tunable
import com.huanli233.hibari.ui.Modifier
import com.huanli233.hibari.ui.viewClass

@Tunable
fun Snackbar(
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    // Note: Snackbar implementation would require special handling
    // This is a placeholder for the snackbar structure
    Node(
        modifier = modifier
            .text(message)
    )
}