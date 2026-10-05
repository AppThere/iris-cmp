import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins { kotlin("multiplatform") version "2.4.20" }

kotlin {
    jvm {
        compilerOptions { jvmTarget.set(JvmTarget.JVM_17) } // d8 (Android) reads 17; java.lang.foreign resolves against the running JDK 25
        mainRun { mainClass.set("s8.MainKt") }
    }
    linuxX64()
    targets.withType<KotlinNativeTarget>().configureEach { binaries.executable { entryPoint = "s8.main" } }
    sourceSets {
        commonMain.dependencies { implementation("org.jetbrains.kotlinx:kotlinx-collections-immutable:0.5.2") }
        all { languageSettings.optIn("kotlinx.cinterop.ExperimentalForeignApi") }
    }
}
// Android has no Panama; the ART run uses a jar of the JVM classes without the Panama backend (see s8.AndroidMain).
tasks.register<Jar>("artJar") {
    dependsOn("jvmMainClasses")
    from(kotlin.jvm().compilations["main"].output.classesDirs) { exclude("s8/Panama*") }
    from(configurations["jvmRuntimeClasspath"].map { zipTree(it) })
    archiveFileName.set("s8-art.jar")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
