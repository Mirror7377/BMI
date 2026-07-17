package com.example.bmi.ui.bmigauge


import com.example.bmi.ui.home.enums.Gender

object BmiConfigProvider {

    // -------- 成年配置 --------
    private val adultConfig = BmiGaugeConfig(
        min = 15.6f,
        max = 40.3f,
        splitPoints = listOf(16.0f, 17.0f, 18.5f, 25.0f, 30.0f, 35.0f, 40.0f),
        colors = listOf(
            0xFF286DE6.toInt(),  // Very Severely Underweight
            0xFF349CEA.toInt(),  // Severely Underweight
            0xFF5BB1F5.toInt(),  // Underweight
            0xFFA8C526.toInt(),  // Normal
            0xFFFECD2E.toInt(),  // Overweight
            0xFFFD9845.toInt(),  // Obese Class I
            0xFFF67D3C.toInt(),  // Obese Class II
            0xFFF04E46.toInt()   // Obese Class III
        ),
        labels = listOf(17.0f, 18.5f, 25.0f, 30.0f, 35.0f, 40.0f)
    )

    // -------- 儿童数据（2~20岁，括号内范围优先） --------
    private val childRangeMap = mapOf(
        // 女童
        Pair(2, Gender.FEMALE) to 14.0f..20.0f,   // 括号内 (14~20)
        Pair(3, Gender.FEMALE) to 13.0f..19.0f,
        Pair(4, Gender.FEMALE) to 13.0f..19.0f,
        Pair(5, Gender.FEMALE) to 13.0f..19.0f,
        Pair(6, Gender.FEMALE) to 13.0f..20.0f,
        Pair(7, Gender.FEMALE) to 13.0f..21.0f,
        Pair(8, Gender.FEMALE) to 13.0f..22.0f,
        Pair(9, Gender.FEMALE) to 13.0f..23.0f,
        Pair(10, Gender.FEMALE) to 13.0f..24.0f,
        Pair(11, Gender.FEMALE) to 14.0f..26.0f,
        Pair(12, Gender.FEMALE) to 14.0f..26.0f,
        Pair(13, Gender.FEMALE) to 14.0f..27.0f,  // 括号内 (14~27)
        Pair(14, Gender.FEMALE) to 15.0f..28.0f,
        Pair(15, Gender.FEMALE) to 15.0f..29.0f,  // 括号内 (15~29)
        Pair(16, Gender.FEMALE) to 16.0f..30.0f,
        Pair(17, Gender.FEMALE) to 16.0f..31.0f,
        Pair(18, Gender.FEMALE) to 16.0f..32.0f,  // 括号内 (16~32)
        Pair(19, Gender.FEMALE) to 17.0f..32.0f,
        Pair(20, Gender.FEMALE) to 17.0f..33.0f,
        // 男童
        Pair(2, Gender.MALE) to 14.0f..20.0f,    // 括号内 (14~20)
        Pair(3, Gender.MALE) to 13.0f..19.0f,
        Pair(4, Gender.MALE) to 13.0f..19.0f,
        Pair(5, Gender.MALE) to 13.0f..19.0f,
        Pair(6, Gender.MALE) to 13.0f..20.0f,
        Pair(7, Gender.MALE) to 13.0f..20.0f,
        Pair(8, Gender.MALE) to 13.0f..21.0f,
        Pair(9, Gender.MALE) to 13.0f..22.0f,
        Pair(10, Gender.MALE) to 13.0f..23.0f,
        Pair(11, Gender.MALE) to 13.0f..24.0f,
        Pair(12, Gender.MALE) to 14.0f..25.0f,
        Pair(13, Gender.MALE) to 14.0f..26.0f,
        Pair(14, Gender.MALE) to 15.0f..27.0f,
        Pair(15, Gender.MALE) to 15.0f..28.0f,
        Pair(16, Gender.MALE) to 16.0f..29.0f,
        Pair(17, Gender.MALE) to 16.0f..29.0f,  // 括号内 (16~29)
        Pair(18, Gender.MALE) to 17.0f..30.0f,
        Pair(19, Gender.MALE) to 17.0f..31.0f,
        Pair(20, Gender.MALE) to 17.0f..32.0f
    )

