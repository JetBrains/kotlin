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
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrType.DEVELOPMENT
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrType.PRODUCTION
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

    private val nameToBinary = mutableMapOf<String, JsBinary>()

    private val defaultCompilation: KotlinJsCompilation
        get() = target.compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME)

    private val defaultTestCompilation: KotlinJsCompilation
        get() = target.compilations.getByName(KotlinCompilation.TEST_COMPILATION_NAME)

    fun executable() = createBinaries(
        baseName = project.name,
        jsBinaryType = JsBinaryType.EXECUTABLE,
        create = ::Executable
    )

    internal fun testExecutable() = createBinaries(
        baseName = project.name,
        jsBinaryType = JsBinaryType.TEST,
        create = ::TestExecutable
    )

    private fun <T : JsBinary> createBinaries(
        baseName: String,
        buildVariantKinds: Collection<KotlinJsIrType> = listOf(PRODUCTION, DEVELOPMENT),
        jsBinaryType: JsBinaryType,
        create: (name: String, buildVariantKind: KotlinJsIrType, compilation: KotlinJsCompilation) -> T
    ) {
        buildVariantKinds.forEach { buildVariantKind ->
            val name = generateBinaryName(
                baseName,
                buildVariantKind,
                jsBinaryType
            )

            require(name !in nameToBinary) {
                "Cannot create binary $name: binary with such a name already exists"
            }

            val compilation = if (jsBinaryType == JsBinaryType.TEST) {
                defaultTestCompilation
            } else {
                defaultCompilation
            }


            val binary = create(baseName, buildVariantKind, compilation)
            add(binary)
            nameToBinary[binary.name] = binary
            // Allow accessing binaries as properties of the container in Groovy DSL.
            if (this is ExtensionAware) {
                extensions.add(binary.name, binary)
            }
        }
    }

    companion object {
        internal fun generateBinaryName(
            name: String,
            buildVariantKind: KotlinJsIrType,
            jsBinaryType: JsBinaryType
        ) =
            lowerCamelCaseName(
                name,
                buildVariantKind.name,
                jsBinaryType.name
            )
    }
}
