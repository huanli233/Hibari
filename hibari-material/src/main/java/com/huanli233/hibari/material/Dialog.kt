package com.huanli233.hibari.material

import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.huanli233.hibari.foundation.Node
import com.huanli233.hibari.runtime.Tunable
import com.huanli233.hibari.ui.Modifier
import com.huanli233.hibari.ui.viewClass

@Tunable
fun AlertDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    text: String? = null,
    confirmButton: @Tunable () -> Unit = {},
    dismissButton: @Tunable () -> Unit = {}
) {
    // Note: Dialog implementation would require special handling
    // This is a placeholder for the dialog structure
    Node(
        modifier = modifier,
        content = {
            confirmButton()
            dismissButton()
        }
    )
}