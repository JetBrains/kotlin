package org.jetbrains.kotlin.konan.util

import org.jetbrains.kotlin.utils.PathUtil
import org.jetbrains.kotlin.*
import java.io.File
import java.nio.file.Paths

object KonanHomeProvider {
    internal val validPropertiesNames = listOf("kotlin.native.home",
                                               "org.jetbrains.kotlin.native.home",
                                               "konan.home")
    internal val kotlinNativeHome
        get() = validPropertiesNames.mapNotNull(System::getProperty).firstOrNull()
    /**
     * Determines a path to the current Kotlin/Native distribution.
     *
     *  - If the system property "konan.home" is set, the method returns its normalized value.
     *  - Otherwise, it determines a path to a jar containing this class. If this path corresponds to the jar path
     *    inside a distribution, the method calculates the path to the distribution on the basis of this jar path.
     *    Otherwise an IllegalStateException is thrown.
     */
    fun determineKonanHome(): String {
        val propertyValue = kotlinNativeHome
        return if (propertyValue != null) {
            // KT-58979: KonanLibraryImpl needs normalized klib paths to correctly provide symbols from resolved klibs
            // For extra safety, path to "konan.home" is also normalized here
            Paths.get(File(propertyValue).absolutePath).normalize().toString()
        } else {
            val jarPath = PathUtil.getResourcePathForClass(this::class.java)

            // Check that the path obtained really points to the distribution.
            check(jarPath.toPath().endsWith(Paths.get("konan/lib/kotlin-native.jar")) ||
                    jarPath.toPath().endsWith(Paths.get("konan/lib/kotlin-native-compiler-embeddable.jar"))) {
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