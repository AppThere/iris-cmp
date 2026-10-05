plugins { id("iris.kmp.library") }

kotlin {
    sourceSets {
        commonMain.dependencies {
            // The fakes implement iris-core interfaces and expose kotlin-test and coroutines-test types.
            api(project(":iris-core"))
            api(kotlin("test"))
            api(libs.kotlinx.coroutines.test)
        }
    }
}
