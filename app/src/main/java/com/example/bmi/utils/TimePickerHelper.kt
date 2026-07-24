package com.example.bmi.utils

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import com.example.bmi.databinding.BottomSheetTimePickerBinding
import com.example.bmi.ui.home.enums.TimeOfDay
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog

class TimePickerHelper(
    private val context: Context,
    private val currentTimeOfDay: TimeOfDay,
    private val onTimeSelected: (TimeOfDay) -> Unit
) {
    private val timeOptions = listOf("Morning", "Afternoon", "Evening", "Night")
    private var selectedIndex = 0

    fun show() {
        val dialog = BottomSheetDialog(context).apply {
            setCancelable(true)
            setCanceledOnTouchOutside(true)
        }
        val binding = BottomSheetTimePickerBinding.inflate(LayoutInflater.from(context))
        dialog.setContentView(binding.root)

        val defaultPos = when (currentTimeOfDay) {
            TimeOfDay.MORNING -> 0
            TimeOfDay.AFTERNOON -> 1
            TimeOfDay.EVENING -> 2
            TimeOfDay.NIGHT -> 3
            else -> 0
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
            state = BottomSheetBehavior.STATE_EXPANDED
            isDraggable = true
            skipCollapsed = true
        }
        dialog.window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)
            setDimAmount(0.5f)
        }
        dialog.show()
    }
}