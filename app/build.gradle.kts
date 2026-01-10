plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.rbtsoft.tankfactory"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.rbtsoft.tankfactory"
        minSdk = 31
        targetSdk = 36
        versionCode = 1101
        versionName = "1.1.0 alpha1"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        resourceConfigurations += setOf("zh")
        externalNativeBuild {
            cmake {
                cppFlags.add("-std=c++17")
            }
        }
        ndk {
            abiFilters.addAll(listOf("arm64-v8a"))
        }
    }

    @Suppress("UnstableApiUsage")
    experimentalProperties["android.experimental.vcs-info.include"] = false

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isJniDebuggable = false
            isDebuggable = false
            vcsInfo.include = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
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
        ignoreAssetsPatterns.add("*.prof")
    }

    aaptOptions {
        ignoreAssetsPattern = ".git:.svn:.ds_store:*.scc:.*:Gcv:cvs:thumbs.db:picasa.ini:*~:baseline.prof:dexopt"
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
            excludes += "dexopt/**"
            excludes += "**/META-INF/com/**"
            excludes += "META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/services/**"
            excludes += "META-INF/**"
            excludes += "**/kotlin-tooling-metadata.json"
            excludes += "**/DebugProbesKt.bin"
            excludes += "kotlin/**"
            excludes += "**/okhttp3/**"
            excludes += "**/*.version"
            excludes += "**/*.txt"
            excludes += "**/*.properties"
        }
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
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.compose.ui.text)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.runtime)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    implementation(libs.coil.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.coroutines.android)
}
