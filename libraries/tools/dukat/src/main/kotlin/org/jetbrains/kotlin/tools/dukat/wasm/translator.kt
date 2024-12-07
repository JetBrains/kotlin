/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tools.dukat.wasm

import org.jetbrains.dukat.astCommon.IdentifierEntity
import org.jetbrains.dukat.astCommon.NameEntity
import org.jetbrains.dukat.astModel.*
import org.jetbrains.dukat.commonLowerings.AddExplicitGettersAndSetters
import org.jetbrains.dukat.idlLowerings.*
import org.jetbrains.dukat.idlParser.parseIDL
import org.jetbrains.dukat.idlReferenceResolver.DirectoryReferencesResolver
import org.jetbrains.dukat.model.commonLowerings.*
import org.jetbrains.dukat.ownerContext.NodeOwner

fun translateIdlToSourceSet(fileName: String): SourceSetModel {
    val translationContext = TranslationContext()
    return parseIDL(fileName, DirectoryReferencesResolver())
        .voidifyEventHandlerReturnType()
        .resolvePartials()
        .addConstructors()
        .resolveTypedefs()
        .specifyEventHandlerTypes()
        .specifyDefaultValues()
        .resolveImplementsStatements()
        .resolveMixins()
        .addItemArrayLike()
        .resolveTypesKeepingUnions()
        .addOverloadsForUnions()
        .markAbstractOrOpen()
        .addMissingMembers()
        .addOverloadsForCallbacks()
        .convertToWasmModel()
        .lower(
            ModelContextAwareLowering(translationContext),
            LowerOverrides(translationContext),
            EscapeIdentificators(),
            AddExplicitGettersAndSetters(),
        )
        .lower(ReplaceDynamics())  // Wasm-specific
        .addKDocs()
        .relocateDeclarations()
        .addImportsForUsedPackages()
        .omitStdLib()
        .lower(WasmPostProcessingHacks())
}

class WasmPostProcessingHacks : TopLevelModelLowering {
    private fun fineItemMethodThatOverridersItemArrayLikeOrNull(klass: ClassLikeModel): MethodModel? {
        if (klass.parentEntities.none { it.value.value == IdentifierEntity("ItemArrayLike") })
            return null

        return klass.members.filterIsInstance<MethodModel>().find { member ->
            member.name == IdentifierEntity("item") &&
                    (member.parameters.firstOrNull()?.type as? TypeValueModel)?.value == IdentifierEntity("Int")
        }
    }

    override fun lowerClassLikeModel(ownerContext: NodeOwner<ClassLikeModel>, parentModule: ModuleModel): ClassLikeModel {
        val klass: ClassLikeModel = super.lowerClassLikeModel(ownerContext, parentModule)
        val itemMethodThatOverridesAny: MethodModel = fineItemMethodThatOverridersItemArrayLikeOrNull(klass)
            ?: return klass

        fun translateMember(member: MemberModel): MemberModel {
            if (member !== itemMethodThatOverridesAny) return member

            val newTypeIdentifier = when ((member.type as? TypeValueModel)?.value) {
                IdentifierEntity("String") -> IdentifierEntity("JsString")
                else -> null
            }

            val newType = if (newTypeIdentifier != null) {
                TypeValueModel(
                    IdentifierEntity("JsString"),
                    fqName = null,
                    metaDescription = null,
                    nullable = member.type.nullable,
                    params = emptyList()
                )
            } else {
                member.type
            }

            return member.copy(
                type = newType,
                override = listOf<NameEntity>(IdentifierEntity("item"))
            )
        }

        return when(klass) {
            is ClassModel -> klass.copy(members = klass.members.map(::translateMember))
            is InterfaceModel -> klass.copy(members = klass.members.map(::translateMember))
            is ObjectModel -> klass.copy(members = klass.members.map(::translateMember))
            else -> error("Unknown ClassLikeModel: ${klass::class}")
        }
    }
}
