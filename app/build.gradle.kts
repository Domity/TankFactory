import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "io.github.domity.tankfactory"
    compileSdk = 36
    ndkVersion = "28.2.13676358"

    defaultConfig {
        applicationId = "io.github.domity.tankfactory"
        minSdk = 31
        targetSdk = 37
        versionCode = 114
        versionName = "1.1.4"
        androidResources{
            localeFilters += listOf("zh")
        }
        externalNativeBuild {
            cmake {
                cppFlags.add("-std=c++17")
            }
        }
        ndk {
            abiFilters.addAll(listOf("arm64-v8a"))
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isJniDebuggable = false
            isDebuggable = false
            vcsInfo.include = false
            proguardFiles("proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    androidResources {
        ignoreAssetsPatterns.add("dexopt")
    }

    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
        dex {
            useLegacyPackaging = true
        }
        resources {
            excludes += "**/dexopt/**"
            excludes += "**/LICENSE.txt"
            excludes += "**/NOTICE.txt"
            excludes += "**/META-INF/*.txt"
            excludes += "**/META-INF/*.textproto"
            excludes += "**/META-INF/*.version"
            excludes += "**/META-INF/com/**"
            excludes += "kotlin/**"
            excludes += "**/kotlin-tooling-metadata.json"
            excludes += "**/DebugProbesKt.bin"
        }
    }
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
}
kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}

configurations.all {
    exclude(group = "androidx.profileinstaller", module = "profileinstaller")
}
dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.compose.foundation)
}
