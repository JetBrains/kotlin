/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.sources.android

import com.android.build.gradle.api.AndroidSourceSet
import org.gradle.api.NamedDomainObjectContainer

internal val NamedDomainObjectContainer<out AndroidSourceSet>.main: AndroidSourceSet
    get() = getByName(AndroidBaseSourceSetName.Main.name)

internal val NamedDomainObjectContainer<out AndroidSourceSet>.test: AndroidSourceSet
    get() = getByName(AndroidBaseSourceSetName.Test.name)

internal val NamedDomainObjectContainer<out AndroidSourceSet>.androidTest: AndroidSourceSet
    get() = getByName(AndroidBaseSourceSetName.AndroidTest.name)

/*
Not written as enum class to avoid Enum.name ambiguity with 'source set name' semantics.
 */
internal sealed class AndroidBaseSourceSetName(val name: String) {
    final override fun toString(): String = name

    object Main : AndroidBaseSourceSetName("main")
    object Test : AndroidBaseSourceSetName("test")
    object AndroidTest : AndroidBaseSourceSetName("androidTest")

    companion object {
        fun byName(name: String): AndroidBaseSourceSetName? = when (name) {
            Main.name -> Main
            Test.name -> Test
            AndroidTest.name -> AndroidTest
            else -> null
        }
    }
}