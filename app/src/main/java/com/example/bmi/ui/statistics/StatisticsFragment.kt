package com.example.bmi.ui.statistics

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.bmi.MainActivity
import com.example.bmi.databinding.FragmentStatisticsBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Calendar

@AndroidEntryPoint
class StatisticsFragment : Fragment() {

    private var _binding: FragmentStatisticsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: StatisticsViewModel by viewModels()

    private companion object {
        private const val OFFSET_DAY = 0.0f
        private const val OFFSET_WEEK = 115.0f
        private const val OFFSET_MONTH = 230.0f
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStatisticsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupPeriodSwitcher()

        // 点击 Update 按钮，切换回 HomeFragment
        binding.tvUpdate.setOnClickListener {
            (requireActivity() as? MainActivity)?.goToHome()
        }

        // 获取当前年月，加载数据
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        viewModel.loadMonthData(year, month)

        // 观察数据
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.monthData.collect { data ->
                    if (data.isNotEmpty()) {
                        binding.chartView.setData(data)
                    }
                }
            }
        }

        // 观察加载状态（可选）
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isLoading.collect { loading ->
                    // 可显示加载进度
                }
            }
        }

        // 图表范围变化回调（可选）
        binding.chartView.onDataRangeChanged = { start, end ->
            // 可以更新顶部的日期范围显示
        }
    }

    private fun setupPeriodSwitcher() {
        // 默认选中 Day
        moveBgTo(OFFSET_DAY)

        binding.tvDay.setOnClickListener {
            moveBgTo(OFFSET_DAY)
            // TODO: 后续实现 Day 数据切换
        }
        binding.tvWeek.setOnClickListener {
            moveBgTo(OFFSET_WEEK)
            // TODO: 后续实现 Week 数据切换
        }
        binding.tvMonth.setOnClickListener {
            moveBgTo(OFFSET_MONTH)
            // TODO: 后续实现 Month 数据切换
        }
    }

    private fun moveBgTo(targetMarginStartDp: Float) {
        val bg = binding.selectedPeriodBg
        val params = bg.layoutParams as ConstraintLayout.LayoutParams
        params.marginStart = dpToPx(targetMarginStartDp)
        bg.layoutParams = params
    }

    private fun dpToPx(dp: Float): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}