/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.ir

import org.gradle.api.DomainObjectSet
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import javax.inject.Inject


open class KotlinJsBinaryContainer
@Inject
constructor(
    val target: KotlinJsIrTarget,
    backingContainer: DomainObjectSet<JsBinary>
) : DomainObjectSet<JsBinary> by backingContainer {
    val project: Project
        get() = target.project

    private val nameToBinary = mutableMapOf<String, JsBinary>()

    fun executable() = createBinaries(project.name, ::Executable)

    private fun <T : JsBinary> createBinaries(
        baseName: String,
        create: (name: String) -> T
    ) {
        val name = generateBinaryName(baseName)

        require(name !in nameToBinary) {
            "Cannot create binary $name: binary with such a name already exists"
        }

        val binary = create(baseName)
        add(binary)
        nameToBinary[binary.name] = binary
        // Allow accessing binaries as properties of the container in Groovy DSL.
        if (this is ExtensionAware) {
            extensions.add(binary.name, binary)
        }
    }

    companion object {
        internal fun generateBinaryName(name: String) =
            lowerCamelCaseName(name)
    }
}
