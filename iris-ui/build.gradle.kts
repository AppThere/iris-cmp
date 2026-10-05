plugins { id("iris.kmp.compose") }

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":iris-editor"))
        }
    }
}
