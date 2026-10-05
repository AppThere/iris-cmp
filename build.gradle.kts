// Root build. Module configuration lives in the convention plugins in build-logic/.

plugins {
    id("iris.architecture")
    id("iris.golden")
}

tasks.named("check") {
    description = "Runs all checks, including the build-logic tests and verifyArchitecture."
    dependsOn(gradle.includedBuild("build-logic").task(":check"))
}
