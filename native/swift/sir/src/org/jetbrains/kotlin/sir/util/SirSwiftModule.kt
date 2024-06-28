/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.util

import org.jetbrains.kotlin.sir.*
import org.jetbrains.kotlin.sir.builder.buildStruct

/**
 * A module representing the swift standard library
 */
object SirSwiftModule : SirModule() {
    override val imports: MutableList<SirImport> = mutableListOf()

    override val name: String get() = "Swift"

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

    override var declarations: MutableList<SirDeclaration> = mutableListOf(
        void,
        never,

        bool,

        int8,
        int16,
        int32,
        int64,

        uint8,
        uint16,
        uint32,
        uint64,

        double,
        float,

        uint,
    )
}

private fun struct(typeName: String) = buildStruct {
    origin = SirOrigin.ExternallyDefined("Swift.$typeName")
    visibility = SirVisibility.PUBLIC
    name = typeName
}.also { it.parent = SirSwiftModule }
