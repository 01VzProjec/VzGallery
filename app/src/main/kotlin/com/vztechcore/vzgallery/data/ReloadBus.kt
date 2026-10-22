package com.vztechcore.vzgallery.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Global channel to request an immediate "reload" from any screen.
 * Listeners: Home, Albums, Library.
 * Trigger: Settings ("Reler todos os arquivos" button).
 */
object ReloadBus {
    private val _trigger = MutableStateFlow(0L)
    val trigger: StateFlow<Long> = _trigger

    fun request() {
        _trigger.value = _trigger.value + 1
    }
}