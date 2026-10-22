package com.vztechcore.vzgallery.data

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Snackbar message with optional action.
 * If `actionLabel` is null, it's just an informational message.
 */
data class SnackbarMessage(
    val text: String,
    val actionLabel: String? = null,
    val onAction: (() -> Unit)? = null
)

object SnackbarBus {
    private val _messages = MutableSharedFlow<SnackbarMessage>(extraBufferCapacity = 8)
    val messages = _messages.asSharedFlow()

    /** Simple snackbar. */
    fun show(text: String) {
        _messages.tryEmit(SnackbarMessage(text))
    }

    /** Snackbar with action button (e.g. "Undo"). */
    fun showWithAction(text: String, actionLabel: String, onAction: () -> Unit) {
        _messages.tryEmit(SnackbarMessage(text, actionLabel, onAction))
    }
}