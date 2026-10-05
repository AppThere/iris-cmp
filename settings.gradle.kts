pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "iris"

include(
    ":iris-core",
    ":iris-pixels",
    ":iris-color",
    ":iris-vector",
    ":iris-exr",
    ":iris-opc",
    ":iris-svg",
    ":iris-model",
    ":iris-io",
    ":iris-render",
    ":iris-render-skia",
    ":iris-input",
    ":iris-brush",
    ":iris-editor",
    ":iris-ui",
    ":iris-platform-input",
    ":iris-platform-files",
    ":iris-platform-color",
    ":iris-testing",
    ":iris-cli",
    ":app-desktop",
    ":app-android",
    ":app-ios",
)
