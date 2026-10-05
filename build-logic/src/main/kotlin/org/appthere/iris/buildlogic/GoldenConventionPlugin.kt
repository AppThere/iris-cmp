package org.appthere.iris.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.register

/** Applied to the root project: `goldenUpdate -Pname=...` and `goldenReview` over `testdata/golden/` (docs/testing-tdd.md section 4). */
class GoldenConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        val testdata = target.layout.projectDirectory.dir("testdata")
        target.tasks.register<GoldenUpdateTask>("goldenUpdate") {
            group = "verification"
            description = "Stages golden candidates matching -Pname=<name> in testdata/golden-pending/ for review."
            goldenName.set(target.providers.gradleProperty("name"))
            candidateDirectories.set(
                target.subprojects.associate { it.name to it.layout.buildDirectory.dir("golden-candidates").get().asFile.absolutePath },
            )
            pendingDirectory.set(testdata.dir("golden-pending"))
            outputs.upToDateWhen { false }
        }
        target.tasks.register<GoldenReviewTask>("goldenReview") {
            group = "verification"
            description = "Lists pending golden adds and changes against testdata/golden/."
            goldenDirectory.set(testdata.dir("golden"))
            pendingDirectory.set(testdata.dir("golden-pending"))
            outputs.upToDateWhen { false }
        }
    }
}
