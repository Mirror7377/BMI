package com.example.bmi.utils

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

@Singleton
class AppEventBus @Inject constructor() {

    private val _bannerEvent = Channel<BannerData>(capacity = 1)

    val bannerEvent = _bannerEvent.receiveAsFlow()

    fun showBanner(iconRes: Int, message: String) {
        _bannerEvent.trySend(BannerData(iconRes, message))
    }

    data class BannerData(val iconRes: Int, val message: String)
}
