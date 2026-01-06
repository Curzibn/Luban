@file:Suppress("UnstableApiUsage")

import com.vanniktech.maven.publish.SonatypeHost
import java.util.Properties

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.maven.publish)
}

// 尝试加载 local.properties 中的配置到项目中
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { localProperties.load(it) }
    localProperties.forEach { key, value ->
        if (project.findProperty(key.toString()) == null) {
            project.extensions.extraProperties[key.toString()] = value
        }
    }
}

val libraryVersion = "2.0.0"

android {
    namespace = "top.zibin.luban"
    compileSdk = 36

    defaultConfig {
        minSdk = 21

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")

        ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
        }

        externalNativeBuild {
            cmake {
                cppFlags += "-std=c++17"
                arguments += listOf("-DANDROID_STL=c++_static", "-DANDROID_SUPPORT_FLEXIBLE_PAGE_SIZES=ON")
            }
        }
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    packaging {
        jniLibs {
            useLegacyPackaging = true
            excludes += "lib/**/libc++_shared.so"
        }
    }
}

dependencies {
    api(libs.kotlinx.coroutines.android)
    api(libs.kotlinx.coroutines.core)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.exifinterface)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

mavenPublishing {
    // 配置发布到 Sonatype Central Portal (central.sonatype.com)
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)

    // 自动签名配置
    signAllPublications()

    coordinates("top.zibin", "luban", libraryVersion)

    pom {
        name.set("Luban")
        description.set("Luban（鲁班） —— Android图片压缩工具，仿微信朋友圈压缩策略。")
        url.set("https://github.com/Curzibn/Luban")
        
        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }
        
        developers {
            developer {
                id.set("Curzibn")
                name.set("Zibin Zheng")
                email.set("a@zibin.top")
            }
        }
        
        scm {
            connection.set("scm:git:github.com/Curzibn/Luban.git")
            developerConnection.set("scm:git:ssh://github.com/Curzibn/Luban.git")
            url.set("https://github.com/Curzibn/Luban/tree/master")
        }
    }
}
