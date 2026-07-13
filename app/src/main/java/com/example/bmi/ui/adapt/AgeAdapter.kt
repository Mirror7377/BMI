package com.example.bmi.ui.adapt

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.bmi.databinding.ItemAgeBinding
import com.example.bmi.R
import com.example.bmi.ui.home.AgeItem

class AgeAdapter(
    val items: List<AgeItem>,   // 公开，便于外部获取
    private val onAgeClicked: (Int) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_REAL = 0
        private const val TYPE_PLACEHOLDER = 1
    }

    override fun getItemViewType(position: Int) =
        if (items[position] is AgeItem.RealAge) TYPE_REAL else TYPE_PLACEHOLDER

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_REAL -> {
                val binding = ItemAgeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                RealViewHolder(binding)
            }
            TYPE_PLACEHOLDER -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_age_placeholder, parent, false)
                PlaceholderViewHolder(view)
            }
            else -> throw IllegalArgumentException()
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is RealViewHolder) {
            val age = (items[position] as AgeItem.RealAge).age
            holder.binding.tvAgeItem.text = age.toString()
            holder.itemView.setOnClickListener { onAgeClicked(age) }
        }
    }

    override fun getItemCount() = items.size

    inner class RealViewHolder(val binding: ItemAgeBinding) : RecyclerView.ViewHolder(binding.root)
    inner class PlaceholderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}