package com.example.bmi.utils

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import com.example.bmi.databinding.BottomSheetDatePickerBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.util.Calendar

class DatePickerHelper(
    private val context: Context,
    private val currentTimestamp: Long,
    private val onDateSelected: (Long) -> Unit
) {
    private var selectedYear = 0
    private var selectedMonth = 0
    private var selectedDay = 0

    private val nowCalendar = Calendar.getInstance()
    private val currentYear = nowCalendar.get(Calendar.YEAR)
    private val currentMonth = nowCalendar.get(Calendar.MONTH) + 1
    private val currentDay = nowCalendar.get(Calendar.DAY_OF_MONTH)

    fun show() {
        val dialog = BottomSheetDialog(context).apply {
            setCancelable(true)
            setCanceledOnTouchOutside(true)
        }
        val binding = BottomSheetDatePickerBinding.inflate(LayoutInflater.from(context))
        dialog.setContentView(binding.root)

        val calendar = Calendar.getInstance().apply {
            timeInMillis = currentTimestamp.coerceAtMost(System.currentTimeMillis())
        }
        val initYear = calendar.get(Calendar.YEAR)
        val initMonth = calendar.get(Calendar.MONTH)
        val initDay = calendar.get(Calendar.DAY_OF_MONTH) - 1

        // 年份
        val years = (1900..currentYear).map { it.toString() }
        val yearPos = initYear - 1900
        selectedYear = yearPos

        WheelPickerHelper.setupWheelPicker(
            recyclerView = binding.rvYear,
            context = context,
            data = years,
            defaultPosition = yearPos,
            onItemSelected = { pos ->
                selectedYear = pos
                updateMonthAndDay(binding, selectedYear + 1900)
            },
            isTimePicker = false
        )

        // 月份和日期（先初始化，再更新）
        updateMonthAndDay(binding, initYear, initMonth, initDay)

        binding.btnDateCancel.setOnClickListener { dialog.dismiss() }
        binding.btnDateDone.setOnClickListener {
            val year = selectedYear + 1900
            val month = selectedMonth + 1
            val day = selectedDay + 1
            val cal = Calendar.getInstance().apply { set(year, month - 1, day) }
            onDateSelected(cal.timeInMillis)
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

    private fun updateMonthAndDay(binding: BottomSheetDatePickerBinding, year: Int, initMonth: Int? = null, initDay: Int? = null) {
        val monthNames = getMonthNamesForYear(year)
        val monthPos = initMonth?.coerceAtMost(monthNames.size - 1) ?: selectedMonth.coerceAtMost(monthNames.size - 1)
        selectedMonth = monthPos

        // 如果月份滚轮未配置，则先配置
        if (binding.rvMonth.adapter == null) {
            WheelPickerHelper.setupWheelPicker(
                recyclerView = binding.rvMonth,
                context = context,
                data = monthNames,
                defaultPosition = monthPos,
                onItemSelected = { pos ->
                    selectedMonth = pos
                    updateDayPicker(binding, selectedYear + 1900, selectedMonth + 1)
                },
                isTimePicker = false
            )
        } else {
            (binding.rvMonth.adapter as? WheelPickerHelper.WheelAdapter)?.updateData(monthNames)
            binding.rvMonth.layoutManager?.scrollToPosition(monthPos)
        }

        // 日期
        val dayNames = getDaysForMonth(year, selectedMonth + 1)
        val dayPos = initDay?.coerceAtMost(dayNames.size - 1) ?: selectedDay.coerceAtMost(dayNames.size - 1)
        selectedDay = dayPos
        if (binding.rvDay.adapter == null) {
            WheelPickerHelper.setupWheelPicker(
                recyclerView = binding.rvDay,
                context = context,
                data = dayNames,
                defaultPosition = dayPos,
                onItemSelected = { pos -> selectedDay = pos },
                isTimePicker = false
            )
        } else {
            (binding.rvDay.adapter as? WheelPickerHelper.WheelAdapter)?.updateData(dayNames)
            binding.rvDay.layoutManager?.scrollToPosition(dayPos)
        }

        // 刷新效果
        binding.rvMonth.post { WheelPickerHelper.updateWheelEffects(binding.rvMonth) }
        binding.rvDay.post { WheelPickerHelper.updateWheelEffects(binding.rvDay) }
    }

    private fun updateDayPicker(binding: BottomSheetDatePickerBinding, year: Int, month: Int) {
        val dayNames = getDaysForMonth(year, month)
        val pos = selectedDay.coerceAtMost(dayNames.size - 1)
        selectedDay = pos
        (binding.rvDay.adapter as? WheelPickerHelper.WheelAdapter)?.updateData(dayNames)
        binding.rvDay.layoutManager?.scrollToPosition(pos)
        binding.rvDay.post { WheelPickerHelper.updateWheelEffects(binding.rvDay) }
    }

    private fun getMonthNamesForYear(year: Int): List<String> {
        val all = listOf("Jan", "Feb", "Mar", "Apr", "May", "June", "July", "Aug", "Sep", "Oct", "Nov", "Dec")
        return if (year == currentYear) all.subList(0, currentMonth) else all
    }

    private fun getDaysForMonth(year: Int, month: Int): List<String> {
        val cal = Calendar.getInstance().apply { set(year, month - 1, 1) }
        val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val limit = if (year == currentYear && month == currentMonth) currentDay else maxDay
        return (1..limit).map { String.format("%02d", it) }
    }
}