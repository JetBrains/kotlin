package org.jetbrains.kotlin.konan.util

import org.jetbrains.kotlin.utils.PathUtil
import java.io.File
import java.nio.file.Paths

object KonanHomeProvider {

    /**
     * Determines a path to the current Kotlin/Native distribution.
     *
     *  - If the system property "konan.home" is set, the method returns its value.
     *  - Otherwise, it determines a path to a jar containing this class. If this path corresponds to the jar path
     *    inside a distribution, the method calculates the path to the distribution on the basis of this jar path.
     *    Otherwise an IllegalStateException is thrown.
     */
    fun determineKonanHome(): String {
        val propertyValue = System.getProperty("konan.home")
        return if (propertyValue != null) {
            File(propertyValue).absolutePath
        } else {
            val jarPath = PathUtil.getResourcePathForClass(this::class.java)

            // Check that the path obtained really points to the distribution.
            val expectedRelativeJarPath = Paths.get("konan/lib/kotlin-native.jar")
            check(jarPath.toPath().endsWith(expectedRelativeJarPath)) {
                val classesPath = if (jarPath.extension == "jar") jarPath else jarPath.parentFile
                """
                    Cannot determine a compiler distribution directory.
                    A path to compiler classes is not a part of a distribution: ${classesPath.absolutePath}.
                    Please set the konan.home system property to specify the distribution path manually.
                """.trimIndent()
            }

            // The compiler jar is located in <dist>/konan/lib.
            jarPath.parentFile.parentFile.parentFile.absolutePath
        }
    }
}