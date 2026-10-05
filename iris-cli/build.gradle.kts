plugins { id("iris.jvm.app") }

dependencies {
    implementation(project(":iris-editor"))
    implementation(project(":iris-io"))
}

tasks.test {
    // CliVersionTest compares the CLI's output with the version the build was configured with.
    systemProperty("iris.expectedVersion", project.version.toString())
}
