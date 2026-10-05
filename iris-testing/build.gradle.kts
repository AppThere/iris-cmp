plugins { id("iris.kmp.library") }

kotlin {
    sourceSets {
        commonMain.dependencies {
            // The fakes implement iris-core and iris-input interfaces and expose kotlin-test and coroutines-test types.
            api(project(":iris-core"))
            api(project(":iris-input"))
            api(kotlin("test"))
            api(libs.kotlinx.coroutines.test)
        }
    }
}
