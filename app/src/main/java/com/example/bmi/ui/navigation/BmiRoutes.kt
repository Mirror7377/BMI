package com.example.bmi.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
object SplashRoute

@Serializable
object HomeRoute

@Serializable
object DisplayRoute

@Serializable
object StatisticsRoute

@Serializable
data class ResultRoute(val recordJson: String)

@Serializable
object RecentRoute

@Serializable
data class HistoryDetailRoute(val recordId: Long)

@Serializable
object ProfileRoute

@Serializable
object FeedbackRoute

@Serializable
object LanguageRoute