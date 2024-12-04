/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tools.dukat.wasm

import org.jetbrains.dukat.idlDeclarations.*
import org.jetbrains.dukat.idlLowerings.IDLLowering

class UnionParametersOverloadsLowering : IDLLowering {
    // Types that are present as union components, but somehow missing IDL declarations. Should be ignored.
    private val typesMissingFromIDL: Set<String> =
        setOf("CSSPseudoElement", "BufferSource")

    override fun lowerInterfaceDeclaration(declaration: IDLInterfaceDeclaration, owner: IDLFileDeclaration): IDLInterfaceDeclaration {
        return declaration.copy(
            operations = declaration.operations.flatMap(::generateOverloads)
        )
    }

    private fun generateOverloads(op: IDLOperationDeclaration): List<IDLOperationDeclaration> {
        val numUnionTypes = op.arguments.count { it.type is IDLUnionTypeDeclaration }
        if (numUnionTypes == 0)
            return listOf(op)

        if (numUnionTypes > 1)
            TODO(
                """
                Not supported in current implementation to keep it simple.
                If updated IDL contains multiple union arguments and this check fails,
                consider updating the algorithm or return listOf(op) as a fallback.
                """
            )

        val unionTypedArgumentIndex = op.arguments.indexOfFirst { it.type is IDLUnionTypeDeclaration }
        val unionTypedArgument = op.arguments[unionTypedArgumentIndex]
        val unionType = unionTypedArgument.type as IDLUnionTypeDeclaration

        var unionMembersFlattened = unionType.unionMembers
        while (unionMembersFlattened.any { it is IDLUnionTypeDeclaration }) {
            unionMembersFlattened = unionMembersFlattened.flatMap {
                if (it is IDLUnionTypeDeclaration) it.unionMembers else listOf(it)
            }
        }

        val result = unionMembersFlattened.mapNotNull { unionMember ->
            if (unionMember.name in typesMissingFromIDL)
                return@mapNotNull null

            op.copy(
                arguments = op.arguments.mapIndexed { argumentIndex, argument: IDLArgumentDeclaration ->
                    if (argumentIndex == unionTypedArgumentIndex) {
                        argument.copy(type = unionMember, defaultValue = null, optional = false)
                    } else {
                        argument.copy()
                    }
                }
            )
        }

        // If union typed argument is optional - create an overload without union argument.
        if (unionTypedArgument.optional || unionTypedArgument.defaultValue != null) {
            return result + op.copy(arguments = op.arguments.subList(0, unionTypedArgumentIndex))
        }

        return result
    }
}

fun IDLSourceSetDeclaration.addOverloadsForUnions(): IDLSourceSetDeclaration {
    return UnionParametersOverloadsLowering().lowerSourceSetDeclaration(this)
}