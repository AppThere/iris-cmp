plugins { id("iris.kmp.library") }

kotlin {
    sourceSets {
        commonTest.dependencies {
            implementation(libs.kotest.property)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
