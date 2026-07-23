/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.yarn

import org.gradle.api.Named
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.provider.Property
import org.jetbrains.kotlin.gradle.targets.js.npm.buildNpmVersion
import org.jetbrains.kotlin.gradle.utils.newInstance
import java.io.Serializable
import javax.inject.Inject

@Deprecated("internal util")
class YarnResolution(
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

@Suppress("DEPRECATION")
@Deprecated("internal util")
fun YarnResolution.toVersionString(): String {
    return buildNpmVersion(includedVersions, excludedVersions)
}

//abstract class YarnResolutionContainer
//internal constructor() {
//    internal abstract val resolutions: NamedDomainObjectContainer<YarnResolutionSpec>
//
//    fun register(name: String, configure: YarnResolutionSpec.() -> Unit) {
//        resolutions.register(name, configure)
//    }
//
//    fun configure(packageName: String)  {
//        resolutions.named(packageName) {
//            configure(this)
//        }
//    }
//}
//
//internal fun org.gradle.api.model.ObjectFactory.YarnResolutionContainer(): YarnResolutionContainer
// = newInstance<YarnResolutionContainer>()


abstract class YarnResolutionSpec
@Inject
internal constructor(
    private val packageName: String,
) : Named {
    abstract val range: Property<String>

    override fun getName(): String = packageName
}
