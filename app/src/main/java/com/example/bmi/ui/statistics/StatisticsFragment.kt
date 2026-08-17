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
import com.example.bmi.data.enums.ChartMode
import com.example.bmi.databinding.FragmentStatisticsBinding
import com.example.bmi.ui.main.MainActivity
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

        binding.tvBmiUpdate.setOnClickListener {
            (requireActivity() as? MainActivity)?.goToHome()
        }
        binding.tvWeightUpdate.setOnClickListener {
            (requireActivity() as? MainActivity)?.goToHome()
        }

    }

    // ========== 观察状态 ==========
    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.chartView.setMode(state.mode)
                    binding.weightChartView.setMode(state.mode)

                    binding.chartView.setData(state.bmiData)
                    binding.weightChartView.setData(state.weightData)
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

    //移动背景
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

    override fun onResume() {
        super.onResume()
        // 获取当前选中的模式（默认 Day）
        val currentMode = viewModel.uiState.value.mode
        when (currentMode) {
            ChartMode.DAY -> viewModel.dispatch(StatisticsIntent.LoadDay)
            ChartMode.WEEK -> viewModel.dispatch(StatisticsIntent.LoadWeek)
            ChartMode.MONTH -> viewModel.dispatch(StatisticsIntent.LoadMonth)
        }
    }
}