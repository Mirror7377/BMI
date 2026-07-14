package com.example.bmi.ui.adapt

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.bmi.R

// 滚轮显示类型
enum class DatePickerType {
    NUMBER,     // 数字（日、年）
    MONTH_TEXT  // 英文月份
}

sealed class DatePickerItem {
    object Placeholder : DatePickerItem()
    data class RealValue(val value: Int) : DatePickerItem()
}

class DatePickerAdapter(
    private var items: List<DatePickerItem>,
    private val itemWidthDp: Int,
    private val type: DatePickerType = DatePickerType.NUMBER // 新增类型参数
) : RecyclerView.Adapter<DatePickerAdapter.DatePickerViewHolder>() {

    // 英文月份缩写（固定英文，不受系统语言影响）
    private val monthNames = arrayOf(
        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    )

    inner class DatePickerViewHolder(val textView: TextView) : RecyclerView.ViewHolder(textView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DatePickerViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_date_picker, parent, false)
        val tv = itemView as TextView
        return DatePickerViewHolder(tv)
    }

    override fun onBindViewHolder(holder: DatePickerViewHolder, position: Int) {
        when (val item = items[position]) {
            is DatePickerItem.Placeholder -> {
                holder.textView.text = ""
            }
            is DatePickerItem.RealValue -> {
                holder.textView.text = when (type) {
                    DatePickerType.MONTH_TEXT -> monthNames[item.value - 1]
                    DatePickerType.NUMBER -> item.value.toString()
                }
            }
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newItems: List<DatePickerItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}