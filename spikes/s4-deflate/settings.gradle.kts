// Spike S4: standalone build, not part of the product. Run with ../../gradlew -p spikes/s4-deflate.
pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositories { mavenCentral() }
}
rootProject.name = "s4-deflate"
