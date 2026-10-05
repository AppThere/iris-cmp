plugins { id("iris.app.android") }

dependencies {
    implementation(project(":iris-ui"))
    implementation(project(":iris-editor"))
    implementation(project(":iris-render-skia"))
    implementation(project(":iris-platform-input"))
    implementation(project(":iris-platform-files"))
    implementation(project(":iris-platform-color"))
}
