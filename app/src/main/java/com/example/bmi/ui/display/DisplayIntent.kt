package com.example.bmi.ui.display

import android.os.Bundle

sealed class DisplayIntent {
    data class LoadFromArguments(val args: Bundle) : DisplayIntent()
    object LoadLatestRecord : DisplayIntent()
    object SaveRecord : DisplayIntent()
    object Discard : DisplayIntent()
    object BackPressed : DisplayIntent()
}