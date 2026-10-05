package org.appthere.iris.buildlogic

import org.gradle.api.JavaVersion
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Bytecode level for every JVM and Android target; the build itself may run on a newer JDK.
internal val IRIS_JVM_TARGET = JvmTarget.JVM_17
internal val IRIS_JAVA_VERSION = JavaVersion.VERSION_17

/** `iris-platform-input` becomes `org.appthere.iris.platform.input`; `app-desktop` becomes `org.appthere.iris.app.desktop`. */
internal fun modulePackage(moduleName: String): String =
    "org.appthere.iris." + moduleName.removePrefix("iris-").replace('-', '.')
