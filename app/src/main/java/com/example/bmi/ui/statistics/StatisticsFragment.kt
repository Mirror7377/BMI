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

    private enum class ChartMode { DAY, WEEK, MONTH }
    private var currentMode = ChartMode.DAY

    private var currentYear = 0
    private var currentMonth = 0

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

        binding.tvBmiUpdate.setOnClickListener {
            (requireActivity() as? MainActivity)?.goToHome()
        }
        binding.tvWeightUpdate.setOnClickListener {
            (requireActivity() as? MainActivity)?.goToHome()
        }

        val calendar = Calendar.getInstance()
        currentYear = calendar.get(Calendar.YEAR)
        currentMonth = calendar.get(Calendar.MONTH)

        // 默认加载 Day 数据
        viewModel.loadMonthData(currentYear, currentMonth)
        viewModel.loadWeightMonthData(currentYear, currentMonth)

        // ========== Flow 观察者（保留，用于数据库更新后自动刷新） ==========
        // BMI Day 数据
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.monthData.collect { data ->
                    if (currentMode == ChartMode.DAY && data.isNotEmpty()) {
                        binding.chartView.setMode(BmiChartView.ChartMode.DAY)
                        binding.chartView.setData(data)
                    }
                }
            }
        }

        // Weight Day 数据
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.weightData.collect { data ->
                    if (currentMode == ChartMode.DAY && data.isNotEmpty()) {
                        binding.weightChartView.setMode(WeightChartView.ChartMode.DAY)
                        binding.weightChartView.setData(data)
                    }
                }
            }
        }

        // BMI Week 数据
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.weekBmiData.collect { data ->
                    if (currentMode == ChartMode.WEEK && data.isNotEmpty()) {
                        binding.chartView.setMode(BmiChartView.ChartMode.WEEK)
                        binding.chartView.setData(data)
                    }
                }
            }
        }

        // Weight Week 数据
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.weekWeightData.collect { data ->
                    if (currentMode == ChartMode.WEEK && data.isNotEmpty()) {
                        binding.weightChartView.setMode(WeightChartView.ChartMode.WEEK)
                        binding.weightChartView.setData(data)
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.monthBmiData.collect { data ->
                    if (currentMode == ChartMode.MONTH && data.isNotEmpty()) {
                        binding.chartView.setMode(BmiChartView.ChartMode.MONTH)
                        binding.chartView.setData(data)
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.monthWeightData.collect { data ->
                    if (currentMode == ChartMode.MONTH && data.isNotEmpty()) {
                        binding.weightChartView.setMode(WeightChartView.ChartMode.MONTH)
                        binding.weightChartView.setData(data)
                    }
                }
            }
        }
    }

    private fun setupPeriodSwitcher() {
        moveBgTo(OFFSET_DAY)

        binding.tvDay.setOnClickListener {
            moveBgTo(OFFSET_DAY)
            currentMode = ChartMode.DAY

            // 1. 立即用缓存数据刷新图表
            binding.chartView.setMode(BmiChartView.ChartMode.DAY)
            binding.chartView.setData(viewModel.getCurrentDayBmiData())

            binding.weightChartView.setMode(WeightChartView.ChartMode.DAY)
            binding.weightChartView.setData(viewModel.getCurrentDayWeightData())

            // 2. 后台重新加载最新数据（完成后 Flow 会自动再次刷新）
            viewModel.loadMonthData(currentYear, currentMonth)
            viewModel.loadWeightMonthData(currentYear, currentMonth)
        }

        binding.tvWeek.setOnClickListener {
            moveBgTo(OFFSET_WEEK)
            currentMode = ChartMode.WEEK

            // 1. 立即用缓存数据刷新图表
            binding.chartView.setMode(BmiChartView.ChartMode.WEEK)
            binding.chartView.setData(viewModel.getCurrentWeekBmiData())

            binding.weightChartView.setMode(WeightChartView.ChartMode.WEEK)
            binding.weightChartView.setData(viewModel.getCurrentWeekWeightData())

            // 2. 后台重新加载最新数据（完成后 Flow 会自动再次刷新）
            viewModel.loadWeekData()
        }

        binding.tvMonth.setOnClickListener {
            moveBgTo(OFFSET_MONTH)
            currentMode = ChartMode.MONTH

            // BMI Month
            binding.chartView.setMode(BmiChartView.ChartMode.MONTH)
            binding.chartView.setData(viewModel.getCurrentMonthBmiData())

            // Weight Month
            binding.weightChartView.setMode(WeightChartView.ChartMode.MONTH)
            binding.weightChartView.setData(viewModel.getCurrentMonthWeightData())

            // 后台加载最新数据
            viewModel.loadMonthStatistics()
            viewModel.loadMonthWeightStatistics()
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