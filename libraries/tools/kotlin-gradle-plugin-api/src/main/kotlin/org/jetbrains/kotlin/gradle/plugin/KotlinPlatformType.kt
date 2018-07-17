/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.Named
import org.gradle.api.attributes.Attribute
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import java.io.Serializable

// TODO: Do we really need a separate platform type for each native target?
open class KotlinPlatformType(private val name: String) : Named, Serializable {

    override fun toString(): String = name
    override fun getName(): String = name

    companion object {

        val COMMON = KotlinPlatformType("common")
        val JVM = KotlinPlatformType("JVM")
        val JS = KotlinPlatformType("JS")

        val attribute = Attribute.of(
            "org.jetbrains.kotlin.platform.type",
            KotlinPlatformType::class.java
        )
    }
}

// TODO: Make KonanTarget serializable
data class KotlinNativePlatformType(val konanTargetName: String) : KotlinPlatformType(konanTargetName) {
    val konanTarget: KonanTarget
        get() = HostManager().targetByName(konanTargetName)
}

fun KonanTarget.toKotlinPlatformType(): KotlinNativePlatformType = KotlinNativePlatformType(name)