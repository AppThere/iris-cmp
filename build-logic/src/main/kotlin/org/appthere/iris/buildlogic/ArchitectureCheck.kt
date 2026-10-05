package org.appthere.iris.buildlogic

/** One `import` line of a Kotlin source file; [path] is relative to its module. */
internal data class SourceImport(
    val path: String,
    val line: Int,
    val name: String,
)

/** What a module declares: project dependencies by scope, and the imports of its Kotlin sources. */
internal data class ModuleFacts(
    val name: String,
    val mainDependencies: Set<String> = emptySet(),
    val testDependencies: Set<String> = emptySet(),
    val imports: List<SourceImport> = emptyList(),
)

private val IMPORT = Regex("""^\s*import\s+([A-Za-z_][\w.]*)""")

internal fun parseImports(
    path: String,
    text: String,
): List<SourceImport> =
    text.lineSequence().mapIndexedNotNull { index, line ->
        IMPORT.find(line)?.let { SourceImport(path, index + 1, it.groupValues[1]) }
    }.toList()

/** Checks [facts] against [ArchitectureRules]; returns one message per violation, empty when the graph is allowed. */
internal fun findArchitectureViolations(facts: Collection<ModuleFacts>): List<String> =
    facts.sortedBy { it.name }.flatMap { module ->
        val rule = ArchitectureRules.ruleFor(module.name)
        if (rule == null) {
            listOf("${module.name} is not in the rule table (docs/architecture.md section 1)")
        } else {
            dependencyViolations(module, rule) + importViolations(module, rule)
        }
    }

private fun dependencyViolations(
    module: ModuleFacts,
    rule: ModuleRule,
): List<String> {
    val testing = ArchitectureRules.TESTING_MODULE
    val main =
        module.mainDependencies.sorted().mapNotNull { dep ->
            when {
                dep == testing && module.name != testing && rule.anyDependency -> "${module.name} must not depend on $testing outside tests"
                rule.anyDependency || dep in rule.allowed -> null
                else -> "${module.name} must not depend on $dep (allowed: ${describe(rule.allowed)})"
            }
        }
    val test =
        module.testDependencies.sorted().mapNotNull { dep ->
            if (rule.anyDependency || dep in rule.allowed || dep == testing) {
                null
            } else {
                "${module.name} must not depend on $dep in tests (allowed: ${describe(rule.allowed)}, testing)"
            }
        }
    return main + test
}

private fun importViolations(
    module: ModuleFacts,
    rule: ModuleRule,
): List<String> {
    if (!rule.importsChecked) return emptyList()
    return module.imports
        .filter { import -> ArchitectureRules.FORBIDDEN_IMPORT_PREFIXES.any { import.name.startsWith(it) } }
        .filterNot { import -> isExempt(import, rule) }
        .map { "${module.name} must not import ${it.name} (${it.path}:${it.line})" }
}

/** `src/<sourceSet>/...`: an exemption applies in every source set except `commonMain` and `commonTest`. */
private fun isExempt(
    import: SourceImport,
    rule: ModuleRule,
): Boolean {
    val sourceSet = import.path.split('/').getOrElse(1) { "" }
    return !sourceSet.startsWith("common") && rule.platformSourceSetExemptions.any { import.name.startsWith(it) }
}

private fun describe(allowed: Set<String>): String =
    if (allowed.isEmpty()) "none" else allowed.joinToString(", ") { it.removePrefix("iris-") }
