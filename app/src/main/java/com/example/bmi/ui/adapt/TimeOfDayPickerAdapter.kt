package com.example.bmi.ui.adapt


import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.bmi.R

sealed class TimePickerItem {
    object Placeholder : TimePickerItem()
    data class RealValue(val text: String) : TimePickerItem()
}

class TimeOfDayPickerAdapter(
    private var items: List<TimePickerItem>
) : RecyclerView.Adapter<TimeOfDayPickerAdapter.TimePickerViewHolder>() {

    inner class TimePickerViewHolder(val textView: TextView) : RecyclerView.ViewHolder(textView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TimePickerViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_time_picker, parent, false) as TextView
        return TimePickerViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: TimePickerViewHolder, position: Int) {
        when (val item = items[position]) {
            is TimePickerItem.Placeholder -> holder.textView.text = ""
            is TimePickerItem.RealValue -> holder.textView.text = item.text
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newItems: List<TimePickerItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}