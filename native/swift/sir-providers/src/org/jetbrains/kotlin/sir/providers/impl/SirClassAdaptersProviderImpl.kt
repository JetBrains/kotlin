/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.providers.impl

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.sir.SirCallableKind
import org.jetbrains.kotlin.sir.SirClass
import org.jetbrains.kotlin.sir.SirFunction
import org.jetbrains.kotlin.sir.SirFunctionBody
import org.jetbrains.kotlin.sir.SirModule
import org.jetbrains.kotlin.sir.SirNominalType
import org.jetbrains.kotlin.sir.SirOrigin
import org.jetbrains.kotlin.sir.SirParameter
import org.jetbrains.kotlin.sir.SirVisibility
import org.jetbrains.kotlin.sir.builder.buildAttribute
import org.jetbrains.kotlin.sir.builder.buildFunction
import org.jetbrains.kotlin.sir.providers.SirClassAdaptersProvider
import org.jetbrains.kotlin.sir.providers.source.KotlinSource
import org.jetbrains.kotlin.sir.util.SirSwiftModule
import org.jetbrains.kotlin.sir.util.addChild
import org.jetbrains.kotlin.sir.util.exportedInitName
import org.jetbrains.kotlin.sir.util.swiftFqName

public class SirClassAdaptersProviderImpl : SirClassAdaptersProvider {
    private val adapters = mutableListOf<SirFunction>()

    override fun SirClass.generateAdapterDeclarations() {
        val kotlinSourceOrigin = origin as? KotlinSource ?: return
        val functionCName = exportedInitName
        val classFqName = swiftFqName
        adapters.add(buildFunction {
            origin = SirOrigin.ExportedInit(`for` = kotlinSourceOrigin)
            visibility = SirVisibility.PRIVATE
            attributes.add(buildAttribute {
                name = "_cdecl"
                arguments.add("\"$functionCName\"")
            })
            kind = SirCallableKind.FUNCTION
            name = functionCName
            returnType = SirNominalType(SirSwiftModule.mutablePointer)
            parameters.add(SirParameter(argumentName = "externalRCRef", type = SirNominalType(SirSwiftModule.uint)))
            body = SirFunctionBody(
                listOf(
                    "return Unmanaged.passRetained($classFqName(__externalRCRef: externalRCRef)).toOpaque()"
                )
            )
        })
    }

    override fun SirModule.dumpAdapterDeclarations() {
        adapters.forEach {
            addChild { it }
        }
        adapters.clear()
    }
}