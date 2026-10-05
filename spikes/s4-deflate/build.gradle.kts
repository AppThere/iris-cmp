import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    kotlin("multiplatform") version "2.4.20"
}

kotlin {
    jvm {
        mainRun { mainClass.set("s4.MainKt") }
    }
    linuxX64()
    macosArm64()
    targets.withType<KotlinNativeTarget>().configureEach {
        binaries.executable { entryPoint = "s4.main" }
    }
    sourceSets {
        commonMain.dependencies {
            // Spike-only candidates; not product dependencies (S4 decides).
            implementation("com.squareup.okio:okio:3.18.2")
            implementation("com.soywiz:korlibs-compression:6.0.0")
        }
    }
}
