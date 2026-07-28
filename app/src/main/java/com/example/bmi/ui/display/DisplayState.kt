// DisplayContract.kt（或直接写在 ViewModel 文件顶部）
package com.example.bmi.ui.display

import com.example.bmi.data.database.BmiRecord

data class DisplayState(
    val record: BmiRecord? = null
)



