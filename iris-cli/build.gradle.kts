plugins { id("iris.jvm.app") }

dependencies {
    implementation(project(":iris-editor"))
    implementation(project(":iris-io"))
}

tasks.test {
    // CliVersionTest compares the CLI's output with the version the build was configured with.
    systemProperty("iris.expectedVersion", project.version.toString())
}

tasks.processResources {
    // Fills in version.properties, which `iris --version` prints.
    val version = project.version.toString()
    inputs.property("version", version)
    filesMatching("**/version.properties") { expand("version" to version) }
}
