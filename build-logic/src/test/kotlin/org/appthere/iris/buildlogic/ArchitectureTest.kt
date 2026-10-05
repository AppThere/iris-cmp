package org.appthere.iris.buildlogic

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ArchitectureTest {
    @Test
    fun `the module graph allowed by the table passes`() {
        val facts =
            ArchitectureRules.table.filterKeys { it != "app-*" }.map { (name, rule) ->
                ModuleFacts(name, mainDependencies = rule.allowed, testDependencies = rule.allowed + "iris-testing")
            }

        assertEquals(emptyList(), findArchitectureViolations(facts + appFacts("app-desktop", setOf("iris-ui", "iris-editor"))))
    }

    @Test
    fun `iris-core depending on iris-model is a violation`() {
        val violations = findArchitectureViolations(listOf(ModuleFacts("iris-core", mainDependencies = setOf("iris-model"))))

        assertEquals(listOf("iris-core must not depend on iris-model (allowed: none)"), violations)
    }

    @Test
    fun `an engine file importing java awt Color is a violation`() {
        val imports = parseImports("src/commonMain/kotlin/Bad.kt", "package x\n\nimport java.awt.Color\n")

        val violations = findArchitectureViolations(listOf(ModuleFacts("iris-model", imports = imports)))

        assertEquals(listOf("iris-model must not import java.awt.Color (src/commonMain/kotlin/Bad.kt:3)"), violations)
    }

    @Test
    fun `every forbidden platform package is rejected in engine modules`() {
        val forbidden =
            listOf(
                "androidx.compose.runtime.Composable",
                "android.graphics.Bitmap",
                "javax.swing.JPanel",
                "platform.UIKit.UIView",
                "org.jetbrains.skia.Surface",
                "kotlinx.cinterop.CPointer",
            )
        val imports = forbidden.mapIndexed { i, name -> SourceImport("A.kt", i + 1, name) }

        val violations = findArchitectureViolations(listOf(ModuleFacts("iris-core", imports = imports)))

        assertEquals(forbidden.size, violations.size, violations.joinToString("\n"))
    }

    @Test
    fun `plain kotlin imports are fine in engine modules`() {
        val imports = parseImports("A.kt", "import kotlin.math.abs\nimport kotlinx.coroutines.flow.Flow\nimport org.appthere.iris.core.Rect\n")

        assertEquals(emptyList(), findArchitectureViolations(listOf(ModuleFacts("iris-core", imports = imports))))
    }

    @Test
    fun `platform modules may import platform APIs`() {
        val imports = listOf(SourceImport("A.kt", 1, "android.view.MotionEvent"), SourceImport("B.kt", 1, "platform.UIKit.UITouch"))

        assertEquals(emptyList(), findArchitectureViolations(listOf(ModuleFacts("iris-platform-input", imports = imports))))
    }

    @Test
    fun `iris-pixels may use cinterop for its TileBuffer actuals, outside commonMain only`() {
        val native = listOf(SourceImport("src/nativeMain/kotlin/TileBuffer.kt", 3, "kotlinx.cinterop.nativeHeap"))
        val common = listOf(SourceImport("src/commonMain/kotlin/Tiles.kt", 3, "kotlinx.cinterop.CPointer"))
        val otherModule = listOf(SourceImport("src/iosMain/kotlin/X.kt", 1, "kotlinx.cinterop.CPointer"))

        assertEquals(emptyList(), findArchitectureViolations(listOf(ModuleFacts("iris-pixels", imports = native))))
        assertEquals(1, findArchitectureViolations(listOf(ModuleFacts("iris-pixels", imports = common))).size)
        assertEquals(1, findArchitectureViolations(listOf(ModuleFacts("iris-core", imports = otherModule))).size)
    }

    @Test
    fun `iris-testing is allowed in test scope only`() {
        val inMain = findArchitectureViolations(listOf(ModuleFacts("iris-model", mainDependencies = setOf("iris-testing"))))
        val inApp = findArchitectureViolations(listOf(appFacts("app-android", setOf("iris-testing"))))
        val inTest = findArchitectureViolations(listOf(ModuleFacts("iris-model", testDependencies = setOf("iris-testing"))))

        assertEquals(listOf("iris-model must not depend on iris-testing (allowed: core, pixels, color, vector)"), inMain)
        assertEquals(listOf("app-android must not depend on iris-testing outside tests"), inApp)
        assertEquals(emptyList(), inTest)
    }

    @Test
    fun `a module missing from the rule table is a violation`() {
        val violations = findArchitectureViolations(listOf(ModuleFacts("iris-extra")))

        assertEquals(listOf("iris-extra is not in the rule table (docs/architecture.md section 1)"), violations)
    }

    @Test
    fun `apps match the app wildcard row`() {
        assertTrue(ArchitectureRules.ruleFor("app-ios")?.anyDependency == true)
    }

    private fun appFacts(
        name: String,
        deps: Set<String>,
    ) = ModuleFacts(name, mainDependencies = deps)
}
