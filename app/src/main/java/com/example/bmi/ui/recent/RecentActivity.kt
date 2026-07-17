package com.example.bmi.ui.recent

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bmi.BaseActivity
import com.example.bmi.databinding.ActivityRecentBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class RecentActivity : BaseActivity() {

    private lateinit var binding: ActivityRecentBinding
    private val viewModel: RecentViewModel by viewModels()
    private lateinit var adapter: RecentAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // RecyclerView
        adapter = RecentAdapter { /* 预留点击回调 */ }
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@RecentActivity)
            adapter = this@RecentActivity.adapter
        }

        // 观察数据
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    adapter.submitList(state.records)
                    binding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE
                }
            }
        }

        // 加载数据
        viewModel.handleIntent(RecentIntent.LoadRecords)

        // 返回
        binding.ivBack.setOnClickListener { finish() }
    }
}