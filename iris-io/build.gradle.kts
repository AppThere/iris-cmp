plugins { id("iris.kmp.library") }

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":iris-model"))
            implementation(project(":iris-exr"))
            implementation(project(":iris-opc"))
            implementation(project(":iris-svg"))
        }
    }
}
