/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.ir

import org.gradle.api.DomainObjectSet
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJsCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinTargetWithBinaries
import org.jetbrains.kotlin.gradle.targets.js.KotlinJsTarget
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsBinaryType
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsBinaryType.DEVELOPMENT
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsBinaryType.PRODUCTION
import org.jetbrains.kotlin.gradle.targets.js.subtargets.KotlinJsSubTarget
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import javax.inject.Inject


open class KotlinJsBinaryContainer
@Inject
constructor(
    val target: KotlinTargetWithBinaries<KotlinJsCompilation, KotlinJsBinaryContainer>,
    backingContainer: DomainObjectSet<JsBinary>
) : DomainObjectSet<JsBinary> by backingContainer {
    val project: Project
        get() = target.project

    private val binaryNames = mutableSetOf<String>()

    private val defaultCompilation: KotlinJsCompilation
        get() = target.compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME)

    fun executable(
        compilation: KotlinJsCompilation = defaultCompilation
    ) {
        if (target is KotlinJsIrTarget) {
            target.whenBrowserConfigured {
                (this as KotlinJsIrSubTarget).produceExecutable()
            }

            target.whenNodejsConfigured {
                (this as KotlinJsIrSubTarget).produceExecutable()
            }

            compilation.binaries.executableIrInternal(compilation)
        }

        if (target is KotlinJsTarget) {
            target.irTarget
                ?.let { throw IllegalStateException("Unfortunately you can't use `executable()` with 'both' compiler type") }

            target.whenBrowserConfigured {
                (this as KotlinJsSubTarget).produceExecutable()
            }

            target.whenNodejsConfigured {
                (this as KotlinJsSubTarget).produceExecutable()
            }

            compilation.binaries.executableLegacyInternal(compilation)
        }
    }

    internal fun executableIrInternal(compilation: KotlinJsCompilation) = createBinaries(
        compilation = compilation,
        jsBinaryType = JsBinaryType.EXECUTABLE,
        create = ::Executable
    )

    private fun executableLegacyInternal(compilation: KotlinJsCompilation) = createBinaries(
        compilation = compilation,
        jsBinaryType = JsBinaryType.EXECUTABLE,
        create = { compilation, name, type ->
            object : JsBinary {
                override val compilation: KotlinJsCompilation = compilation
                override val name: String = name
                override val type: KotlinJsBinaryType = type
            }
        }
    )

    internal fun getIrBinary(
        type: KotlinJsBinaryType
    ): JsIrBinary =
        matching { it.type == type }
            .withType(JsIrBinary::class.java)
            .single()

    private fun <T : JsBinary> createBinaries(
        compilation: KotlinJsCompilation,
        types: Collection<KotlinJsBinaryType> = listOf(PRODUCTION, DEVELOPMENT),
        jsBinaryType: JsBinaryType,
        create: (compilation: KotlinJsCompilation, name: String, type: KotlinJsBinaryType) -> T
    ) {
        types.forEach { buildVariantKind ->
            val name = generateBinaryName(
                compilation,
                buildVariantKind,
                jsBinaryType
            )

            require(name !in binaryNames) {
                "Cannot create binary $name: binary with such a name already exists"
            }

            val binary = create(compilation, name, buildVariantKind)
            add(binary)
            // Allow accessing binaries as properties of the container in Groovy DSL.
            if (this is ExtensionAware) {
                extensions.add(binary.name, binary)
            }
        }
    }

    companion object {
        internal fun generateBinaryName(
            compilation: KotlinJsCompilation,
            type: KotlinJsBinaryType,
            jsBinaryType: JsBinaryType?
        ) =
            lowerCamelCaseName(
                compilation.name.let { if (it == KotlinCompilation.MAIN_COMPILATION_NAME) null else it },
                type.name.toLowerCase(),
                jsBinaryType?.name?.toLowerCase()
            )
    }
}
