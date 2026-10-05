plugins {
    `kotlin-dsl`
}

group = "org.appthere.iris.buildlogic"

// TestKit tests that build small fixture projects with the convention plugins applied.
val functionalTest: SourceSet = sourceSets.create("functionalTest")

gradlePlugin {
    testSourceSets(functionalTest)
    plugins {
        register("kmpLibrary") {
            id = "iris.kmp.library"
            implementationClass = "org.appthere.iris.buildlogic.KmpLibraryConventionPlugin"
        }
        register("kmpCompose") {
            id = "iris.kmp.compose"
            implementationClass = "org.appthere.iris.buildlogic.KmpComposeConventionPlugin"
        }
        register("appDesktop") {
            id = "iris.app.desktop"
            implementationClass = "org.appthere.iris.buildlogic.DesktopAppConventionPlugin"
        }
        register("appAndroid") {
            id = "iris.app.android"
            implementationClass = "org.appthere.iris.buildlogic.AndroidAppConventionPlugin"
        }
        register("appIos") {
            id = "iris.app.ios"
            implementationClass = "org.appthere.iris.buildlogic.IosAppConventionPlugin"
        }
        register("jvmApp") {
            id = "iris.jvm.app"
            implementationClass = "org.appthere.iris.buildlogic.JvmAppConventionPlugin"
        }
        register("quality") {
            id = "iris.quality"
            implementationClass = "org.appthere.iris.buildlogic.QualityConventionPlugin"
        }
    }
}

configurations[functionalTest.implementationConfigurationName].extendsFrom(configurations.testImplementation.get())
configurations[functionalTest.runtimeOnlyConfigurationName].extendsFrom(configurations.testRuntimeOnly.get())

dependencies {
    implementation(libs.gradlePlugin.kotlin)
    implementation(libs.gradlePlugin.android)
    implementation(libs.gradlePlugin.composeCompiler)
    implementation(libs.gradlePlugin.compose)
    implementation(libs.gradlePlugin.detekt)
    implementation(libs.gradlePlugin.spotless)
    implementation(libs.gradlePlugin.kover)

    testImplementation(kotlin("test-junit5"))
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    "functionalTestImplementation"(gradleTestKit())
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

val functionalTestTask =
    tasks.register<Test>("functionalTest") {
        description = "Runs TestKit tests against fixture projects."
        group = "verification"
        testClassesDirs = functionalTest.output.classesDirs
        classpath = functionalTest.runtimeClasspath
        // Fixtures read the real catalog, so the tests check the versions we ship with.
        systemProperty("iris.versionCatalog", layout.projectDirectory.file("../gradle/libs.versions.toml").asFile.absolutePath)
        // Share the user's Gradle home so fixtures do not re-download every dependency.
        systemProperty("iris.testKitDir", gradle.gradleUserHomeDir.absolutePath)
        shouldRunAfter(tasks.test)
    }

tasks.check {
    dependsOn(functionalTestTask)
}
