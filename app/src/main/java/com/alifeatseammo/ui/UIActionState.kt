package com.alifeatseammo.ui

sealed class UIActionState {
    object Idle : UIActionState()
    data class Loading(val label: String) : UIActionState()
    data class Success(val label: String) : UIActionState()
    data class Error(val message: String) : UIActionState()
}
