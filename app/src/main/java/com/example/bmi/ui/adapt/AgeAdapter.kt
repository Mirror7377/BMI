package com.example.bmi.ui.adapt

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.bmi.databinding.ItemAgeBinding

class AgeAdapter(
    private val ages: List<Int>,
    private val onAgeClicked: (Int) -> Unit
) : RecyclerView.Adapter<AgeAdapter.AgeViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AgeViewHolder {
        val binding = ItemAgeBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AgeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AgeViewHolder, position: Int) {
        val age = ages[position]

        holder.binding.tvAgeItem.text = age.toString()

        holder.itemView.setOnClickListener {
            onAgeClicked(age)
        }
    }

    override fun getItemCount(): Int = ages.size

    class AgeViewHolder(
        val binding: ItemAgeBinding
    ) : RecyclerView.ViewHolder(binding.root)
}