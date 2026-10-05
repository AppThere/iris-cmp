plugins { id("iris.kmp.library") }

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":iris-core"))
            // StylusInputSource exposes Flow.
            api(libs.kotlinx.coroutines.core)
        }
    }
}
