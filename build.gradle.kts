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
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)
    implementation(compose.components.resources)

    // Ktor client dependencies
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    // We're using kotlinx.serialization directly without Ktor's content negotiation
    implementation(libs.kotlinx.serialization.json)

    // SLF4J provider — Ktor logs through SLF4J; without a provider it prints "No SLF4J providers were found" at startup.
    runtimeOnly(libs.slf4j.simple)

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
    testImplementation(libs.ktor.client.mock)
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
                "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
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
            "-XX:+UseG1GC",
            "-XX:+UseStringDeduplication",
            "-Djava.awt.headless=false"
        )

        buildTypes.release.proguard {
            isEnabled = true
            optimize = true
            obfuscate = false
            configurationFiles.from(project.file("proguard-rules.pro"))
        }

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb, TargetFormat.Exe)
            // Slim the bundled JRE — only ship the JDK modules the app actually needs.
            // List produced by `./gradlew suggestRuntimeModules`; rerun if dependencies change.
            modules("java.instrument", "java.management", "java.prefs", "jdk.unsupported")
            packageName = "RedmineTime"
            val appVersionStr = project.findProperty("appVersion")?.toString() ?: "1.0.0"
            // jpackage requires a strict X.Y.Z numeric version — strip any pre-release suffix (e.g. "-beta").
            val installerVersion = appVersionStr.substringBefore('-')
            packageVersion = installerVersion

            macOS {
                iconFile.set(project.file("src/main/resources/app_icon.icns"))
                packageVersion = installerVersion
                dmgPackageVersion = installerVersion
            }

            windows {
                iconFile.set(project.file("src/main/resources/app_icon.ico"))
                packageVersion = installerVersion
                msiPackageVersion = installerVersion
                exePackageVersion = installerVersion
                menuGroup = "RedmineTime"
                shortcut = true
                dirChooser = true
                console = false
                upgradeUuid = "61DAB35E-17CB-43B0-81D5-B30E1C0BABE7"
            }

            linux {
                iconFile.set(project.file("src/main/resources/app_icon.png"))
                packageVersion = installerVersion
                debPackageVersion = installerVersion
            }
        }
    }
}
