package com.example.bmi

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.bmi.data.database.BmiRecord
import com.example.bmi.data.repository.BmiRepository
import com.example.bmi.databinding.ActivityMainBinding
import com.example.bmi.ui.display.DisplayFragment
import com.example.bmi.ui.home.HomeFragment
import com.example.bmi.ui.statistics.StatisticsFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject

// 新增：底部导航控制接口，供Fragment调用解耦
interface BottomNavController {
    fun forceHideBottomNav()
    fun restoreBottomNavAuto()
}

@AndroidEntryPoint
class MainActivity : BaseActivity(), BottomNavController {

    @Inject
    lateinit var repository: BmiRepository

    private lateinit var binding: ActivityMainBinding

    // 自动控制开关：true=根据数据库自动显隐；false=强制手动控制
    private var isAutoNavControl = true
    // 缓存最新记录，恢复自动控制时同步状态
    private var latestRecord: BmiRecord? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        // 新增：根布局统一背景，避免Fragment切换间隙露白
        binding.root.setBackgroundColor(resources.getColor(android.R.color.white, theme))

        // 全局观察数据库记录，自动控制底部导航栏显隐
        lifecycleScope.launch {
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                repository.observeLatestRecord()
                    .distinctUntilChanged()
                    .collect { record ->
                        latestRecord = record
                        // 仅自动控制模式下才更新UI，强制隐藏时完全不响应数据库变化
                        if (isAutoNavControl) {
                            updateBottomNavVisibility(record != null)
                        }
                    }
            }
        }

        // 设置底部导航的点击监听
        setupBottomNav()

        // 新增：初始化默认显示Home页（只创建一次）
        if (supportFragmentManager.findFragmentByTag("Home") == null) {
            navigateToHome(isEmptyMode = false)
        }
    }

    private fun setupBottomNav() {
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    navigateToHome(isEmptyMode = false)
                    true
                }
                R.id.nav_display -> {
                    navigateToDisplay()
                    true
                }
//                R.id.nav_statistics -> {
//                    navigateToStatistics()
//                    true
//                }
                else -> false
            }
        }
    }

    // ---------- Fragment 导航方法（优化为复用实例+show/hide，解决白屏） ----------
    fun navigateToHome(isEmptyMode: Boolean) {
        val fm = supportFragmentManager
        val homeFragment = fm.findFragmentByTag("Home") as? HomeFragment
            ?: HomeFragment.newInstance(isEmptyMode)
        val displayFragment = fm.findFragmentByTag("Display")

        // 新增：切换前提前恢复导航栏自动控制（Home页遵循数据库规则）
        restoreBottomNavAuto()

        fm.beginTransaction().apply {
            // 隐藏其他Fragment，不销毁视图，切换无白屏
            displayFragment?.let { hide(it) }
            // 复用已有实例，不存在则添加
            if (homeFragment.isAdded) {
                show(homeFragment)
            } else {
                add(R.id.fragment_container, homeFragment, "Home")
            }
        }.commit()
    }

    private fun navigateToDisplay() {
        val fm = supportFragmentManager
        val displayFragment = fm.findFragmentByTag("Display") as? DisplayFragment
            ?: DisplayFragment()
        val homeFragment = fm.findFragmentByTag("Home")

        // 新增：切换前提前强制隐藏导航栏，避免闪现
        forceHideBottomNav()

        fm.beginTransaction().apply {
            homeFragment?.let { hide(it) }
            if (displayFragment.isAdded) {
                show(displayFragment)
            } else {
                add(R.id.fragment_container, displayFragment, "Display")
            }
        }.commit()
    }

//    private fun navigateToStatistics() {
//        supportFragmentManager.beginTransaction()
//            .replace(R.id.fragment_container, StatisticsFragment(), "Statistics")
//            .commit()
//    }

    // ---------- 新增：统一收口导航栏显隐，避免多处修改冲突 ----------
    private fun updateBottomNavVisibility(show: Boolean) {
        binding.bottomNav.visibility = if (show) View.VISIBLE else View.GONE
    }

    // 强制隐藏导航栏，暂停自动控制（DisplayFragment专用）
    override fun forceHideBottomNav() {
        isAutoNavControl = false
        updateBottomNavVisibility(false)
    }

    // 恢复自动控制，同步当前数据库状态
    override fun restoreBottomNavAuto() {
        isAutoNavControl = true
        updateBottomNavVisibility(latestRecord != null)
    }
}