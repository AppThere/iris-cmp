package org.appthere.iris.buildlogic

import org.gradle.api.JavaVersion
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Bytecode level for every JVM and Android target; the build itself may run on a newer JDK.
internal val IRIS_JVM_TARGET = JvmTarget.JVM_17
internal val IRIS_JAVA_VERSION = JavaVersion.VERSION_17

/** Package root of every Iris module, and the Android application id. A placeholder until P-001 is settled. */
internal const val IRIS_PACKAGE_ROOT = "org.appthere.iris"

/** `iris-platform-input` becomes `org.appthere.iris.platform.input`; `app-desktop` becomes `org.appthere.iris.app.desktop`. */
internal fun modulePackage(moduleName: String): String =
    "$IRIS_PACKAGE_ROOT." + moduleName.removePrefix("iris-").replace('-', '.')
