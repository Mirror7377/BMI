package com.example.bmi.ui.bmigauge


data class BmiGaugeConfig(
    val min: Float,
    val max: Float,
    val splitPoints: List<Float>,  // 分界点，长度为 分段数-1
    val colors: List<Int>,         // 颜色列表，长度 = 分段数
    val labels: List<Float>        // 需要显示刻度的数值
) {
    init {
        require(splitPoints.size + 1 == colors.size) { "分段数必须匹配" }
        require(splitPoints.all { it in min..max }) { "分界点必须在范围内" }
        require(labels.all { it in min..max }) { "标签必须在范围内" }
    }
}