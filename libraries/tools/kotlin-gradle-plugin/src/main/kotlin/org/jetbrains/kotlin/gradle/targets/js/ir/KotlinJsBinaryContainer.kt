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
import org.jetbrains.kotlin.gradle.targets.js.dsl.BuildVariantKind
import org.jetbrains.kotlin.gradle.targets.js.dsl.BuildVariantKind.DEVELOPMENT
import org.jetbrains.kotlin.gradle.targets.js.dsl.BuildVariantKind.PRODUCTION
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsSubTargetContainerDsl
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
        if (compilation !is KotlinJsIrCompilation) {
            project.logger.warn(
                "binaries.executable configuration is useless with IR compiler." +
                        "Use produceExecutable() instead"
            )
            return
        }

        (target as KotlinJsSubTargetContainerDsl).whenBrowserConfigured {
            (this as KotlinJsIrSubTarget).produceExecutable()
        }

        target.whenNodejsConfigured {
            (this as KotlinJsIrSubTarget).produceExecutable()
        }

        compilation.binaries.executableInternal(compilation)
    }

    internal fun executableInternal(compilation: KotlinJsCompilation) = createBinaries(
        compilation = compilation,
        jsBinaryType = JsBinaryType.EXECUTABLE,
        create = ::Executable
    )

    internal fun getBinary(
        buildVariantKind: BuildVariantKind
    ): JsBinary =
        matching { it.type == buildVariantKind }
            .single()

    private fun <T : JsBinary> createBinaries(
        compilation: KotlinJsCompilation,
        buildVariantKinds: Collection<BuildVariantKind> = listOf(PRODUCTION, DEVELOPMENT),
        jsBinaryType: JsBinaryType,
        create: (compilation: KotlinJsCompilation, name: String, buildVariantKind: BuildVariantKind) -> T
    ) {
        buildVariantKinds.forEach { buildVariantKind ->
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
            buildVariantKind: BuildVariantKind,
            jsBinaryType: JsBinaryType?
        ) =
            lowerCamelCaseName(
                compilation.name.let { if (it == KotlinCompilation.MAIN_COMPILATION_NAME) null else it },
                buildVariantKind.name.toLowerCase(),
                jsBinaryType?.name?.toLowerCase()
            )
    }
}
