import nl.littlerobots.vcu.plugin.resolver.VersionSelectors
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.compose.desktop.application.tasks.AbstractJLinkTask
import org.jetbrains.compose.desktop.application.tasks.AbstractJPackageTask
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import javax.inject.Inject


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
    implementation(libs.compose.material3)
    // JetBrains pinned this at 1.7.3 — using direct coordinates (not the deprecated
    // `compose.materialIconsExtended` accessor) silences the warning. AndroidX's
    // KMP-published equivalent isn't a drop-in: it pulls androidx.compose.ui transitives
    // that conflict with CMP's compose-ui at runtime.
    implementation(libs.compose.material.icons.extended)
    implementation(libs.compose.components.resources)

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

    // OS keychain (macOS Keychain, Windows Credential Manager, Linux Secret Service)
    implementation(libs.java.keyring)

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
            // `jcef` only exists in JetBrains Runtime builds with JCEF.
            modules("java.instrument", "java.management", "java.prefs", "jcef", "jdk.security.auth", "jdk.unsupported")
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

interface InjectedOps {
    @get:Inject
    val exec: ExecOperations

    @get:Inject
    val fs: FileSystemOperations
}

val injectedOps = objects.newInstance<InjectedOps>()
val hostOs: String = System.getProperty("os.name")

// JBR keeps the CEF framework and its helper apps in Contents/Frameworks, outside the jcef module, so jpackage
// leaves them out. JCEF expects them at java.home/../Frameworks; the bundle is re-signed ad hoc after.
// mise flattens Contents/Home into the install root, which moves Contents/Frameworks below java.home.
if (hostOs.startsWith("Mac")) {
    tasks.withType<AbstractJPackageTask>()
        .matching { it.name == "createDistributable" || it.name == "createReleaseDistributable" }
        .configureEach {
            val jbrHome = javaHome
            val appDir = destinationDir
            val ops = injectedOps
            doLast("copyJcefFrameworks") {
                val frameworks = listOf("../Frameworks", "Contents/Frameworks")
                    .map { File(jbrHome.get(), it).normalize() }
                    .firstOrNull { File(it, "Chromium Embedded Framework.framework").isDirectory }
                    ?: throw GradleException("No JCEF frameworks found in ${jbrHome.get()}: use a JBR with JCEF")
                val app = appDir.get().asFile.listFiles { f -> f.name.endsWith(".app") }?.singleOrNull()
                    ?: throw GradleException("No .app bundle found in ${appDir.get()}")
                val target = File(app, "Contents/runtime/Contents/Frameworks")
                ops.exec.exec { commandLine("ditto", frameworks.absolutePath, target.absolutePath) }
                ops.exec.exec { commandLine("codesign", "--force", "--sign", "-", app.absolutePath) }
            }
        }
}

// On Windows the jcef module ships the CEF resources (paks, ICU data, helper, locales) in its bin section,
// which jlink does not copy into the runtime image.
if (hostOs.startsWith("Windows")) {
    tasks.withType<AbstractJLinkTask>().configureEach {
        val jbrBin = javaHome.map { File(it, "bin") }
        val runtimeBin = destinationDir.map { it.dir("bin") }
        val ops = injectedOps
        doLast("copyJcefResources") {
            ops.fs.copy {
                from(jbrBin) {
                    include("jcef_helper.exe", "icudtl.dat", "*.pak", "v8_context_snapshot.bin", "locales/**")
                }
                into(runtimeBin)
            }
        }
    }
}
