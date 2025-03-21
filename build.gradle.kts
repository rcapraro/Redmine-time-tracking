import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
}

group = "com.ps"
version = project.findProperty("appVersion")?.toString() ?: "1.0.0"

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    google()
    maven("https://jitpack.io")
}

dependencies {
    // Compose Desktop
    implementation(compose.desktop.currentOs)
    implementation(compose.material)

    // Redmine API Client
    implementation(libs.redmine.api)
    implementation(libs.httpclient)

    // Kotlin Coroutines
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.swing)

    // Date/Time handling
    implementation(libs.datetime)

    // Koin
    implementation(libs.koin.core)
    implementation(libs.koin.compose)

    // Testing
    testImplementation(libs.koin.test)
    testImplementation(libs.koin.test.junit5)
    testImplementation(libs.mockk)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.junit)
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

kotlin {
    jvmToolchain(17)
}

// Ensure Java 17 is used for all tasks
tasks.withType<JavaCompile>().configureEach {
    sourceCompatibility = JavaVersion.VERSION_17.toString()
    targetCompatibility = JavaVersion.VERSION_17.toString()
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

// Task to generate Version.kt file with the current version
tasks.register("generateVersionFile") {
    val versionFile = file("src/main/kotlin/com/ps/redmine/Version.kt")
    val appVersion = project.findProperty("appVersion")?.toString() ?: "1.0.0"

    inputs.property("appVersion", appVersion)
    outputs.file(versionFile)

    doLast {
        versionFile.parentFile.mkdirs()
        versionFile.writeText(
            """
            package com.ps.redmine

            object Version {
                const val VERSION = "$appVersion"
            }
        """.trimIndent()
        )
    }
}

tasks.named("compileKotlin") {
    dependsOn("generateVersionFile")
}

compose.desktop {
    application {
        mainClass = "com.ps.redmine.MainKt"

        // Disable ProGuard to avoid Java version compatibility issues
        buildTypes.release.proguard {
            isEnabled = false
        }

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb, TargetFormat.Exe)
            packageName = "RedmineTime"
            val appVersionStr = project.findProperty("appVersion")?.toString() ?: "1.0.0"
            packageVersion = appVersionStr

            macOS {
                iconFile.set(project.file("src/main/resources/app_icon.icns"))
                packageVersion = appVersionStr
                dmgPackageVersion = appVersionStr
            }

            windows {
                iconFile.set(project.file("src/main/resources/app_icon.ico"))
                packageVersion = appVersionStr
                msiPackageVersion = appVersionStr
                exePackageVersion = appVersionStr
            }

            linux {
                iconFile.set(project.file("src/main/resources/app_icon.png"))
                packageVersion = appVersionStr
                debPackageVersion = appVersionStr
            }
        }
    }
}
