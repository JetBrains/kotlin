/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalLibraryAbiReader::class)

package org.jetbrains.kotlin.abi.tools.v2.klib

import org.jetbrains.kotlin.abi.tools.api.AbiFilters
import org.jetbrains.kotlin.abi.tools.filtering.compileMatcher
import org.jetbrains.kotlin.library.abi.*
import java.io.File
import java.io.FileNotFoundException


internal fun extractAbiFromKlib(to: Appendable, klibFile: File, filters: AbiFilters) {
    if (!klibFile.exists()) {
        throw FileNotFoundException("File does not exist: ${klibFile.absolutePath}")
    }
    val abiFilters = mutableListOf<AbiReadingFilter>(Filter(filters))

    val library = try {
        LibraryAbiReader.readAbiInfo(klibFile, abiFilters)
    } catch (t: Throwable) {
        throw IllegalStateException("Unable to read klib from ${klibFile.absolutePath}", t)
    }

    val supportedSignatureVersions = library.signatureVersions.asSequence().filter { it.isSupportedByAbiReader }

    val signatureVersion =
        supportedSignatureVersions.maxByOrNull { it.versionNumber } ?: throw IllegalStateException("Can't choose signatureVersion")

    LibraryAbiRenderer.render(
        library, to, AbiRenderingSettings(
            renderedSignatureVersion = signatureVersion,
            renderManifest = true,
            renderDeclarations = true
        )
    )
}

private class Filter(filters: AbiFilters) : AbiReadingFilter {
    val filtersMatcher = compileMatcher(filters)

    override fun isDeclarationExcluded(declaration: AbiDeclaration): Boolean {
        if (filtersMatcher.isEmpty) return false

        if (declaration is AbiFunction || declaration is AbiProperty) {
            if (filtersMatcher.hasAnnotationFilters) {
                var annotationNames = declaration.annotatedWith().names()

                val backingField = (declaration as? AbiProperty)?.backingField
                if (backingField != null) {
                    // add field's annotations
                    annotationNames = annotationNames + backingField.annotatedWith().names()
                }

                if (filtersMatcher.isExcludedByAnnotations(annotationNames)) return true
            }
        }

        if (declaration !is AbiClass) return false

        if (filtersMatcher.hasClassNameFilters && filtersMatcher.isExcludedByName(declaration.qualifiedName.toKotlinQualifiedName())) {
            return true
        }

        if (filtersMatcher.hasAnnotationFilters) {
            val annotationNames = declaration.annotatedWith().names()
            if (filtersMatcher.isExcludedByAnnotations(annotationNames)) return true
        }

        return false
    }

    private fun List<AbiAnnotation>.names(): List<String> = map { annotation -> annotation.qualifiedName.toKotlinQualifiedName() }
}

private fun AbiQualifiedName.toKotlinQualifiedName(): String {
    val packageName = packageName.value
    val className = relativeName.value
    return if (packageName.isNotEmpty()) {
        "$packageName.$className"
    } else {
        className
    }
}
