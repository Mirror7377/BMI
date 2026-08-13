import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.kotlin.ksp)
    alias(libs.plugins.hilt)
    id("kotlin-parcelize")
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.bmi"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.example.bmi"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        viewBinding = true
        compose = true
    }

    // ===== 新增：Compose 编译器报告配置 =====
    // 启用后，编译时会在 app/build/compose_reports/ 下生成 app-compose-report.txt
    // 该文件详细列出了每个 Composable 函数的重组作用域、稳定性等信息
    composeCompiler {
        reportsDestination = file("${layout.buildDirectory.get()}/compose_reports")
        // 如需更深入的稳定性分析，可取消下面注释并创建 stability-config.txt 文件
        // stabilityConfigurationFile = file("stability-config.txt")
    }

    // ===== Compose 编译器版本控制（与 composeCompiler 共存） =====
    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.compose.compiler.get()
    }

    kotlin {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_21
        }
    }
}

dependencies {
    // ===== 原有依赖 =====
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.material)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    implementation(libs.hilt.android)

    ksp(libs.room.compiler)
    kapt(libs.hilt.compiler)

    implementation("com.google.code.gson:gson:2.14.0")

    // ===== Compose 依赖 =====
    implementation(platform(libs.androidx.compose.bom))

    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.material3)

    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation("androidx.compose.material:material-icons-extended:1.6.8")
}