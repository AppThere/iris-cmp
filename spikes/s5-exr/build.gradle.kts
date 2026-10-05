plugins {
    kotlin("jvm") version "2.4.20"
    application
}

dependencies {
    // Deflate provider decided by S4 (D-028).
    implementation("com.squareup.okio:okio:3.18.2")
}

application { mainClass.set("s5.MainKt") }
