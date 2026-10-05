// Spike S10: standalone build, not part of the product. Run with ../../gradlew -p spikes/s10-chunks run --args=...
pluginManagement { repositories { mavenCentral(); gradlePluginPortal() } }
dependencyResolutionManagement { repositories { mavenCentral() } }
rootProject.name = "s10-chunks"
