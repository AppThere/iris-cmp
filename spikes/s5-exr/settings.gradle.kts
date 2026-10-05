// Spike S5: standalone build, not part of the product. Run with ../../gradlew -p spikes/s5-exr run --args=...
pluginManagement { repositories { mavenCentral(); gradlePluginPortal() } }
dependencyResolutionManagement { repositories { mavenCentral() } }
rootProject.name = "s5-exr"
