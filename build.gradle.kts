import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm") version "1.8.20"
    id("org.jetbrains.compose") version "1.7.3"
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
    implementation("com.taskadapter:redmine-java-api:3.1.3")
    implementation("org.apache.httpcomponents:httpclient:4.5.14")

    // Kotlin Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.7.1")

    // Date/Time handling
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.0")

    // Koin
    implementation("io.insert-koin:koin-core:3.4.0")
    implementation("io.insert-koin:koin-compose:1.0.4")

    // Testing
    testImplementation("io.insert-koin:koin-test:3.4.0")
    testImplementation("io.insert-koin:koin-test-junit5:3.4.0")
    testImplementation("io.mockk:mockk-jvm:1.13.5")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.1")
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.3")
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
