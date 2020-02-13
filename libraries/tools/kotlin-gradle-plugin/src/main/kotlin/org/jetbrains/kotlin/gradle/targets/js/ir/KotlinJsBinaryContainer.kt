/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.ir

import org.gradle.api.DomainObjectSet
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinBinaryContainer
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJsCompilation
import org.jetbrains.kotlin.gradle.targets.js.dsl.BuildVariantKind
import org.jetbrains.kotlin.gradle.targets.js.dsl.BuildVariantKind.DEVELOPMENT
import org.jetbrains.kotlin.gradle.targets.js.dsl.BuildVariantKind.PRODUCTION
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import javax.inject.Inject


open class KotlinJsBinaryContainer
@Inject
constructor(
    val target: KotlinBinaryContainer<KotlinJsCompilation, KotlinJsBinaryContainer>,
    backingContainer: DomainObjectSet<JsBinary>
) : DomainObjectSet<JsBinary> by backingContainer {
    val project: Project
        get() = target.project

    private val binaryNames = mutableSetOf<String>()
    private val compilationToBinaries = mutableMapOf<KotlinJsCompilation, MutableSet<JsBinary>>()

    private val defaultCompilation: KotlinJsCompilation
        get() = target.compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME)

    private val defaultTestCompilation: KotlinJsCompilation
        get() = target.compilations.getByName(KotlinCompilation.TEST_COMPILATION_NAME)

    fun executable(
        compilation: KotlinJsCompilation = defaultCompilation
    ) = createBinaries(
        compilation = compilation,
        jsBinaryType = JsBinaryType.EXECUTABLE,
        create = ::Executable
    )

    internal fun testExecutable() = createBinaries(
        compilation = defaultTestCompilation,
        jsBinaryType = JsBinaryType.TEST,
        create = ::TestExecutable
    )

    internal fun getBinary(
        compilation: KotlinJsCompilation,
        buildVariantKind: BuildVariantKind
    ): JsBinary =
        compilationToBinaries.getValue(
            compilation
        ).single { it.type == buildVariantKind }


    private fun <T : JsBinary> createBinaries(
        compilation: KotlinJsCompilation,
        buildVariantKinds: Collection<BuildVariantKind> = listOf(PRODUCTION, DEVELOPMENT),
        jsBinaryType: JsBinaryType,
        create: (name: String, buildVariantKind: BuildVariantKind, compilation: KotlinJsCompilation) -> T
    ) {
        buildVariantKinds.forEach { buildVariantKind ->
            val name = generateBinaryName(
                buildVariantKind,
                jsBinaryType
            )

            require(name !in binaryNames) {
                "Cannot create binary $name: binary with such a name already exists"
            }

            val binary = create(name, buildVariantKind, compilation)
            add(binary)
            with(compilationToBinaries[compilation]) {
                if (this != null) {
                    add(binary)
                } else {
                    compilationToBinaries[compilation] = mutableSetOf<JsBinary>(binary)
                }
            }
            // Allow accessing binaries as properties of the container in Groovy DSL.
            if (this is ExtensionAware) {
                extensions.add(binary.name, binary)
            }
        }
    }

    companion object {
        internal fun generateBinaryName(
            buildVariantKind: BuildVariantKind,
            jsBinaryType: JsBinaryType
        ) =
            lowerCamelCaseName(
                buildVariantKind.name,
                jsBinaryType.name
            )
    }
}
