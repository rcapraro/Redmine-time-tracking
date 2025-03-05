import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.compose)
}

group = "com.ps"
version = "1.0-SNAPSHOT"

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

compose.desktop {
    application {
        mainClass = "com.ps.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "RedmineTime"
            packageVersion = "1.0.0"
        }
    }
}
