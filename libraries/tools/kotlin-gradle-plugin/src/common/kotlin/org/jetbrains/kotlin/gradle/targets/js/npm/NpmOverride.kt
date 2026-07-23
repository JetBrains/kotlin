/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm

import org.gradle.api.Named
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import java.io.Serializable
import javax.inject.Inject

@Deprecated("Replaced.")
class NpmOverride(
    val path: String,
) : Serializable {
    var includedVersions = mutableListOf<String>()
    var excludedVersions = mutableListOf<String>()

    fun include(vararg include: String) {
        includedVersions.addAll(include)
    }

    fun exclude(vararg exclude: String) {
        excludedVersions.addAll(exclude)
    }
}

@Deprecated("Replaced.")
@Suppress("DEPRECATION")
fun NpmOverride.toVersionString(): String {
    return buildNpmVersion(includedVersions, excludedVersions)
}

//abstract class NpmOverrideContainer
//@Inject internal constructor(
//    private val objects: ObjectFactory,
//) {
//
//}

abstract class NpmOverrideSpec
@Inject
internal constructor(
    private val packageName: String,
) : Named {
    abstract val range: Property<String>

    override fun getName(): String = packageName
}
