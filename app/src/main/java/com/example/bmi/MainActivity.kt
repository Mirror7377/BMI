package com.example.bmi

import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
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

@AndroidEntryPoint
class MainActivity : BaseActivity() {

    @Inject
    lateinit var repository: BmiRepository

    private lateinit var binding: ActivityMainBinding
    private var latestRecord: BmiRecord? = null

    // 当前显示的 Fragment 标签
    private var currentTag: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)



        // 监听数据库，控制导航栏显隐 + 通知 HomeFragment 更新按钮位置
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                repository.observeLatestRecord()
                    .distinctUntilChanged()//记录发生变化才发射新数据
                    .collect { record ->
                        latestRecord = record
                        val isEmpty = record == null

                        // 控制底部导航栏显隐
                        binding.bottomNav.visibility = if (isEmpty) View.GONE else View.VISIBLE

                        // 同步更新 HomeFragment 的按钮位置
                        val homeFragment = supportFragmentManager.findFragmentByTag("Home") as? HomeFragment
                        homeFragment?.setEmptyMode(isEmpty)
                    }
            }
        }

        setupBottomNav()

        // 初始显示 HomeFragment
        navigateToHome()
    }

    private fun setupBottomNav() {
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    navigateToHome()
                    true
                }
                R.id.nav_display -> {
                    navigateToDisplay()
                    true
                }
                R.id.nav_statistics ->{
                    navigateToStatistics()
                    true
                }
                else -> false
            }
        }
    }

    /**
     * 显示 HomeFragment
     */
    private fun navigateToHome() {
        val fragmentManager = supportFragmentManager
        var homeFragment = fragmentManager.findFragmentByTag("Home")
        if (homeFragment == null) {
            homeFragment = HomeFragment.newInstance()
            fragmentManager.beginTransaction()
                .add(R.id.fragment_container, homeFragment, "Home")
                .commit()
        }

        if (currentTag == "Home") return

        fragmentManager.fragments.forEach { fragment ->
            if (fragment != homeFragment && fragment.isAdded) {
                fragmentManager.beginTransaction()
                    .hide(fragment)
                    .commitNow()
            }
        }

        if (!homeFragment.isVisible) {
            fragmentManager.beginTransaction()
                .show(homeFragment)
                .commitNow()
        }

        currentTag = "Home"
    }

    /**
     * 显示 DisplayFragment（历史记录展示页）
     */
    private fun navigateToDisplay() {
        val fragmentManager = supportFragmentManager
        var displayFragment = fragmentManager.findFragmentByTag("Display")
        if (displayFragment == null) {
            displayFragment = DisplayFragment.newInstance()
            fragmentManager.beginTransaction()
                .add(R.id.fragment_container, displayFragment, "Display")
                .commit()
        }

        if (currentTag == "Display") return

        fragmentManager.fragments.forEach { fragment ->
            if (fragment != displayFragment && fragment.isAdded) {
                fragmentManager.beginTransaction()
                    .hide(fragment)
                    .commitNow()
            }
        }

        if (!displayFragment.isVisible) {
            fragmentManager.beginTransaction()
                .show(displayFragment)
                .commitNow()
        }

        currentTag = "Display"
    }

    private fun navigateToStatistics() {
        val fragmentManager = supportFragmentManager
        var statisticsFragment = fragmentManager.findFragmentByTag("Statistics")
        if (statisticsFragment == null) {
            statisticsFragment = StatisticsFragment()
            fragmentManager.beginTransaction()
                .add(R.id.fragment_container, statisticsFragment, "Statistics")
                .commit()
        }

        // 如果当前已经是 Statistics，则不重复操作
        if (currentTag == "Statistics") return

        // 隐藏其他已添加的 Fragment
        fragmentManager.fragments.forEach { fragment ->
            if (fragment != statisticsFragment && fragment.isAdded) {
                fragmentManager.beginTransaction()
                    .hide(fragment)
                    .commitNow()
            }
        }

        // 显示 StatisticsFragment
        if (!statisticsFragment.isVisible) {
            fragmentManager.beginTransaction()
                .show(statisticsFragment)
                .commitNow()
        }

        currentTag = "Statistics"
    }

    // MainActivity.kt

    /**
     * 供外部调用 切换到 HomeFragment
     */
    fun goToHome() {
        navigateToHome()
        binding.bottomNav.selectedItemId = R.id.nav_home
    }
}