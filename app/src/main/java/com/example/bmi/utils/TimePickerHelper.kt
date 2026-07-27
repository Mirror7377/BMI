package com.example.bmi.utils

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import com.example.bmi.R
import com.example.bmi.databinding.BottomSheetTimePickerBinding
import com.example.bmi.ui.home.enums.TimeOfDay
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog

class TimePickerHelper(
    private val context: Context,
    private val currentTimeOfDay: TimeOfDay,
    private val onTimeSelected: (TimeOfDay) -> Unit
) {
    private var selectedIndex = 0

    fun show() {
        val dialog = BottomSheetDialog(
            context,
            R.style.Theme_BMI_BottomSheetDialog
        ).apply {
            setCanceledOnTouchOutside(true)
        }
        val binding = BottomSheetTimePickerBinding.inflate(LayoutInflater.from(context))
        dialog.setContentView(binding.root)

        // 动态构建本地化选项列表
        val timeOptions = listOf(
            context.getString(R.string.time_morning),
            context.getString(R.string.time_afternoon),
            context.getString(R.string.time_evening),
            context.getString(R.string.time_night)
        )


        val defaultPos = when (currentTimeOfDay) {
            TimeOfDay.MORNING -> 0
            TimeOfDay.AFTERNOON -> 1
            TimeOfDay.EVENING -> 2
            TimeOfDay.NIGHT -> 3
        }
        selectedIndex = defaultPos

        WheelPickerHelper.setupWheelPicker(
            recyclerView = binding.rvTimePicker,
            context = context,
            data = timeOptions,
            defaultPosition = defaultPos,
            onItemSelected = { pos -> selectedIndex = pos },
            isTimePicker = true
        )

        binding.btnTimeCancel.setOnClickListener { dialog.dismiss() }
        binding.btnTimeDone.setOnClickListener {
            val timeOfDay = when (selectedIndex) {
                0 -> TimeOfDay.MORNING
                1 -> TimeOfDay.AFTERNOON
                2 -> TimeOfDay.EVENING
                3 -> TimeOfDay.NIGHT
                else -> TimeOfDay.MORNING
            }
            onTimeSelected(timeOfDay)
            dialog.dismiss()
        }

        val behavior = BottomSheetBehavior.from(binding.root.parent as View)
        behavior.apply {
            peekHeight = 0
            //将弹窗状态强制设为“完全展开”
            state = BottomSheetBehavior.STATE_EXPANDED
            //跳过“折叠态
            skipCollapsed = true
        }
        dialog.show()
    }
}