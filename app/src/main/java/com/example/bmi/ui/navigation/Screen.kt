package com.example.bmi.ui.navigation

//todo 重构路由
sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Main : Screen("main")
    object Result : Screen("result")
    object Recent : Screen("recent")
    object HistoryDetail : Screen("history")
    object Profile : Screen("profile")
    object Language : Screen("language")
    object Feedback : Screen("feedback")

    // 带参数的导航
    object HistoryDetailWithId : Screen("history/{recordId}") {
        fun pass(recordId: Long): String = "history/$recordId"
    }

    object ResultWithRecord : Screen("result/{recordId}") {
        fun pass(recordId: Long): String = "result/$recordId"
    }
}