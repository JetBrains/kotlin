/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.util

import org.jetbrains.kotlin.sir.SirDeclaration
import org.jetbrains.kotlin.sir.SirModule
import org.jetbrains.kotlin.sir.SirOrigin
import org.jetbrains.kotlin.sir.SirVisibility
import org.jetbrains.kotlin.sir.builder.buildStruct
import org.jetbrains.kotlin.sir.visitors.SirTransformer
import org.jetbrains.kotlin.sir.visitors.SirVisitor

/**
 * A module representing the swift standard library
 */
object SirSwiftModule : SirModule() {
    override val name: String get() = "Swift"
    override val declarations: List<SirDeclaration> by lazy {
        listOf(
            void,

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
        )
    }

    val bool = primitive("Bool")

    val int8 = primitive("Int8")
    val int16 = primitive("Int16")
    val int32 = primitive("Int32")
    val int64 = primitive("Int64")

    val uint8 = primitive("UInt8")
    val uint16 = primitive("UInt16")
    val uint32 = primitive("UInt32")
    val uint64 = primitive("UInt64")

    val double = primitive("Double")
    val float = primitive("Float")

    val void = buildStruct {
        origin = SirOrigin.ExternallyDefined(name = "Swift.Void")
        visibility = SirVisibility.PUBLIC
        name = "Void"
    }.also { it.parent = SirSwiftModule }

    override fun <R, D> acceptChildren(visitor: SirVisitor<R, D>, data: D) {
        declarations.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: SirTransformer<D>, data: D) {}
}

private fun primitive(typeName: String) = buildStruct {
    origin = SirOrigin.ExternallyDefined("Swift.$typeName")
    visibility = SirVisibility.PUBLIC
    name = typeName
}.also { it.parent = SirSwiftModule }
