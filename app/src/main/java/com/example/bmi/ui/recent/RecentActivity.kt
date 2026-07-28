package com.example.bmi.ui.recent

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bmi.BaseActivity
import com.example.bmi.R
import com.example.bmi.databinding.ActivityRecentBinding
import com.example.bmi.ui.adapt.RecentAdapter
import com.example.bmi.ui.historydetai.HistoryDetailActivity
import com.example.bmi.utils.CommonBanner
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

        setupRecyclerView()
        setupListeners()
        observeState()
        // 触发数据加载
        viewModel.handleIntent(RecentIntent.LoadRecords)
    }

    // ========== 设置 RecyclerView ==========
    private fun setupRecyclerView() {
        adapter = RecentAdapter { record ->
            val intent = Intent(this, HistoryDetailActivity::class.java).apply {
                putExtra("RECORD_ID", record.id)
            }
            startActivity(intent)
        }
        binding.recyclerView.apply {
            //设置列表的排列方式为垂直线性排列
            layoutManager = LinearLayoutManager(this@RecentActivity)
            //把之前创建好的 RecentAdapter（你写的那个充满数据绑定逻辑的适配器）安装到 RecyclerView 上。
            adapter = this@RecentActivity.adapter
        }
    }

    // ========== 设置监听器（点击事件） ==========
    private fun setupListeners() {
        binding.ivBack.setOnClickListener { finish() }
    }

    // ========== 观察状态 ==========
    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    renderState(state)
                }
            }
        }
    }

    // ========== 渲染状态（分离 UI 更新逻辑） ==========
    private fun renderState(state: RecentState) {
        //把最新的数据列表（state.records）交给适配器，让适配器去刷新列表界面。
        adapter.submitList(state.records)
    }


    // ========== 生命周期相关 ==========
    override fun onResume() {
        super.onResume()
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        if (prefs.getBoolean("show_delete_success", false)) {
            prefs.edit().remove("show_delete_success").apply()
            CommonBanner.show(
                this,
                R.drawable.check_circle,
                "Deleted successfully."
            )
        }
    }
}