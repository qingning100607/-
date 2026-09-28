import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}
// 签名信息统一从 local.properties 读取（该文件已 gitignore，绝不入库）
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

fun localProp(key: String, def: String = ""): String = localProps.getProperty(key) ?: def

android {
    namespace = "com.qingning.cloudrest"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.qingning.cloudrest"
        minSdk = 24
        targetSdk = 36
        versionCode = 21
        versionName = "1.11.1"
    }
    signingConfigs {
        create("release") {
            storeFile = rootProject.file(localProp("cloudrest.storeFile", "keystore/release.jks"))
            storePassword = localProp("cloudrest.storePassword")
            keyAlias = localProp("cloudrest.keyAlias", "androiddebugkey")
            keyPassword = localProp("cloudrest.keyPassword")
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
}
dependencies {
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.4")
    implementation("androidx.activity:activity-compose:1.12.4")
    val composeVersion = "1.10.2"
    implementation("androidx.compose.ui:ui:$composeVersion")
    implementation("androidx.compose.ui:ui-graphics:$composeVersion")
    implementation("androidx.compose.ui:ui-tooling-preview:$composeVersion")
    implementation("androidx.compose.foundation:foundation:$composeVersion")
    implementation("androidx.compose.material3:material3:1.4.0")
    implementation("androidx.compose.material:material-icons-core:1.7.8")
    debugImplementation("androidx.compose.ui:ui-tooling:$composeVersion")
}
