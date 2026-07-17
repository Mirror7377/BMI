package com.example.bmi.ui.recent

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.bmi.data.database.BmiRecord
import com.example.bmi.databinding.ItemRecentRecordBinding
import com.example.bmi.ui.bmigauge.BmiClassifier
import java.text.SimpleDateFormat
import java.util.Locale

class RecentAdapter(
    private val onItemClick: (BmiRecord) -> Unit
) : ListAdapter<BmiRecord, RecentAdapter.ViewHolder>(DiffCallback()) {

    companion object {
        private fun dpToPx(dp: Int, density: Float): Int {
            return (dp * density + 0.5f).toInt()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRecentRecordBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding, onItemClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        private val binding: ItemRecentRecordBinding,
        private val onItemClick: (BmiRecord) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(record: BmiRecord) {
            //  BMI 数值
            binding.tvBmiValue.text = String.format("%.1f", record.bmi)

            // 在 RecentAdapter 的 bind 方法中
            val bmiLevel = if (record.age > 20) {
                BmiClassifier.classifyAdult(record.bmi)
            } else {
                BmiClassifier.classifyChild(record.age, record.gender, record.bmi)
            }
            binding.tvLevelName.text = bmiLevel.statusText

            // 3. 彩色圆点（纯代码动态创建）
            val density = binding.root.context.resources.displayMetrics.density
            val dotDrawable = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(bmiLevel.cardBgColor)
                setSize(
                    dpToPx(18, density),
                    dpToPx(18, density)
                )
            }
            binding.dotLevel.background = dotDrawable

            // 日期 + 时段
            val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
            val dateStr = dateFormat.format(record.timestamp)
            // 从 record.timeOfDay 获取时段（Morning/Afternoon/Evening）
            val timeStr = record.timeOfDay
            binding.tvDateTime.text = "$dateStr\n$timeStr"

            // 5. 点击事件
            binding.root.setOnClickListener {
                onItemClick(record)
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<BmiRecord>() {
        override fun areItemsTheSame(oldItem: BmiRecord, newItem: BmiRecord): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: BmiRecord, newItem: BmiRecord): Boolean =
            oldItem == newItem
    }

}