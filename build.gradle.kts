import nl.littlerobots.vcu.plugin.resolver.VersionSelectors
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension


plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.versionCatalogUpdate)
}

group = "com.ps"
version = project.findProperty("appVersion")?.toString() ?: "1.0.0"

repositories {
    mavenCentral()
    google()
    // Only include JetBrains repositories that are actually needed
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    maven("https://maven.pkg.jetbrains.space/public/p/ktor/maven")
}

dependencies {
    // Compose Desktop
    implementation(compose.desktop.currentOs)
    implementation(compose.material)
    implementation(compose.materialIconsExtended)

    // Ktor client dependencies
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    // We're using kotlinx.serialization directly without Ktor's content negotiation
    implementation(libs.kotlinx.serialization.json)

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
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

configure<KotlinJvmProjectExtension> {
    jvmToolchain(21)
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        freeCompilerArgs.addAll(
            listOf(
                "-opt-in=kotlin.time.ExperimentalTime",
                "-opt-in=androidx.compose.material.ExperimentalMaterialApi",
                "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi"
                // Note: StrongSkipping is enabled by default in Compose 1.8.2
            )
        )
    }
}

versionCatalogUpdate {
    sortByKey.set(false)
    versionSelector(VersionSelectors.STABLE)
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

        jvmArgs += listOf(
            // Performance optimizations
            "-XX:+UseG1GC",
            "-XX:+UseStringDeduplication"
        )

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
