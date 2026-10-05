plugins { id("iris.kmp.library") }

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":iris-core"))
            implementation(project(":iris-pixels"))
            implementation(project(":iris-color"))
            implementation(project(":iris-input"))
        }
    }
}
