// Spike S8: standalone build, not part of the product. Run with ../../gradlew -p spikes/s8-tilebuffer ...
pluginManagement { repositories { mavenCentral(); gradlePluginPortal() } }
dependencyResolutionManagement { repositories { mavenCentral() } }
rootProject.name = "s8-tilebuffer"
