/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.gradle.api.GradleException
import org.gradle.jvm.toolchain.JavaLauncher
import java.nio.file.Path
import kotlin.io.path.exists

private const val GRAALVM_VERSION_PROPERTY = "GRAALVM_VERSION"
private const val MIN_GRAALVM_VERSION = "25.1.3"

fun JavaLauncher.resolveNativeImageExecutable(isWindows: Boolean = false): Path {
    val javaHome = executablePath.asFile.toPath().parent.parent

    val nativeImageName = if (isWindows) "native-image.exe" else "native-image"
    val nativeImageBin = javaHome.resolve("lib/svm/bin/$nativeImageName")
    if (!nativeImageBin.exists()) {
        throw GradleException("native-image not found at ${nativeImageBin.toAbsolutePath()} (JAVA_HOME=${javaHome.toAbsolutePath()})")
    }

    val releaseFile = javaHome.resolve("release")
    if (!releaseFile.exists()) {
        throw GradleException("JDK release file not found at ${releaseFile.toAbsolutePath()} (JAVA_HOME=${javaHome.toAbsolutePath()})")
    }

    val releaseVersion = releaseFile.toFile()
        .readLines()
        .firstOrNull { it.startsWith("$GRAALVM_VERSION_PROPERTY=") }
        ?.substringAfter("=")
        ?.trim()
        ?.removePrefix("\"")
        ?.removeSuffix("\"")
        ?.asSemVer()
        ?: throw GradleException("Failed to read GRAALVM_VERSION from the release file: $releaseFile")

    val expectedVersion = MIN_GRAALVM_VERSION.asSemVer()
    for ((actual, expected) in releaseVersion.zip(expectedVersion)) {
        if (actual < expected) throw GradleException(
            "The native-image requires GraalVM ${expectedVersion.joinToString(".")} or newer, " +
                    "but the current version is ${releaseVersion.joinToString(".")}. " +
                    "Please update GraalVM to the latest version."
        )
        if (actual > expected) break
    }

    return nativeImageBin
}

private fun String.asSemVer(): List<Int> {
    return split(".").map { it.toInt() }.also {
        if (it.size != 3) throw GradleException("Unexpected GRAALVM_VERSION format: \'$this\'")
    }
}
