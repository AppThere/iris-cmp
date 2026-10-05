package org.appthere.iris.buildlogic

/** One `<license>` entry of a POM. */
internal data class PomLicense(
    val name: String,
    val url: String,
)

/** Where a dependency ends up: in what Iris distributes, or only in tests. */
internal enum class LicenseScope { SHIPPED, TEST }

/** D-020: licenses allowed in shipped code; tests may also use EPL. */
private val SHIPPED_LICENSES = setOf("Apache-2.0", "MIT", "BSD-2-Clause", "BSD-3-Clause", "ISC", "Zlib")
private val TEST_LICENSES = SHIPPED_LICENSES + setOf("EPL-1.0", "EPL-2.0")

private val SPDX_PATTERNS: List<Pair<String, Regex>> =
    listOf(
        "Apache-2.0" to Regex("""apache.*(\b|v)2(\.0)?\b|apache\.org/licenses/license-2\.0"""),
        "MIT" to Regex("""\bmit\b|opensource\.org/licenses/mit"""),
        "BSD-3-Clause" to Regex("""bsd[- ]3|3-clause|new bsd|revised bsd"""),
        "BSD-2-Clause" to Regex("""bsd[- ]2|2-clause|simplified bsd"""),
        "ISC" to Regex("""\bisc\b"""),
        "Zlib" to Regex("""\bzlib\b"""),
        "EPL-1.0" to Regex("""(eclipse public license|\bepl\b).*(\b|v)1\.0\b|epl-1\.0|epl-v10"""),
        "EPL-2.0" to Regex("""(eclipse public license|\bepl\b).*(\b|v)2\.0\b|epl-2\.0|epl-v20"""),
    )

/** The SPDX id for a POM license, or null when it is not one we recognize (including ambiguous names like "BSD License"). */
internal fun spdxId(license: PomLicense): String? {
    val text = (license.name + " " + license.url).lowercase()
    return SPDX_PATTERNS.firstOrNull { (_, pattern) -> pattern.containsMatchIn(text) }?.first
}

/**
 * Null when [component] may be used in [scope]. A dual-licensed component passes if any of its licenses is allowed.
 * Otherwise a message naming each license (SPDX id, or the POM name in quotes when unrecognized).
 */
internal fun licenseViolation(
    component: String,
    licenses: List<PomLicense>,
    scope: LicenseScope,
): String? {
    if (licenses.isEmpty()) return "$component declares no license in its POM (or its parents)"
    val allowed = if (scope == LicenseScope.SHIPPED) SHIPPED_LICENSES else TEST_LICENSES
    val ids = licenses.map { spdxId(it) }
    if (ids.any { it in allowed }) return null
    val described = licenses.zip(ids).joinToString(", ") { (license, id) -> id ?: "'${license.name}'" }
    val where = if (scope == LicenseScope.SHIPPED) "shipped code" else "tests"
    return "$component is licensed under $described, which is not allowed in $where"
}

/** The licenses of [coordinate], taken from the nearest POM in its parent chain that declares any. */
internal fun licensesWithParents(
    coordinate: String,
    pomOf: (String) -> Pom?,
): List<PomLicense> {
    var current: String? = coordinate
    repeat(MAX_PARENT_DEPTH) {
        val pom = current?.let(pomOf) ?: return emptyList()
        if (pom.licenses.isNotEmpty()) return pom.licenses
        current = pom.parent
    }
    return emptyList()
}

private const val MAX_PARENT_DEPTH = 10

/**
 * Which configurations ship: runtime classpaths and the klibs native binaries link. Test configurations are [LicenseScope.TEST].
 * Debug variants and the desktop hot-reload configurations (`dev*`, `composeHotReload*`) are never distributed, so null.
 */
internal fun licenseScopeOf(configuration: String): LicenseScope? {
    val resolvesShippedCode = configuration.endsWith("RuntimeClasspath") || configuration == "runtimeClasspath" ||
        configuration.endsWith("CompileKlibraries")
    return when {
        !resolvesShippedCode -> null
        configuration.startsWith("test") || configuration.contains("Test") -> LicenseScope.TEST
        NOT_DISTRIBUTED.any { configuration.startsWith(it) } -> null
        else -> LicenseScope.SHIPPED
    }
}

private val NOT_DISTRIBUTED = listOf("debug", "dev", "composeHotReload")
