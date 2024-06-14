/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.native.analysis.api

import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.project.structure.KtLibraryModule
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.ToolingSingleFileKlibResolveStrategy
import org.jetbrains.kotlin.library.metadata.KlibMetadataProtoBuf
import org.jetbrains.kotlin.library.metadata.parseModuleHeader
import org.jetbrains.kotlin.library.metadata.parsePackageFragment
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.getExtensionOrNull
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.util.DummyLogger
import org.jetbrains.kotlin.util.Logger
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.isDirectory

/**
 * Provides a set of [KlibDeclarationAddress] contained within the given [KtLibraryModule] (if the library is based upon a klib file).
 * These addresses will contain all top level declarations such as top level classes, interfaces, objects, ... and
 * top level callables such as functions and properties.
 *
 * Example, given the provided source file
 * ```kotlin
 * package com.example
 * fun topLevelFunction() = 42
 *
 * class Foo {
 *     val foo() = 42
 *     class Bar {
 *         fun bar() = 42
 *     }
 * }
 * ```
 *
 * The read addresses will include `topLevelFunction` as well as `Foo`, as both declarations are top level.
 * These addresses can then be resolved to its given [KaSymbol] by
 *
 * - [KlibClassAddress.getClassOrObjectSymbol]: Resolves to the class symbol or null
 * - [KlibCallableAddress.getCallableSymbols]: Resolves all callable symbols under this given address
 *
 * @return The returned set has a stable order
 */
public fun KtLibraryModule.readKlibDeclarationAddresses(): Set<KlibDeclarationAddress>? {
    val binary = getBinaryRoots().singleOrNull() ?: return null
    if (!(binary.extension == "klib" || binary.isDirectory())) return null
    return readKlibDeclarationAddresses(binary)
}

internal fun readKlibDeclarationAddresses(path: Path, logger: Logger = DummyLogger): Set<KlibDeclarationAddress>? {
    val library = ToolingSingleFileKlibResolveStrategy.tryResolve(File(path), logger) ?: return null
    return readKlibDeclarationAddresses(library)
}

internal fun readKlibDeclarationAddresses(library: KotlinLibrary): Set<KlibDeclarationAddress> {
    val headerProto = parseModuleHeader(library.moduleHeaderData)

    val packageMetadataSequence = headerProto.packageFragmentNameList.asSequence().flatMap { packageFragmentName ->
        library.packageMetadataParts(packageFragmentName).asSequence().map { packageMetadataPart ->
            library.packageMetadata(packageFragmentName, packageMetadataPart)
        }
    }

    return packageMetadataSequence.flatMap { packageMetadata ->
        val packageFragmentProto = parsePackageFragment(packageMetadata)

        with(PackageFragmentReadingContext(library, packageFragmentProto) ?: return@flatMap emptySet()) {
            packageFragmentProto.readKlibClassAddresses() +
                    packageFragmentProto.readKlibTypeAliasAddresses() +
                    packageFragmentProto.readKlibPropertyAddresses() +
                    packageFragmentProto.readKlibFunctionAddresses()
        }
    }.toSet()

}

context(PackageFragmentReadingContext)
internal fun ProtoBuf.PackageFragment.readKlibClassAddresses(): Set<KlibClassAddress> {
    return class_List.mapNotNull { classProto ->
        val classId = ClassId.fromString(nameResolver.getQualifiedClassName(classProto.fqName))
        if (classId.isNestedClass) return@mapNotNull null
        KlibClassAddress(
            libraryPath = libraryPath,
            packageFqName = packageFqName,
            sourceFileName = classProto.getExtensionOrNull(KlibMetadataProtoBuf.classFile)?.let { fileNameId ->
                nameResolver.strings.getString(fileNameId)
            },
            classId = classId
        )
    }.toSet()
}

context(PackageFragmentReadingContext)
internal fun ProtoBuf.PackageFragment.readKlibTypeAliasAddresses(): Set<KlibTypeAliasAddress> {
    return this.`package`.typeAliasList.map { typeAliasProto ->
        val name = Name.identifier(nameResolver.getString(typeAliasProto.name))
        KlibTypeAliasAddress(
            libraryPath = libraryPath,
            packageFqName = packageFqName,
            classId = ClassId(packageFqName, name)
        )
    }.toSet()
}

context(PackageFragmentReadingContext)
internal fun ProtoBuf.PackageFragment.readKlibPropertyAddresses(): Set<KlibPropertyAddress> {
    return `package`.propertyList.map { propertyProto ->
        KlibPropertyAddress(
            libraryPath = libraryPath,
            sourceFileName = propertyProto.getExtensionOrNull(KlibMetadataProtoBuf.propertyFile)?.let { fileNameId ->
                nameResolver.strings.getString(fileNameId)
            },
            packageFqName = packageFqName,
            callableName = Name.identifier(nameResolver.getString(propertyProto.name))
        )
    }.toSet()
}

context(PackageFragmentReadingContext)
internal fun ProtoBuf.PackageFragment.readKlibFunctionAddresses(): Set<KlibFunctionAddress> {
    return `package`.functionList.map { functionProto ->
        KlibFunctionAddress(
            libraryPath = libraryPath,
            sourceFileName = functionProto.getExtensionOrNull(KlibMetadataProtoBuf.functionFile)?.let { fileNameId ->
                nameResolver.getString(fileNameId)
            },
            packageFqName = packageFqName,
            callableName = Name.identifier(nameResolver.getString(functionProto.name))
        )
    }.toSet()
}
