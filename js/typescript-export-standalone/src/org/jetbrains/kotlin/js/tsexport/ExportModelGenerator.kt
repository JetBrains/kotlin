
/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(KaContextParameterApi::class, KaExperimentalApi::class)

package org.jetbrains.kotlin.js.tsexport

import org.jetbrains.kotlin.analysis.api.KaContextParameterApi
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaNonPublicApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.components.containingDeclaration
import org.jetbrains.kotlin.analysis.api.components.isNullable
import org.jetbrains.kotlin.analysis.api.components.klibSourceFileName
import org.jetbrains.kotlin.analysis.api.klib.reader.getAllDeclarations
import org.jetbrains.kotlin.analysis.api.projectStructure.KaLibraryModule
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.ir.backend.js.tsexport.*
import org.jetbrains.kotlin.ir.backend.js.tsexport.ExportedType.Primitive
import org.jetbrains.kotlin.js.common.makeValidES5Identifier
import org.jetbrains.kotlin.js.common.safeModuleName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import org.jetbrains.kotlin.utils.compactIfPossible
import org.jetbrains.kotlin.utils.memoryOptimizedMap

internal class ExportModelGenerator(private val config: TypeScriptExportConfig) {
    context(_: KaSession)
    fun generateExport(library: KaLibraryModule, config: TypeScriptModuleConfig): ProcessedModule {
        // TODO: Collect implicitly exported declarations, see ImplicitlyExportedDeclarationsMarkingLowering
        val fileMap = buildMap {
            for (declaration in library.getAllDeclarations()) {
                val packageFqName = when (declaration) {
                    is KaClassLikeSymbol -> declaration.classId!!.packageFqName
                    is KaCallableSymbol -> declaration.callableId!!.packageName
                    else -> error("Unexpected declaration kind: $declaration")
                }

                // TODO(KT-82224): Respect @JsFileName
                @OptIn(KaNonPublicApi::class)
                val fileName = declaration.klibSourceFileName ?: continue

                val key = FileArtifactKey(packageFqName, fileName)
                computeIfAbsent(key) { _ -> mutableListOf() }.addIfNotNull(
                    exportTopLevelDeclaration(declaration)
                )
            }
        }

        return ProcessedModule(
            library,
            fileMap.mapValues { (key, exports) ->
                when {
                    exports.isEmpty() -> emptyList()
                    !this.config.generateNamespacesForPackages || key.packageFqName.isRoot -> exports.compactIfPossible()
                    else -> listOf(ExportedNamespace(key.packageFqName.asString(), exports.compactIfPossible()))
                }
            },
            jsOutputName = config.outputName ?: library.libraryName.safeModuleName,
        )
    }

    context(_: KaSession)
    private fun exportTopLevelDeclaration(declaration: KaDeclarationSymbol): ExportedDeclaration? {
        if (!shouldDeclarationBeExportedImplicitlyOrExplicitly(declaration)) return null

        return when (declaration) {
            is KaNamedFunctionSymbol -> exportFunction(declaration, parent = null)
            is KaPropertySymbol -> exportProperty(declaration, parent = null)
            is KaClassSymbol -> ErrorDeclaration("Class declarations are not implemented yet")
            is KaTypeAliasSymbol -> ErrorDeclaration("Type alias declarations are not implemented yet")
            else -> null
        }
    }

    context(_: KaSession)
    private fun exportFunction(function: KaNamedFunctionSymbol, parent: KaDeclarationSymbol?): ExportedDeclaration? =
        when (val exportability = functionExportability(function)) {
            is Exportability.NotNeeded, is Exportability.Implicit -> null
            is Exportability.Prohibited -> ErrorDeclaration(exportability.reason)
            is Exportability.Allowed -> {
                val returnType = if (function.isSuspend) {
                    ExportedType.ClassType(
                        name = "Promise",
                        arguments = listOf(exportType(function.returnType))
                    )
                } else {
                    exportType(function.returnType)
                }
                ExportedFunction(
                    name = function.getJsSymbolForOverriddenDeclaration()?.let(ExportedMemberName::WellKnownSymbol)
                        ?: ExportedMemberName.Identifier(function.getExportedIdentifier()),
                    returnType = returnType,
                    parameters = exportFunctionParameters(function),
                    typeParameters = function.typeParameters.memoryOptimizedMap { exportTypeParameter(it) },
                    isMember = parent is KaClassSymbol,
                    isStatic = false, // TODO: isEs6ConstructorReplacement || isStaticMethodOfClass
                    isAbstract = parent is KaClassSymbol && parent.classKind != KaClassKind.INTERFACE && function.modality == KaSymbolModality.ABSTRACT,
                    isProtected = function.visibility == KaSymbolVisibility.PROTECTED,
                )
            }
        }

    private fun sanitizeName(parameterName: Name): String {
        // Parameter names do not matter in d.ts files. They can be renamed as we like
        var sanitizedName = makeValidES5Identifier(parameterName.asString(), withHash = false)
        if (sanitizedName in allReservedWords)
            sanitizedName = "_$sanitizedName"
        return sanitizedName
    }

