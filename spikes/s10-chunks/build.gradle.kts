plugins {
    kotlin("jvm") version "2.4.20"
    application
}

dependencies { implementation("com.squareup.okio:okio:3.18.2") }

// Reuses the S5 EXR codec and the S4 ZIP writer/reader as they are.
sourceSets.main {
    kotlin.srcDir("../s5-exr/src/main/kotlin")
    kotlin.srcDir("build/generated/s4")
}
val copyS4 = tasks.register<Copy>("copyS4Zip") {
    from("../s4-deflate/src/commonMain/kotlin/s4") { include("Zip.kt", "ZipWriter.kt", "ZipReader.kt", "Crc32.kt", "Codec.kt", "OkioCodec.kt") }
    into("build/generated/s4")
}
tasks.named("compileKotlin") { dependsOn(copyS4) }

application {
    mainClass.set("s10.MainKt")
    applicationDefaultJvmArgs = listOf("-Xmx12g")
}
