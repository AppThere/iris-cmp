package org.appthere.iris.buildlogic

/**
 * The Compose desktop runtime artifact for a host, from the `os.name` and `os.arch` system properties.
 * Mirrors the deprecated `compose.desktop.currentOs` accessor of the Compose Gradle plugin.
 */
internal fun composeDesktopArtifact(
    osName: String,
    osArch: String,
): String {
    val os =
        when {
            osName.startsWith("Linux") -> "linux"
            osName.startsWith("Win") -> "windows"
            osName.startsWith("Mac") -> "macos"
            else -> error("Compose desktop does not support the host OS '$osName'")
        }
    val arch =
        when (osArch) {
            "amd64", "x86_64" -> "x64"
            "aarch64" -> "arm64"
            else -> error("Compose desktop does not support the host architecture '$osArch'")
        }
    return "desktop-jvm-$os-$arch"
}
