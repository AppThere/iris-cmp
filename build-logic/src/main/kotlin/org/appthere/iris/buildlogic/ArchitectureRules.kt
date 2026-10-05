package org.appthere.iris.buildlogic

/**
 * What a module may depend on, copied from docs/architecture.md section 1 (`ArchitectureTableSyncTest` keeps them in step).
 * [anyDependency] covers the `all` and `any (test only)` rows. [importsChecked] marks modules that must not import platform APIs.
 */
internal data class ModuleRule(
    val allowed: Set<String> = emptySet(),
    val anyDependency: Boolean = false,
    val importsChecked: Boolean = false,
)

internal object ArchitectureRules {
    const val TESTING_MODULE = "iris-testing"
    private const val APP_PATTERN = "app-*"

    /** Package prefixes engine modules must not import: UI toolkits, Android, Apple platform libraries, Skia and C interop. */
    val FORBIDDEN_IMPORT_PREFIXES: List<String> =
        listOf(
            "androidx.",
            "android.",
            "java.awt.",
            "javax.swing.",
            "javafx.",
            "platform.",
            "org.jetbrains.skia.",
            "org.jetbrains.skiko.",
            "org.jetbrains.compose.",
            "kotlinx.cinterop.",
        )

    val table: Map<String, ModuleRule> =
        mapOf(
            "iris-core" to engine(),
            "iris-pixels" to engine("core"),
            "iris-color" to engine("core", "pixels"),
            "iris-vector" to engine("core"),
            "iris-exr" to engine("core", "pixels"),
            "iris-opc" to engine("core"),
            "iris-svg" to engine("core", "vector", "color"),
            "iris-model" to engine("core", "pixels", "color", "vector"),
            "iris-io" to engine("model", "exr", "opc", "svg"),
            "iris-render" to engine("model", "pixels", "color", "vector"),
            "iris-render-skia" to platform("render"),
            "iris-input" to engine("core"),
            "iris-brush" to engine("core", "pixels", "color", "input"),
            "iris-editor" to engine("model", "render", "brush", "input", "io"),
            "iris-ui" to platform("editor"),
            "iris-platform-input" to platform("input"),
            "iris-platform-files" to platform("core"),
            "iris-platform-color" to platform("color"),
            TESTING_MODULE to ModuleRule(anyDependency = true, importsChecked = true),
            "iris-cli" to platform("editor", "io"),
            APP_PATTERN to ModuleRule(anyDependency = true),
        )

    fun ruleFor(module: String): ModuleRule? = table[module] ?: table[APP_PATTERN].takeIf { module.startsWith("app-") }

    private fun engine(vararg deps: String) = ModuleRule(names(deps), importsChecked = true)

    private fun platform(vararg deps: String) = ModuleRule(names(deps))

    private fun names(deps: Array<out String>) = deps.map { "iris-$it" }.toSet()
}
