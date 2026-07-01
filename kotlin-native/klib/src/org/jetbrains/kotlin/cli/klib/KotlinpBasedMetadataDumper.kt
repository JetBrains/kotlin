/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.klib

import kotlinx.metadata.klib.KlibMetadataVersion
import kotlinx.metadata.klib.KlibModuleMetadata
import kotlinx.metadata.klib.className
import kotlinx.metadata.klib.fqName
import org.jetbrains.kotlin.kotlinp.Printer
import org.jetbrains.kotlin.kotlinp.Settings
import org.jetbrains.kotlin.kotlinp.klib.*
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.components.metadata
import org.jetbrains.kotlin.library.metadataVersion

internal class KotlinpBasedMetadataDumper(output: KlibToolOutput) {

    private val printer = Printer(output)

    /**
     * @param testMode if `true` then a special pre-processing is performed towards the metadata before rendering:
     *        - empty package fragments are removed
     *        - package fragments with the same package FQN are merged
     *        - classes are sorted in alphabetical order
     */
    fun dumpLibrary(library: KotlinLibrary, testMode: Boolean) {
        val moduleMetadata = loadModuleMetadata(library)
            .let { originalModuleMetadata -> if (testMode) preprocessMetadataForTests(originalModuleMetadata) else originalModuleMetadata }

        KlibKotlinp(
                settings = Settings(isVerbose = true, sortDeclarations = testMode),
                signatureComputer = null,
        ).renderModule(moduleMetadata, printer)
    }

    private fun preprocessMetadataForTests(originalModuleMetadata: KlibModuleMetadata) = KlibModuleMetadata(
        name = originalModuleMetadata.name,
        fragments = originalModuleMetadata.fragments.groupBy { it.fqName }.mapNotNull { [packageFqName, fragments] ->
            val classNames = fragments.flatMap { it.className }.sorted()
            val classes = fragments.flatMap { it.classes }.sortedBy { it.name }
            val functions = fragments.flatMap { it.pkg?.functions.orEmpty() }.sortedBy { it.sortingKey() }
            val properties = fragments.flatMap { it.pkg?.properties.orEmpty() }.sortedBy { it.sortingKey() }
            val typeAliases = fragments.flatMap { it.pkg?.typeAliases.orEmpty() }.sortedBy { it.name }

            if (classNames.isEmpty() && classes.isEmpty() && functions.isEmpty() && properties.isEmpty() && typeAliases.isEmpty())
                return@mapNotNull null

            classes.forEach { clazz ->
                clazz.constructors.sortBy { it.sortingKey() }
                clazz.functions.sortBy { it.sortingKey() }
                clazz.properties.sortBy { it.sortingKey() }
                clazz.typeAliases.sortBy { it.name }
            }

            KmModuleFragment().apply {
                this.fqName = packageFqName
                this.className += classNames
                this.classes += classes

                if (functions.isNotEmpty() || properties.isNotEmpty() || typeAliases.isNotEmpty()) {
                    this.pkg = KmPackage().apply {
                        this.functions += functions
                        this.properties += properties
                        this.typeAliases += typeAliases
                    }
                }
            }
        }.sortedBy { it.fqName.orEmpty() },
        metadataVersion = originalModuleMetadata.metadataVersion,
    )

    private fun loadModuleMetadata(library: KotlinLibrary) = KlibModuleMetadata.readLenient(
        object : KlibModuleMetadata.MetadataLibraryProvider {
            private val metadata = library.metadata
            override val moduleHeaderData get() = metadata.moduleHeaderData
            override val metadataVersion = KlibMetadataVersion((library.metadataVersion?.toArray()
                    ?: error("No metadata version specified in ${library.path}")))
            override fun packageMetadata(fqName: String, partName: String) = metadata.getPackageFragment(fqName, partName)
            override fun packageMetadataParts(fqName: String) = metadata.getPackageFragmentNames(fqName)
        }
    )

    private fun KmConstructor.sortingKey() = constructorId("")
    private fun KmFunction.sortingKey() = functionId("")
    private fun KmProperty.sortingKey() = propertyId("")
}
