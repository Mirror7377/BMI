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
        observeUiState()

        // 保留原有的更新按钮点击事件
        binding.tvBmiUpdate.setOnClickListener {
            (requireActivity() as? MainActivity)?.goToHome()
        }
        binding.tvWeightUpdate.setOnClickListener {
            (requireActivity() as? MainActivity)?.goToHome()
        }

        // 保留原有的日期范围回调（如果需要在 Fragment 中处理，可设置）
        // 原代码中并未使用该回调做 UI 显示，所以不处理也可以
        // 若需要，可在这里设置 binding.chartView.onDataRangeChanged = { ... }
    }

    // ========== 观察状态 ==========
    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    // 设置模式（View 原有方法）
                    binding.chartView.setMode(
                        when (state.mode) {
                            ChartMode.DAY -> BmiChartView.ChartMode.DAY
                            ChartMode.WEEK -> BmiChartView.ChartMode.WEEK
                            ChartMode.MONTH -> BmiChartView.ChartMode.MONTH
                        }
                    )
                    binding.weightChartView.setMode(
                        when (state.mode) {
                            ChartMode.DAY -> WeightChartView.ChartMode.DAY
                            ChartMode.WEEK -> WeightChartView.ChartMode.WEEK
                            ChartMode.MONTH -> WeightChartView.ChartMode.MONTH
                        }
                    )

                    // 设置数据（View 原有方法）
                    binding.chartView.setData(state.bmiData)
                    binding.weightChartView.setData(state.weightData)

                    // 加载状态（可显示进度条，根据需求）
                    // 例如：binding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE
                }
            }
        }
    }

    // ========== 模式切换 ==========
    private fun setupPeriodSwitcher() {
        moveBgTo(OFFSET_DAY)

        binding.tvDay.setOnClickListener {
            moveBgTo(OFFSET_DAY)
            viewModel.dispatch(StatisticsIntent.LoadDay)
        }

        binding.tvWeek.setOnClickListener {
            moveBgTo(OFFSET_WEEK)
            viewModel.dispatch(StatisticsIntent.LoadWeek)
        }

        binding.tvMonth.setOnClickListener {
            moveBgTo(OFFSET_MONTH)
            viewModel.dispatch(StatisticsIntent.LoadMonth)
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