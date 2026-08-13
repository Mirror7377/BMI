package com.example.bmi.ui.result

import android.os.Bundle

sealed class ResultIntent {
    data class Init(val bundle: Bundle?) : ResultIntent()
    data object SaveRecord : ResultIntent()
}