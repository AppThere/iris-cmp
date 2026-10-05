package s7

import kotlin.math.abs

private const val COLORD = "/usr/share/color/icc/colord"

/** Runs transicc (Little CMS 2.17) on [inputs], relative colorimetric, and parses its "X=1.0 Y=2.0" lines. */
fun transicc(input: String, output: String, inputs: List<DoubleArray>): List<DoubleArray> {
    val p = ProcessBuilder("transicc", "-i", input, "-o", output, "-t", "1").redirectError(ProcessBuilder.Redirect.DISCARD).start()
    p.outputStream.bufferedWriter().use { w -> inputs.forEach { w.write(it.joinToString(" ") + "\n") } }
    val lines = p.inputStream.bufferedReader().readLines().filter { "=" in it }
    p.waitFor()
    return lines.map { l -> Regex("=\\s*(-?[0-9.]+)").findAll(l).map { it.groupValues[1].toDouble() }.toList().toDoubleArray() }
}

private fun grid(n: Int, dims: Int): List<DoubleArray> =
    (0 until Math.pow(n.toDouble(), dims.toDouble()).toInt()).map { k -> DoubleArray(dims) { d -> ((k / Math.pow(n.toDouble(), d.toDouble()).toInt()) % n) / (n - 1.0) } }

private fun rgbToRgb(src: String, dst: String) {
    val a = MatrixShaper(Profile.load(src))
    val b = MatrixShaper(Profile.load(dst))
    val inputs = grid(9, 3)
    val ref = transicc(src, dst, inputs.map { v -> DoubleArray(3) { v[it] * 255 } })
    val diffs = inputs.indices.map { i -> val ours = b.fromXyz(a.toXyz(inputs[i])); (0 until 3).maxOf { abs(ours[it] * 255 - ref[i][it]) } }
    println("matrix/TRC ${src.substringAfterLast('/')} -> ${dst.substringAfterLast('/')}: ${inputs.size} colors, max |diff| ${"%.4f".format(diffs.max())} / 255, mean ${"%.5f".format(diffs.average())}")
}

private fun cmykToLab(path: String) {
    val p = Profile.load(path)
    val lut = Lut(p.tag(if (p.has("A2B1")) "A2B1" else "A2B0"), pcsInputIsXyz = false)
    val inputs = grid(6, 4)
    val ref = transicc(path, "*Lab", inputs.map { v -> DoubleArray(4) { v[it] * 100 } })
    val de = inputs.indices.map { i -> deltaE76(lut.decodeLab(lut.eval(inputs[i])), ref[i]) }
    println("LUT ${path.substringAfterLast('/')} CMYK -> Lab (A2B1, mft2): ${inputs.size} colors, max dE76 ${"%.4f".format(de.max())}, mean ${"%.5f".format(de.average())}")
}

private fun labToCmyk(path: String) {
    val p = Profile.load(path)
    val lut = Lut(p.tag(if (p.has("B2A1")) "B2A1" else "B2A0"), pcsInputIsXyz = false, labInput = true)
    val inputs = grid(7, 3).map { v -> doubleArrayOf(20 + v[0] * 75, -60 + v[1] * 120, -60 + v[2] * 120) }
    val ref = transicc("*Lab", path, inputs)
    val diffs = inputs.indices.map { i -> val ours = lut.eval(lut.encodeLab(inputs[i])); (0 until 4).maxOf { abs(ours[it] * 100 - ref[i][it]) } }
    println("LUT ${path.substringAfterLast('/')} Lab -> CMYK (${p.tag(if (p.has("B2A1")) "B2A1" else "B2A0").type}): ${inputs.size} colors, max |diff| ${"%.4f".format(diffs.max())} % ink, mean ${"%.5f".format(diffs.average())}")
}

fun main() {
    rgbToRgb("$COLORD/sRGB.icc", "$COLORD/ProPhotoRGB.icc")
    rgbToRgb("$COLORD/sRGB.icc", "$COLORD/AdobeRGB1998.icc")
    rgbToRgb("$COLORD/Rec709.icc", "$COLORD/sRGB.icc")
    rgbToRgb("/usr/share/color/icc/ghostscript/srgb.icc", "/usr/share/color/icc/ghostscript/rommrgb.icc")
    cmykToLab("$COLORD/FOGRA39L_coated.icc")
    cmykToLab("$COLORD/GRACoL_TR006_coated.icc")
    labToCmyk("$COLORD/FOGRA39L_coated.icc")
    labToCmyk("/usr/share/color/icc/ghostscript/default_cmyk.icc")
}
