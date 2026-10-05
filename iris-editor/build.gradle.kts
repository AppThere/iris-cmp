plugins { id("iris.kmp.library") }

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":iris-model"))
            implementation(project(":iris-render"))
            implementation(project(":iris-brush"))
            implementation(project(":iris-input"))
            implementation(project(":iris-io"))
        }
    }
}