    // 儿童分界值：Triple(underweight上限, normal上限, overweight上限)
    private val childSplitMap = mapOf(
        // 女童
        2 to Triple(14.4f, 18.0f, 19.1f),
        3 to Triple(14.0f, 17.3f, 18.3f), // 原文 "17.3" （修正）
        4 to Triple(13.7f, 16.8f, 18.0f),
        5 to Triple(13.5f, 16.8f, 18.3f),
        6 to Triple(13.4f, 17.2f, 18.8f),
        7 to Triple(13.5f, 17.6f, 19.6f),
        8 to Triple(13.6f, 18.4f, 20.6f),
        9 to Triple(13.8f, 19.2f, 21.8f),
        10 to Triple(14.0f, 20.0f, 23.0f),
        11 to Triple(14.8f, 21.7f, 25.2f),
        12 to Triple(14.8f, 21.7f, 25.2f),
        13 to Triple(15.4f, 22.6f, 26.4f),
        14 to Triple(15.8f, 23.4f, 27.2f),
        15 to Triple(16.4f, 24.1f, 28.1f),
        16 to Triple(16.8f, 24.6f, 28.9f),
        17 to Triple(17.2f, 25.2f, 29.6f),
        18 to Triple(17.6f, 25.6f, 30.4f),
        19 to Triple(17.8f, 26.2f, 31.0f),
        20 to Triple(17.9f, 26.5f, 31.7f),
        // 男童
        2 to Triple(14.8f, 18.2f, 19.3f),
        3 to Triple(14.4f, 17.4f, 18.3f),
        4 to Triple(14.0f, 16.9f, 18.0f),
        5 to Triple(13.8f, 16.8f, 18.1f),
        6 to Triple(13.7f, 17.0f, 18.6f),
        7 to Triple(13.6f, 17.4f, 19.2f),
        8 to Triple(13.7f, 17.8f, 20.0f),
        9 to Triple(14.0f, 18.6f, 21.1f),
        10 to Triple(14.2f, 19.4f, 22.2f),
        11 to Triple(14.5f, 20.0f, 23.2f),
        12 to Triple(15.0f, 21.0f, 24.2f),
        13 to Triple(15.5f, 21.7f, 25.4f),
        14 to Triple(16.0f, 22.6f, 26.0f),
        15 to Triple(16.5f, 23.5f, 26.8f),
        16 to Triple(17.1f, 24.2f, 27.7f),
        17 to Triple(17.6f, 24.8f, 28.3f),
        18 to Triple(18.3f, 25.6f, 29.0f),
        19 to Triple(18.5f, 26.4f, 29.8f),
        20 to Triple(18.5f, 27.2f, 30.7f)
    )

    /**
     * 获取对应年龄性别的配置
     * @param age 年龄（≥2）
     * @param gender 字符串 "MALE" 或 "FEMALE"
     */
    fun getConfig(age: Int, gender: String): BmiGaugeConfig {
        val genderEnum = if (gender == Gender.MALE.name) Gender.MALE else Gender.FEMALE
        return if (age > 20) {
            adultConfig
        } else {
            val range = childRangeMap[Pair(age, genderEnum)]
                ?: error("No range data for age=$age, gender=$gender")
            val splits = childSplitMap[age]?.let { triple ->
                listOf(triple.first, triple.second, triple.third)
            } ?: error("No split data for age=$age")
            // 儿童颜色：Underweight, Normal, Overweight, Obese I
            val colors = listOf(
                0xFF5BB1F5.toInt(), // Underweight
                0xFFA8C526.toInt(), // Normal
                0xFFFECD2E.toInt(), // Overweight
                0xFFFD9845.toInt()  // Obese I
            )
            BmiGaugeConfig(
                min = range.start,
                max = range.endInclusive,
                splitPoints = splits,
                colors = colors,
                labels = splits
            )
        }
    }

    /**
     * 儿童BMI等级分类（返回 BmiLevel）
     */
    fun classifyChild(age: Int, gender: String, bmi: Double): BmiLevel {
        require(age in 2..20) { "儿童年龄必须在2~20之间" }
        val genderEnum = if (gender == Gender.MALE.name) Gender.MALE else Gender.FEMALE
        val splits = childSplitMap[age] ?: return BmiLevel.UNDERWEIGHT
        val (underweightMax, normalMax, overweightMax) = splits
        return when {
            bmi < underweightMax -> BmiLevel.UNDERWEIGHT
            bmi < normalMax -> BmiLevel.NORMAL
            bmi < overweightMax -> BmiLevel.OVERWEIGHT
            else -> BmiLevel.OBESE_CLASS_I
        }
    }
}