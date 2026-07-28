package com.example.bmi.ui.profile

import android.app.Application
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bmi.data.database.BmiRecord
import com.example.bmi.data.repository.BmiRepository
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject
@HiltViewModel
class ProfileViewModel @Inject constructor(private val repository: BmiRepository,private val application: Application) : ViewModel() {
    private val _state = MutableStateFlow(ProfileState())
    val state: StateFlow<ProfileState> = _state.asStateFlow()

    private val _effect = MutableSharedFlow<ProfileEffect>()
    val effect: SharedFlow<ProfileEffect> = _effect.asSharedFlow()

    fun handleIntent(intent: ProfileIntent) {
        when (intent) {
            is ProfileIntent.Init -> loadData()
            is ProfileIntent.Login -> performLogin()
            is ProfileIntent.Logout -> performLogout()
            is ProfileIntent.ImportJson -> importJson(intent.json)
            is ProfileIntent.ImportSampleData -> importSampleData()
        }
    }

    private fun loadData() {
        //模拟未登录
        viewModelScope.launch {
            _state.value = _state.value.copy(
                isLoggedIn = false,
                userName = "",
                userEmail = ""
            )
        }
    }

    private fun performLogin() {
        viewModelScope.launch {
            // 模拟登录
            _state.value = _state.value.copy(
                isLoggedIn = true,
                userName = "Cassie",
                userEmail = "cassiexiao@gmail.com"
            )
        }
    }

    private fun performLogout() {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                isLoggedIn = false,
                userName = "",
                userEmail = ""
            )
        }
    }

    private fun importJson(json: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val gson = Gson()
                // 让 Gson 知道要解析成 List<BmiRecord>。
                val type = object : TypeToken<List<BmiRecord>>() {}.type
                val records: List<BmiRecord> = gson.fromJson(json, type)
                if (records.isNotEmpty()) {
                    repository.insertAll(records)
                    withContext(Dispatchers.Main) {
                        //切换回主线程，因为 UI 相关的操作（如 Toast、Banner）必须在主线程执行。
                        _effect.emit(ProfileEffect.Success)
                    }
                } else {
                    Log.d("ProfileViewModel", "No records to import.")
                }
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Import failed: ${e.message}", e)
            }
        }
    }

    private fun importSampleData() {
        viewModelScope.launch(Dispatchers.IO) { // 全部在 IO 线程
            try {
                // 1. 读取文件（在后台线程）
                val jsonString = application.assets.open("sample_records.json")
                    .bufferedReader().use { it.readText() }

                // 2. 解析 JSON（Gson 解析也耗 CPU，也在后台线程）
                val gson = Gson()
                val type = object : TypeToken<List<BmiRecord>>() {}.type
                val records: List<BmiRecord> = gson.fromJson(jsonString, type)

                // 3. 插入数据库
                if (records.isNotEmpty()) {
                    repository.insertAll(records)
                    withContext(Dispatchers.Main) {
                        _effect.emit(ProfileEffect.Success) // 成功 -> 显示 Banner
                    }
                } else {
                    Log.d("ProfileViewModel", "No records to import.")
                }
            } catch (e: IOException) {
                // 文件读取失败
                Log.e("ProfileViewModel", "Failed to load sample data: ${e.message}", e)
            } catch (e: Exception) {
                // 解析或数据库插入失败
                Log.e("ProfileViewModel", "Import failed: ${e.message}", e)
            }
        }
    }
}