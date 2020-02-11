/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.ir

import org.gradle.api.*
import org.gradle.api.plugins.ExtensionAware
import org.gradle.util.WrapUtil
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.targets.js.dsl.BuildVariantKind
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import javax.inject.Inject


open class KotlinJsBinaryContainer
@Inject
constructor(
    val target: KotlinJsIrTarget,
    backingContainer: DomainObjectSet<JsBinary>
) : DomainObjectSet<JsBinary> by backingContainer
{
    val project: Project
        get() = target.project

    private val defaultCompilation: KotlinJsIrCompilation
        get() = target.compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME)

    private val nameToBinary = mutableMapOf<String, JsBinary>()

    fun executable(
        buildTypes: Collection<BuildVariantKind> = listOf(BuildVariantKind.PRODUCTION, BuildVariantKind.DEVELOPMENT),
        configure: Executable.() -> Unit = {}
    ) = createBinaries(project.name, buildTypes, ::Executable, configure)

    private fun <T : JsBinary> createBinaries(
        baseName: String,
        buildKinds: Collection<BuildVariantKind>,
        create: (name: String, buildType: BuildVariantKind, compilation: KotlinJsIrCompilation) -> T,
        configure: T.() -> Unit
    ) {
        buildKinds.forEach { buildKind ->
            val name = generateBinaryName(baseName, buildKind)

            require(name !in nameToBinary) {
                "Cannot create binary $name: binary with such a name already exists"
            }

            val binary = create(baseName, buildKind, defaultCompilation)
            add(binary)
            nameToBinary[binary.name] = binary
            // Allow accessing binaries as properties of the container in Groovy DSL.
            if (this is ExtensionAware) {
                extensions.add(binary.name, binary)
            }
            binary.configure()
        }
    }

    companion object {
        internal fun generateBinaryName(name: String, buildKind: BuildVariantKind) =
            lowerCamelCaseName(name, buildKind.name)
    }
}
