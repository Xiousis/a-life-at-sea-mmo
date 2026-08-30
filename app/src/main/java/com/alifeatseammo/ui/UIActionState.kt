package com.alifeatseammo.ui

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

sealed class UIActionState {
    object Idle : UIActionState()
    data class Loading(val label: String) : UIActionState()
    data class Success(val label: String) : UIActionState()
    data class Error(val message: String) : UIActionState()
}

fun CoroutineScope.launchUIAction(
    label: String,
    actionState: MutableStateFlow<UIActionState>,
    errorState: MutableStateFlow<String?>? = null,
    onError: ((Exception) -> String)? = null,
    block: suspend () -> Unit
) {
    if (actionState.value is UIActionState.Loading) return

    launch {
        actionState.value = UIActionState.Loading(label)
        try {
            block()
            actionState.value = UIActionState.Success(label)
            kotlinx.coroutines.delay(2000)
            if (actionState.value is UIActionState.Success && (actionState.value as UIActionState.Success).label == label) {
                actionState.value = UIActionState.Idle
            }
        } catch (e: Exception) {
            val message = onError?.invoke(e) ?: e.message ?: "Action failed"
            actionState.value = UIActionState.Error(message)
            errorState?.value = message
        }
    }
}
