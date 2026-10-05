plugins { id("iris.kmp.compose") }

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.foundation)
            implementation(project(":iris-editor"))
        }
    }
}
