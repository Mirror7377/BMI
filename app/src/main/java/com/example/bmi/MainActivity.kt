package com.example.bmi

import android.os.Bundle
import android.view.View
import androidx.core.content.ContentProviderCompat.requireContext
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
import com.example.bmi.utils.CommonBanner
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

    private var currentTag: String? = null
    private var bottomNavHeight = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 获取底部导航高度
        binding.bottomNav.post {
            bottomNavHeight = binding.bottomNav.height
        }

        // 监听数据库，控制导航栏显隐 + 通知 HomeFragment 更新按钮位置
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                repository.observeLatestRecord()
                    .distinctUntilChanged()
                    .collect { record ->
                        latestRecord = record
                        val isEmpty = record == null
                        binding.bottomNav.visibility = if (isEmpty) View.GONE else View.VISIBLE

                        val homeFragment = supportFragmentManager.findFragmentByTag("Home") as? HomeFragment
                        homeFragment?.setEmptyMode(isEmpty)
                    }
            }
        }

        setupBottomNav()

        // 根据 SplashActivity 传递的标志决定初始显示
        val hasData = intent.getBooleanExtra("hasData", false)
        if (hasData) {
            // 有数据：显示 DisplayFragment，并高亮底部导航第二项（index 1）
            navigateToDisplay()
            binding.bottomNav.selectedItemId = R.id.nav_display
        } else {
            // 无数据：显示 HomeFragment
            navigateToHome()
            binding.bottomNav.selectedItemId = R.id.nav_home
        }
    }

    fun getBottomNavHeight(): Int = bottomNavHeight

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
                R.id.nav_statistics -> {
                    navigateToStatistics()
                    true
                }
                else -> false
            }
        }
    }

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

        if (currentTag == "Statistics") return

        fragmentManager.fragments.forEach { fragment ->
            if (fragment != statisticsFragment && fragment.isAdded) {
                fragmentManager.beginTransaction()
                    .hide(fragment)
                    .commitNow()
            }
        }

        if (!statisticsFragment.isVisible) {
            fragmentManager.beginTransaction()
                .show(statisticsFragment)
                .commitNow()
        }

        currentTag = "Statistics"
    }

    fun goToHome() {
        navigateToHome()
        binding.bottomNav.selectedItemId = R.id.nav_home
    }

    fun hideBottomNav() {
        binding.bottomNav.visibility = View.GONE
    }

    fun showBottomNav() {
        if (latestRecord != null) {
            binding.bottomNav.visibility = View.VISIBLE
        }
    }

    override fun onResume() {
        super.onResume()
        checkPostSaveNavigation()
    }

    /**
     * 检查是否有保存后的导航标记，若有则执行跳转并清除
     */
    private fun checkPostSaveNavigation() {
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val target = prefs.getString("post_save_target", null) ?: return
        //删除键名
        prefs.edit().remove("post_save_target").apply()

        when (target) {
            "display" -> {
                navigateToDisplay()
                binding.bottomNav.selectedItemId = R.id.nav_display
            }
            "statistics" -> {
                navigateToStatistics()
                binding.bottomNav.selectedItemId = R.id.nav_statistics
            }
        }
    }
}