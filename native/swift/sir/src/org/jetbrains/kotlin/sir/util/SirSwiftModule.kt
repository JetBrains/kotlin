/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.util

import org.jetbrains.kotlin.sir.*
import org.jetbrains.kotlin.sir.SirVisibility
import org.jetbrains.kotlin.sir.builder.*

/**
 * A module representing the swift standard library
 */
object SirSwiftModule : SirModule() {
    override val imports: MutableList<SirImport> = mutableListOf()

    override val name: String get() = "Swift"
    override val declarations: MutableList<SirDeclaration> = mutableListOf()

    val bool = struct("Bool")

    val int8 = struct("Int8")
    val int16 = struct("Int16")
    val int32 = struct("Int32")
    val int64 = struct("Int64")

    val uint8 = struct("UInt8")
    val uint16 = struct("UInt16")
    val uint32 = struct("UInt32")
    val uint64 = struct("UInt64")

    val double = struct("Double")
    val float = struct("Float")

    val unsafeMutableRawPointer = struct("UnsafeMutableRawPointer")
    val int = struct("Int")

    val void = struct("Void")
    val never = struct("Never")
    val anyHashable = struct("AnyHashable")
    val string = struct("String")

    val array = struct("Array")
    val set = struct("Set")
    val dictionary = struct("Dictionary")
    val range = struct("Range")
    val closedRange = struct("ClosedRange")

    private val unicode = enumStub("Unicode")
    private val utf16 = unicode.enumStub("UTF16")

    private val utf16Extension = extension(SirNominalType(utf16))

    val utf16CodeUnit = utf16Extension.addTypealias("CodeUnit", SirNominalType(utf16))

    val optional: SirEnum = enumStub("Optional")

    val caseIterable = protocol("CaseIterable")
    val losslessStringConvertible = protocol("LosslessStringConvertible")
    val rawRepresentable = protocol("RawRepresentable")
}

private fun SirMutableDeclarationContainer.struct(typeName: String) = addChild {
    buildStruct {
        origin = externallyDefined(typeName)
        visibility = SirVisibility.PUBLIC
        name = typeName
    }
}

private fun SirMutableDeclarationContainer.enumStub(typeName: String) = addChild {
    SirEnumStub(
        origin = externallyDefined(typeName),
        name = typeName
    )
}

private fun SirMutableDeclarationContainer.protocol(typeName: String) = addChild {
    buildProtocol {
        origin = externallyDefined(typeName)
        visibility = SirVisibility.PUBLIC
        name = typeName
    }
}

private fun SirMutableDeclarationContainer.extension(type: SirNominalType) = addChild {
    buildExtension {
        // This doesn't retain information about the extension and its place in the hierarchy,
        // and thus is a bit imprecise, but the result is more readable and consistent with other [SirExtension]s.
        origin = type.typeDeclaration.origin

        visibility = SirVisibility.PUBLIC
        extendedType = type
    }
}

private fun SirMutableDeclarationContainer.addTypealias(name: String, type: SirType) = addChild {
    buildTypealias {
        origin = externallyDefined(name)
        visibility = SirVisibility.PUBLIC
        this.name = name
        this.type = type
    }
}

private fun SirDeclarationParent.externallyDefined(name: String) =
    SirOrigin.ExternallyDefined(swiftFqNameOrNull?.let { "$it.$name" } ?: name)
