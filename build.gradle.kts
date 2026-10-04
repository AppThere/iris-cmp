// Root build. Module configuration lives in the convention plugins in build-logic/.

tasks.register("check") {
    description = "Runs all checks, including the build-logic tests."
    group = "verification"
    dependsOn(gradle.includedBuild("build-logic").task(":check"))
}