    context(_: KaSession)
    private fun exportFunctionParameters(function: KaFunctionSymbol): List<ExportedParameter> {
        return buildList {
            for (parameter in function.contextParameters) {
                add(ExportedParameter(sanitizeName(parameter.name), exportType(parameter.returnType)))
            }
            function.receiverParameter?.let {
                add(ExportedParameter(sanitizeName(SpecialNames.THIS), exportType(it.returnType)))
            }
            for (parameter in function.valueParameters) {
                val type = if (parameter.isVararg) {
                    TypeExporter(config).exportSpecializedArrayWithElementType(parameter.returnType)
                } else {
                    exportType(parameter.returnType)
                }
                add(ExportedParameter(sanitizeName(parameter.name), type, parameter.hasDefaultValue))
            }
        }
    }

    context(_: KaSession)
    private fun exportProperty(property: KaPropertySymbol, parent: KaDeclarationSymbol?): ExportedDeclaration? {
        // Frontend will report an error on an attempt to export an extension property.
        // Just to be safe, filter out such properties here as well.
        if (property.receiverType != null) {
            return null
        }
        val parentClass = parent as? KaClassSymbol
        val isMember = parentClass != null
        val isStatic = property.isStatic
        val isObjectGetter = false  // TODO: Should be true for getInstance functions of objects
        val isOptional = property.isExternal && isMember && property.returnType.isNullable
        val shouldBeExportedAsObjectWithAccessorsInside = !config.generateNamespacesForPackages && !isMember && !isStatic

        val propertyType = when {
            !shouldBeExportedAsObjectWithAccessorsInside -> exportType(property.returnType)
            isObjectGetter -> ExportedType.InlineInterfaceType(
                listOf(
                    ExportedFunction(
                        name = ExportedMemberName.Identifier("getInstance"),
                        returnType = exportType(property.returnType),
                        parameters = emptyList(),
                        isMember = true,
                        isProtected = false
                    )
                )
            )
            else -> // TODO: add correct default implementations processing
                ExportedType.InlineInterfaceType(
                    listOfNotNull(
                        ExportedFunction(
                            name = ExportedMemberName.Identifier("get"),
                            returnType = exportType(property.getter?.returnType ?: property.returnType),
                            parameters = emptyList(),
                            isMember = true,
                            isProtected = false
                        ),
                        runIf(!property.isVal) {
                            ExportedFunction(
                                name = ExportedMemberName.Identifier("set"),
                                returnType = Primitive.Unit,
                                parameters = listOf(
                                    property.setter?.parameter?.let {
                                        ExportedParameter(sanitizeName(it.name), exportType(it.returnType))
                                    } ?: ExportedParameter("value", exportType(property.returnType))
                                ),
                                isMember = true,
                                isProtected = false
                            )
                        }
                    )
                )
        }

        return ExportedProperty(
            name = ExportedMemberName.Identifier(property.getExportedIdentifier()),
            type = propertyType,
            mutable = !property.isVal,
            isMember = parentClass != null,
            isStatic = property.isStatic,
            isAbstract = parentClass?.classKind != KaClassKind.INTERFACE && property.modality == KaSymbolModality.ABSTRACT,
            isProtected = property.visibility == KaSymbolVisibility.PROTECTED,
            isField = parentClass?.classKind == KaClassKind.INTERFACE,
            isObjectGetter = isObjectGetter,
            isOptional = isOptional,
        )
    }

    context(_: KaSession)
    private fun exportType(type: KaType): ExportedType = TypeExporter(config).exportType(type)

    context(_: KaSession)
    private fun exportTypeParameter(typeParameter: KaTypeParameterSymbol): ExportedTypeParameter {
        val constraints = typeParameter.upperBounds
            .mapNotNull {
                val exportedType = exportType(it)
                if (exportedType is ExportedType.ErrorType) return@mapNotNull null
                if (exportedType is ExportedType.ImplicitlyExportedType && exportedType.exportedSupertype == Primitive.Any) {
                    exportedType.copy(exportedSupertype = Primitive.Unknown)
                } else {
                    exportedType
                }
            }

        return ExportedTypeParameter(
            name = typeParameter.name.identifier,
            constraint = when (constraints.size) {
                0 -> null
                1 -> constraints[0]
                else -> constraints.reduce(ExportedType::IntersectionType)
            }
        )
    }

    context(_: KaSession)
    private fun functionExportability(function: KaNamedFunctionSymbol): Exportability {
        if (function.isInline && function.typeParameters.any { it.isReified })
            return Exportability.Prohibited("Inline reified function")

        val parentClass = function.containingDeclaration as? KaClassSymbol

        // TODO: Use [] syntax instead of prohibiting
        val name = function.getExportedIdentifier()
        if (parentClass == null && name in allReservedWords) {
            return Exportability.Prohibited("Name is a reserved word")
        }

        return Exportability.Allowed
    }
}
