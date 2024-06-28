/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.ir

import org.gradle.api.DomainObjectSet
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJsCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinTargetWithBinaries
import org.jetbrains.kotlin.gradle.plugin.mpp.isMain
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsBinaryMode
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsBinaryMode.DEVELOPMENT
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsBinaryMode.PRODUCTION
import org.jetbrains.kotlin.gradle.targets.js.ir.JsIrBinary.Companion.configLinkTask
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly
import javax.inject.Inject

open class KotlinJsBinaryContainer
@Inject
constructor(
    val target: KotlinTargetWithBinaries<KotlinJsIrCompilation, KotlinJsBinaryContainer>,
    backingContainer: DomainObjectSet<JsIrBinary>,
) : DomainObjectSet<JsIrBinary> by backingContainer {
    val project: Project
        get() = target.project

    private val binaryNames = mutableSetOf<String>()

    private val defaultCompilation: KotlinJsIrCompilation
        get() = target.compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME)

    fun executable(
        compilation: KotlinJsIrCompilation,
    ): List<JsIrBinary> {
        return executable(compilation as KotlinJsCompilation)
    }

    @JvmOverloads
    fun executable(
        compilation: KotlinJsCompilation = defaultCompilation,
    ): List<JsIrBinary> {
        if (target is KotlinJsIrTarget) {
            target.whenBrowserConfigured {
                (this as KotlinJsIrSubTarget).produceExecutable()
            }

            target.whenNodejsConfigured {
                (this as KotlinJsIrSubTarget).produceExecutable()
            }

            target.whenD8Configured {
                (this as KotlinJsIrSubTarget).produceExecutable()
            }

            return compilation.binaries.executableIrInternal(compilation as KotlinJsIrCompilation)
        }

        throw GradleException("Target should be KotlinJsIrTarget, but found $target")
    }

    internal fun executableIrInternal(compilation: KotlinJsIrCompilation): List<JsIrBinary> = createBinaries(
        compilation = compilation,
        jsBinaryType = KotlinJsBinaryType.EXECUTABLE,
        create = { jsCompilation, name, mode ->
            if (target.platformType == KotlinPlatformType.wasm) {
                ExecutableWasm(jsCompilation, name, mode)
            } else {
                Executable(jsCompilation, name, mode)
            }
        }
    )

    // For Groovy DSL
    @JvmOverloads
    fun library(
        compilation: KotlinJsIrCompilation = defaultCompilation,
    ): List<JsIrBinary> {
        if (target is KotlinJsIrTarget) {
            target.whenBrowserConfigured {
                (this as KotlinJsIrSubTarget).produceLibrary()
            }

            target.whenNodejsConfigured {
                (this as KotlinJsIrSubTarget).produceLibrary()
            }

            target.whenD8Configured {
                (this as KotlinJsIrSubTarget).produceLibrary()
            }

            return createBinaries(
                compilation = compilation,
                jsBinaryType = KotlinJsBinaryType.LIBRARY,
                create = ::Library
            )
        }

        throw GradleException(
            """
            Library can be produced only for IR compiler.
            Use `kotlin.js.compiler=ir` Gradle property or `js(IR)` target declaration.
            """
        )
    }

    internal fun getIrBinaries(
        mode: KotlinJsBinaryMode,
    ): DomainObjectSet<JsIrBinary> =
        withType(JsIrBinary::class.java)
            .matching { it.mode == mode }

    private fun <T : JsIrBinary> createBinaries(
        compilation: KotlinJsIrCompilation,
        modes: Collection<KotlinJsBinaryMode> = listOf(PRODUCTION, DEVELOPMENT),
        jsBinaryType: KotlinJsBinaryType,
        create: (compilation: KotlinJsIrCompilation, name: String, mode: KotlinJsBinaryMode) -> T,
    ) =
        modes.map {
            createBinary(
                compilation,
                it,
                jsBinaryType,
                create
            )
        }

    private fun <T : JsIrBinary> createBinary(
        compilation: KotlinJsIrCompilation,
        mode: KotlinJsBinaryMode,
        jsBinaryType: KotlinJsBinaryType,
        create: (compilation: KotlinJsIrCompilation, name: String, mode: KotlinJsBinaryMode) -> T,
    ): JsIrBinary {
        val name = generateBinaryName(
            compilation,
            mode,
            jsBinaryType
        )

        if (name in binaryNames) {
            return single { it.name == name }
        }

        binaryNames.add(name)

        val binary = create(compilation, name, mode).also {
            it.configLinkTask()
        }

        add(binary)
        // Allow accessing binaries as properties of the container in Groovy DSL.
        if (this is ExtensionAware) {
            extensions.add(binary.name, binary)
        }

        return binary
    }

    companion object {
        internal fun generateBinaryName(
            compilation: KotlinJsCompilation,
            mode: KotlinJsBinaryMode,
            jsBinaryType: KotlinJsBinaryType?,
        ) =
            lowerCamelCaseName(
                if (compilation.isMain()) null else compilation.name,
                mode.name.toLowerCaseAsciiOnly(),
                jsBinaryType?.name?.toLowerCaseAsciiOnly()
            )
    }
}
