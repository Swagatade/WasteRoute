package com.wasteroute.app.components

import androidx.compose.runtime.Composable

/**
 * Obsolete proxy component. Real Google Sign-In is handled natively via Android Credential Manager.
 */
@Deprecated("Real Google Sign-In is handled natively via Android Credential Manager.")
@Suppress("FunctionName")
@Composable
fun GoogleAccountChooserSheet(
    isOpen: Boolean = false,
    onDismissRequest: () -> Unit = {},
    onAccountSelected: ((Any) -> Unit)? = null
) {
    // Proxy UI removed. Firebase Authentication & Android Credential Manager are used directly.
}
