/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.util

import org.jetbrains.kotlin.sir.*
import org.jetbrains.kotlin.sir.builder.buildEnum
import org.jetbrains.kotlin.sir.builder.buildExtension
import org.jetbrains.kotlin.sir.builder.buildStruct
import org.jetbrains.kotlin.sir.builder.buildTypealias

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

    val uint = struct("UInt")

    val void = struct("Void")
    val never = struct("Never")
    val string = struct("String")

    private val unicode = enum("Unicode")
    private val utf16 = unicode.enum("UTF16")

    private val utf16Extension = extension(SirNominalType(utf16))

    val utf16CodeUnit = utf16Extension.addTypealias("CodeUnit", SirNominalType(utf16))
}

private fun SirMutableDeclarationContainer.struct(typeName: String) = addChild {
    buildStruct {
        origin = externallyDefined(typeName)
        visibility = SirVisibility.PUBLIC
        name = typeName
    }
}

private fun SirMutableDeclarationContainer.enum(typeName: String) = addChild {
    buildEnum {
        origin = externallyDefined(typeName)
        visibility = SirVisibility.PUBLIC
        name = typeName
    }
}

private fun SirMutableDeclarationContainer.extension(type: SirNominalType) = addChild {
    buildExtension {
        // This doesn't retain information about the extension and its place in the hierarchy,
        // and thus is a bit imprecise, but the result is more readable and consistent with other [SirExtension]s.
        origin = type.type.origin

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
