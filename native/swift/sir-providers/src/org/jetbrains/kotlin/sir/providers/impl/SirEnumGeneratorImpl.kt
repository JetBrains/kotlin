/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.providers.impl

import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.sir.*
import org.jetbrains.kotlin.sir.providers.SirEnumGenerator
import org.jetbrains.kotlin.sir.util.addChild

public class SirEnumGeneratorImpl(
    private val moduleForEnums: SirModule
) : SirEnumGenerator {

    private val createdEnums: MutableMap<FqName, SirEnumStub> = mutableMapOf()

    override val collectedPackages: Set<FqName>
        get() = createdEnums.keys

    override fun FqName.sirPackageEnum(): SirEnum {
        return sirPackageEnumStub()
    }

    private fun FqName.sirPackageEnumStub(): SirEnumStub {
        require(!this.isRoot)

        val parent: SirMutableDeclarationContainer = if (parent().isRoot) {
            moduleForEnums
        } else {
            parent().sirPackageEnumStub()
        }

        return createEnum(this, parent)
    }

    private fun createEnum(fqName: FqName, parent: SirMutableDeclarationContainer): SirEnumStub = createdEnums.getOrPut(fqName) {
        val enumToCreateName = fqName.pathSegments().last().asString()
        parent.declarations
            .filterIsInstance<SirEnumStub>().find { it.name == enumToCreateName }
            ?: parent.addChild {
                SirEnumStub(
                    origin = SirOrigin.Namespace(fqName.pathSegments().map { it.asString() }),
                    name = enumToCreateName
                )
            }
    }
}
