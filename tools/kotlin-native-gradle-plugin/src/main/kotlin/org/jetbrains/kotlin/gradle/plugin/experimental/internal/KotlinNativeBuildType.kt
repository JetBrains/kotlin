package org.jetbrains.kotlin.gradle.plugin.experimental.internal

import org.gradle.api.Named
import java.util.*

class KotlinNativeBuildType(
        private val name: String,
        val debuggable: Boolean,
        val optimized: Boolean
) : Named {

    override fun getName() = name

    companion object {
        val DEBUG = KotlinNativeBuildType("debug", true, false)
        val RELEASE = KotlinNativeBuildType("release", true, true)
        val DEFAULT_BUILD_TYPES: Collection<KotlinNativeBuildType> = Arrays.asList(DEBUG, RELEASE)
    }
}