plugins { id("iris.kmp.library") }

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":iris-core"))
            implementation(project(":iris-vector"))
            implementation(project(":iris-color"))
        }
    }
}
