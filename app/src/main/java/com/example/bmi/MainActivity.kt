package com.example.bmi

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        // 默认隐藏底部导航
        binding.bottomNav.visibility = View.GONE

        // 观察数据库记录数
        lifecycleScope.launch {
            repository.hasAnyRecord()
                .distinctUntilChanged()  // 避免重复触发
                .collect { hasRecord ->
                    if (hasRecord) {
                        // 有记录 → 显示底部导航，跳转到 BMI 显示页
                        binding.bottomNav.visibility = View.VISIBLE
                        binding.bottomNav.selectedItemId = R.id.nav_display
                        //todo
                        navigateToDisplay()
                    } else {
                        // 无记录 → 隐藏底部导航，跳转到特殊首页
                        binding.bottomNav.visibility = View.GONE
                        navigateToHome(isEmptyMode = true)
                    }
                }
        }

        // 设置底部导航的点击监听
        setupBottomNav()
    }

    private fun setupBottomNav() {
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    navigateToHome(isEmptyMode = false)
                    true
                }
                //todo
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

    // ---------- Fragment 导航方法 ----------
    private fun navigateToHome(isEmptyMode: Boolean) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, HomeFragment.newInstance(isEmptyMode), "Home")
            .commit()
    }
    //todo
    private fun navigateToDisplay() {
        // 如果 DisplayFragment 还未创建，先创建占位
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, DisplayFragment(), "Display")
            .commit()
    }
//
//    private fun navigateToStatistics() {
//        supportFragmentManager.beginTransaction()
//            .replace(R.id.fragment_container, StatisticsFragment(), "Statistics")
//            .commit()
//    }
}