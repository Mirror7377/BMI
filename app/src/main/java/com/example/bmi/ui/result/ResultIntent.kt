package com.example.bmi.ui.result


import android.os.Bundle


sealed class ResultIntent {
    data class LoadFromArguments(val args: Bundle) : ResultIntent()
    object LoadLatestRecord : ResultIntent()
    object SaveRecord : ResultIntent()
    object Discard : ResultIntent()
    object BackPressed : ResultIntent()
}