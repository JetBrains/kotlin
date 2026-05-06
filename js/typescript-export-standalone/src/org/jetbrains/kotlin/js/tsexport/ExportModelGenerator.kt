/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(KaContextParameterApi::class, KaExperimentalApi::class)

package org.jetbrains.kotlin.js.tsexport

import org.jetbrains.kotlin.analysis.api.KaContextParameterApi
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaNonPublicApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.components.*
import org.jetbrains.kotlin.analysis.api.klib.reader.getAllDeclarations
import org.jetbrains.kotlin.analysis.api.projectStructure.KaLibraryModule
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.ir.backend.js.tsexport.*
import org.jetbrains.kotlin.js.common.makeValidES5Identifier
import org.jetbrains.kotlin.js.common.safeModuleName
import org.jetbrains.kotlin.js.config.ModuleKind
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.resolve.DataClassResolver
import org.jetbrains.kotlin.util.ImplementationStatus
import org.jetbrains.kotlin.utils.*
import org.jetbrains.kotlin.utils.addToStdlib.runIf

public const val EXTENSION_RECEIVER_NAME: String = "_this_"

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
                computeIfAbsent(key) { _ -> mutableListOf() }.addAll(exportTopLevelDeclaration(declaration))
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
    private fun exportTopLevelDeclaration(declaration: KaDeclarationSymbol): List<ExportedDeclaration> {
        if (!shouldDeclarationBeExportedImplicitlyOrExplicitly(declaration)) return emptyList()

        return when (declaration) {
            is KaNamedFunctionSymbol -> listOfNotNull(exportFunction(declaration, parent = null, classTypeParameterScope = emptyMap()))
            is KaPropertySymbol -> exportProperty(declaration, parent = null, classTypeParameterScope = emptyMap())
            is KaNamedClassSymbol -> listOfNotNull(exportClass(declaration, parent = null, outerClassTypeParameterScope = emptyMap()))
            is KaTypeAliasSymbol -> listOf(ErrorDeclaration("Type alias declarations are not implemented yet"))
            else -> emptyList()
        }
    }

    context(_: KaSession)
    private fun exportClass(
        klass: KaNamedClassSymbol,
        parent: KaDeclarationSymbol?,
        outerClassTypeParameterScope: TypeParameterScope,
    ): ExportedClass? {
        val superTypes = klass.superTypes // TODO: Collect supertype transitive hierarchy

        when (val exportability = classExportability(klass, parent)) {
            is Exportability.Prohibited -> ErrorDeclaration(exportability.reason)
            Exportability.NotNeeded -> return null
            Exportability.Implicit -> return exportDeclarationImplicitly(klass, superTypes)
            Exportability.Allowed -> {}
        }

        val typeParameterScope = TypeParameterScope(
            container = klass,
            config = config,
            outerScope = if (klass.isInner) outerClassTypeParameterScope else emptyMap(),
            renameOuterTypeParameters = true,
        )

        val (members, nestedClasses) = exportClassMembers(klass, superTypes, typeParameterScope)

        val superClasses = superTypes
            .filter {
                val expandedSymbol = it.expandedSymbol ?: return@filter false
                expandedSymbol.classKind != KaClassKind.INTERFACE
                        && !it.isAnyType
                        && !it.isClassType(StandardClassIds.Enum)
                        && !expandedSymbol.isJsImplicitExport()
            }
            .map { exportType(it, typeParameterScope, shouldCalculateExportedSupertypeForImplicit = false) }
            .memoryOptimizedFilter { it !is ExportedType.ErrorType }
        val superInterfaces = superTypes
            .filter {
                val expandedSymbol = it.expandedSymbol ?: return@filter false
                expandedSymbol.classKind == KaClassKind.INTERFACE || expandedSymbol.isJsImplicitExport()
            }
            .map { exportType(it, typeParameterScope, shouldCalculateExportedSupertypeForImplicit = false) }
            .memoryOptimizedFilter { it !is ExportedType.ErrorType }

        val name = klass.getExportedIdentifier()
        return if (klass.classKind == KaClassKind.OBJECT || klass.classKind == KaClassKind.COMPANION_OBJECT) {
            ExportedObject(
                name = name,
                superClasses = superClasses,
                superInterfaces = superInterfaces,
                members = members,
                nestedClasses = nestedClasses,
                originalClassId = klass.classId,
                isExternal = klass.isExternal,
                isCompanion = klass.classKind == KaClassKind.COMPANION_OBJECT,
                isTopLevel = parent == null,
            )
        } else {
            ExportedRegularClass(
                name = name,
                isInterface = klass.classKind == KaClassKind.INTERFACE,
                isAbstract = klass.modality == KaSymbolModality.ABSTRACT
                        || klass.modality == KaSymbolModality.SEALED
                        || klass.classKind == KaClassKind.ENUM_CLASS,
                superClasses = superClasses,
                superInterfaces = superInterfaces,
                typeParameters = typeParameterScope.values.toList(),
                members = members,
                nestedClasses = nestedClasses,
                originalClassId = klass.classId,
                isExternal = klass.isExternal,
            )
        }.withAttributes(klass)
    }

    context(_: KaSession)
    private fun exportFunction(
        function: KaNamedFunctionSymbol,
        parent: KaDeclarationSymbol?,
        classTypeParameterScope: TypeParameterScope,
        isFactoryPropertyForInnerClass: Boolean = false,
        isExportedDefaultImplementation: Boolean = false,
    ): ExportedDeclaration? =
        when (val exportability = functionExportability(function, parent)) {
            is Exportability.NotNeeded, is Exportability.Implicit -> null
            is Exportability.Prohibited -> ErrorDeclaration(exportability.reason)
            is Exportability.Allowed -> {
                val isStatic = function.isStatic || function.isJsStatic()
                val inlineClassesShouldBeUnboxed = function.isExternal
                val outerScope = if (!isStatic || isFactoryPropertyForInnerClass) classTypeParameterScope else emptyMap()
                val functionTypeParameterScope = TypeParameterScope(function, config, outerScope)
                val typeParameters = if (isExportedDefaultImplementation) {
                    (parent as KaNamedClassSymbol).typeParameters + function.typeParameters
                } else {
                    function.typeParameters
                }
                val returnType =
                    exportType(function.returnType, functionTypeParameterScope, inlineClassesShouldBeUnboxed = inlineClassesShouldBeUnboxed)
                ExportedFunction(
                    name = function.getJsSymbolForOverriddenDeclaration()?.let(ExportedMemberName::WellKnownSymbol)
                        ?: ExportedMemberName.Identifier(function.getExportedIdentifier()),
                    returnType = if (function.isSuspend) {
                        ExportedType.ClassType(name = FqName("Promise"), arguments = listOf(returnType))
                    } else {
                        returnType
                    },
                    parameters = exportFunctionParameters(
                        function,
                        parent,
                        functionTypeParameterScope,
                        inlineClassesShouldBeUnboxed,
                        isExportedDefaultImplementation = isExportedDefaultImplementation,
                    ),
                    typeParameters = typeParameters.memoryOptimizedMap { functionTypeParameterScope[it]!! },
                    isMember = parent is KaClassSymbol && !isExportedDefaultImplementation,
                    isStatic = isStatic,
                    isAbstract = parent is KaClassSymbol && parent.classKind != KaClassKind.INTERFACE && function.modality == KaSymbolModality.ABSTRACT,
                    isProtected = function.visibility == KaSymbolVisibility.PROTECTED,
                ).withAttributes(function)
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
    private fun exportConstructor(
        constructor: KaConstructorSymbol,
        constructedClass: KaNamedClassSymbol,
        typeParameterScope: TypeParameterScope,
        isFactoryPropertyForInnerClass: Boolean = false,
    ): ExportedDeclaration? {
        val visibility = when {
            constructedClass.isInner && !isFactoryPropertyForInnerClass -> when (constructedClass.modality) {
                // Inner classes should be constructed as `new outerClassValue.Inner()`
                // in JavaScript instead of `new OuterClass.Inner(outerClassValue)`.
                // The only time when you might actually want to call the real inner class
                // constructor is when you're inheriting from it.
                KaSymbolModality.SEALED, KaSymbolModality.FINAL -> ExportedVisibility.PRIVATE
                KaSymbolModality.ABSTRACT, KaSymbolModality.OPEN -> ExportedVisibility.PROTECTED
            }
            else -> constructor.exportedVisibility(constructedClass)
        }
        if (isFactoryPropertyForInnerClass && visibility != ExportedVisibility.DEFAULT) return null

        val inlineClassesShouldBeUnboxed = constructedClass.isExternal

        val parameters = if (constructor.isPrimary && visibility == ExportedVisibility.PRIVATE) {
            emptyList()
        } else {
            exportFunctionParameters(
                constructor,
                constructedClass,
                typeParameterScope,
                inlineClassesShouldBeUnboxed,
                isFactoryPropertyForInnerClass
            )
        }

        fun returnType() = ExportedType.ClassType(
            name = constructedClass.getExportedFqName(config.generateNamespacesForPackages, config),
            arguments = typeParameterScope.values.map(ExportedType::TypeParameterRef),
        )

        if (constructor.isPrimary) {
            return if (isFactoryPropertyForInnerClass) {
                ExportedConstructSignature(
                    parameters = parameters,
                    returnType = returnType(),
                    typeParameters = constructedClass.typeParameters.memoryOptimizedMap { typeParameterScope[it]!! },
                    isProtected = false,
                )
            } else {
                ExportedConstructor(parameters, visibility)
            }.withAttributes(constructor)
        }

        if (visibility == ExportedVisibility.PRIVATE || (constructedClass.isInner && !isFactoryPropertyForInnerClass)) return null

        val jsName = constructor.getJsNameForOverriddenDeclaration() ?: return null
        return ExportedFunction(
            name = ExportedMemberName.Identifier(jsName),
            returnType = returnType(),
            parameters = parameters,
            typeParameters = constructedClass.typeParameters.memoryOptimizedMap { typeParameterScope[it]!! },
            isMember = true,
            isStatic = !isFactoryPropertyForInnerClass,
            isProtected = constructor.visibility == KaSymbolVisibility.PROTECTED,
        ).withAttributes(constructor)
    }

    context(_: KaSession)
    private fun exportFunctionParameters(
        function: KaFunctionSymbol,
        parent: KaDeclarationSymbol?,
        functionTypeParameterScope: TypeParameterScope,
        inlineClassesShouldBeUnboxed: Boolean,
        inFactoryPropertyForInnerClass: Boolean = false,
        isExportedDefaultImplementation: Boolean = false,
    ): List<ExportedParameter> {
        return buildList {
            if (!inFactoryPropertyForInnerClass
                && function is KaConstructorSymbol
                && parent is KaNamedClassSymbol
                && function.isPrimary
                && parent.isInner
            ) {
                add(
                    ExportedParameter(
                        $$"$outer",
                        exportType(
                            (parent.containingDeclaration as KaNamedClassSymbol).defaultType,
                            functionTypeParameterScope,
                            inlineClassesShouldBeUnboxed = inlineClassesShouldBeUnboxed
                        )
                    )
                )
            }
            if (isExportedDefaultImplementation) {
                add(ExportedParameter($$"$this", exportType((parent as KaNamedClassSymbol).defaultType, functionTypeParameterScope)))
            }
            for (parameter in function.contextParameters) {
                add(
                    ExportedParameter(
                        sanitizeName(parameter.name),
                        exportType(
                            parameter.returnType,
                            functionTypeParameterScope,
                            inlineClassesShouldBeUnboxed = inlineClassesShouldBeUnboxed
                        )
                    )
                )
            }
            function.receiverParameter?.let {
                add(
                    ExportedParameter(
                        EXTENSION_RECEIVER_NAME,
                        exportType(
                            it.returnType,
                            functionTypeParameterScope,
                            inlineClassesShouldBeUnboxed = inlineClassesShouldBeUnboxed
                        )
                    )
                )
            }
            for (parameter in function.valueParameters) {
                val type = if (parameter.isVararg) {
                    TypeExporter(config, functionTypeParameterScope).exportSpecializedArrayWithElementType(parameter.returnType)
                } else {
                    exportType(
                        parameter.returnType,
                        functionTypeParameterScope,
                        inlineClassesShouldBeUnboxed = inlineClassesShouldBeUnboxed
                    )
                }
                add(ExportedParameter(sanitizeName(parameter.name), type, parameter.hasDefaultValue))
            }
        }
    }

    context(_: KaSession)
    private fun exportProperty(
        property: KaPropertySymbol,
        parent: KaDeclarationSymbol?,
        classTypeParameterScope: TypeParameterScope,
        isExportedDefaultImplementation: Boolean = false,
    ): List<ExportedDeclaration> {
        // Frontend will report an error on an attempt to export an extension property.
        // Just to be safe, filter out such properties here as well.
        if (property.receiverType != null) {
            return emptyList()
        }

        val parentClass = parent as? KaClassSymbol
        val isAbstract = parentClass?.classKind != KaClassKind.INTERFACE && property.modality == KaSymbolModality.ABSTRACT
        val isStatic = property.isStatic || property.isJsStatic()
        val inlineClassesShouldBeUnboxed = property.isExternal

        val getter = property.getter
        val customGetterName = getter?.getJsNameForOverriddenDeclaration()
        val setter = property.setter
        val customSetterName = setter?.getJsNameForOverriddenDeclaration()
        val isProtected = property.visibility == KaSymbolVisibility.PROTECTED
        val typeParameters = if (parentClass != null && isExportedDefaultImplementation) {
            parentClass.typeParameters.memoryOptimizedMap { classTypeParameterScope[it]!! }
        } else {
            emptyList()
        }
        if (customGetterName != null || customSetterName != null) {
            val isMember = parentClass != null && !isExportedDefaultImplementation
            return buildList {
                if (customSetterName != null) {
                    add(
                        ExportedFunction(
                            name = ExportedMemberName.Identifier(customSetterName),
                            returnType = ExportedType.Primitive.Unit,
                            parameters = exportFunctionParameters(
                                setter,
                                parent,
                                classTypeParameterScope,
                                inlineClassesShouldBeUnboxed,
                                isExportedDefaultImplementation = isExportedDefaultImplementation,
                            ),
                            typeParameters = typeParameters,
                            isMember = isMember,
                            isStatic = isStatic && !isExportedDefaultImplementation,
                            isAbstract = isAbstract,
                            isProtected = isProtected,
                        )
                            .withAttributes(property, ignoreDoc = true)
                            .withAttributes(setter)
                    )
                }
                if (customGetterName != null) {
                    add(
                        ExportedFunction(
                            name = ExportedMemberName.Identifier(customGetterName),
                            returnType = exportType(
                                property.returnType,
                                classTypeParameterScope,
                                inlineClassesShouldBeUnboxed,
                            ),
                            parameters = exportFunctionParameters(
                                getter,
                                parent,
                                classTypeParameterScope,
                                inlineClassesShouldBeUnboxed,
                                isExportedDefaultImplementation = isExportedDefaultImplementation,
                            ),
                            typeParameters = typeParameters,
                            isMember = isMember,
                            isStatic = isStatic,
                            isAbstract = isAbstract,
                            isProtected = isProtected,
                        )
                            .withAttributes(property, ignoreDoc = true)
                            .withAttributes(getter)
                    )
                }
            }
        }

        val isMember = parentClass != null && !isExportedDefaultImplementation
        val isObjectGetter = false  // TODO: Should be true for getInstance functions of objects
        val isOptional = property.isExternal && isMember && property.returnType.isNullable
        val shouldBeExportedAsObjectWithAccessorsInside =
            (isExportedDefaultImplementation || config.artifactConfiguration.moduleKind == ModuleKind.ES) && !isMember && !isStatic

        val originalPropertyType = exportType(
            property.returnType,
            classTypeParameterScope,
            inlineClassesShouldBeUnboxed = inlineClassesShouldBeUnboxed,
        )
        val propertyType = when {
            !shouldBeExportedAsObjectWithAccessorsInside -> originalPropertyType
            isObjectGetter -> ExportedType.InlineInterfaceType(
                listOf(
                    ExportedFunction(
                        name = ExportedMemberName.Identifier("getInstance"),
                        returnType = originalPropertyType,
                        parameters = emptyList(),
                        isMember = true,
                        isProtected = false
                    ).withAttributes(property),
                )
            )
            else -> {
                val thisParam = runIf(isExportedDefaultImplementation) {
                    ExportedParameter($$"$this", exportType(parentClass!!.defaultType, classTypeParameterScope))
                }
                ExportedType.InlineInterfaceType(
                    listOfNotNull(
                        ExportedFunction(
                            name = ExportedMemberName.Identifier("get"),
                            returnType = originalPropertyType,
                            parameters = listOfNotNull(thisParam),
                            typeParameters = typeParameters,
                            isMember = true,
                            isProtected = false
                        )
                            .withAttributes(property, ignoreDoc = true)
                            .withAttributes(getter),

                        runIf(!property.isVal) {
                            ExportedFunction(
                                name = ExportedMemberName.Identifier("set"),
                                returnType = ExportedType.Primitive.Unit,
                                parameters = listOfNotNull(thisParam, ExportedParameter("value", originalPropertyType)),
                                typeParameters = typeParameters,
                                isMember = true,
                                isProtected = false
                            )
                                .withAttributes(property, ignoreDoc = true)
                                .withAttributes(setter)
                        }
                    )
                )
            }
        }

        val name = ExportedMemberName.Identifier(property.getExportedIdentifier())
        if (!isMember || parentClass.classKind == KaClassKind.INTERFACE) {
            return listOf(
                ExportedField(
                    name = name,
                    type = propertyType,
                    mutable = !property.isVal && !shouldBeExportedAsObjectWithAccessorsInside,
                    isMember = isMember,
                    isStatic = isStatic,
                    isAbstract = isAbstract,
                    isProtected = isProtected,
                    isObjectGetter = isObjectGetter,
                    isOptional = isOptional,
                ).withAttributes(property)
            )
        } else {
            val accessors: MutableList<ExportedDeclaration> = SmartList(
                ExportedPropertyGetter(
                    name = name,
                    type = propertyType,
                    isStatic = isStatic,
                    isAbstract = isAbstract,
                    isProtected = isProtected,
                )
                    .withAttributes(property, ignoreDoc = true)
                    .withAttributes(getter)
            )
            if (!property.isVal) {
                accessors.add(
                    ExportedPropertySetter(
                        name = name,
                        type = propertyType,
                        isStatic = isStatic,
                        isAbstract = isAbstract,
                        isProtected = isProtected,
                    )
                        .withAttributes(property, ignoreDoc = true)
                        .withAttributes(setter)
                )
            }
            return accessors
        }
    }

    context(_: KaSession)
    private fun exportEnumEntry(entry: KaEnumEntrySymbol, ordinal: Int, parentClass: KaNamedClassSymbol): ExportedPropertyGetter {
        fun fakeProperty(name: String, type: ExportedType) =
            ExportedPropertyGetter(ExportedMemberName.Identifier(name), type)

        val nameProperty = fakeProperty(
            name = "name",
            type = ExportedType.LiteralType.StringLiteralType(entry.name.asString()),
        )

        val ordinalProperty = fakeProperty(
            name = "ordinal",
            type = ExportedType.LiteralType.NumberLiteralType(ordinal),
        )

        val type = ExportedType.InlineInterfaceType(listOf(nameProperty, ordinalProperty))

        return ExportedPropertyGetter(
            name = ExportedMemberName.Identifier(entry.getExportedIdentifier()),
            type = ExportedType.IntersectionType(exportType(parentClass.defaultType, emptyMap(),), type),
            isStatic = true,
            isProtected = parentClass.visibility == KaSymbolVisibility.PROTECTED,
        ).withAttributes(entry)
    }

    private fun classExportability(klass: KaNamedClassSymbol, parent: KaDeclarationSymbol?): Exportability {
        when (klass.classKind) {
            KaClassKind.ANNOTATION_CLASS ->
                return Exportability.Prohibited("Class ${klass.classId?.asSingleFqName()} with kind: ${klass.classKind}")
            KaClassKind.OBJECT,
            KaClassKind.CLASS,
            KaClassKind.INTERFACE,
            KaClassKind.ENUM_CLASS,
            KaClassKind.COMPANION_OBJECT,
            KaClassKind.ANONYMOUS_OBJECT,
                -> {
            }
        }

        if (klass.exportedVisibility(parent) == ExportedVisibility.PRIVATE) {
            return Exportability.NotNeeded
        }

        if (klass.isJsImplicitExport()) {
            return Exportability.Implicit
        }

        if (klass.isInline)
            return Exportability.Prohibited("Inline class ${klass.classId?.asSingleFqName()}")

        return Exportability.Allowed
    }


    private fun exportDeclarationImplicitly(klass: KaNamedClassSymbol, superTypes: List<KaType>): ExportedClass? {
        return null // TODO(KT-82266)
    }

    context(_: KaSession)
    private fun exportClassMembers(
        klass: KaNamedClassSymbol,
        superTypes: List<KaType>,
        typeParameterScope: TypeParameterScope,
    ): ExportedClassDeclarationsInfo {
        val members = mutableListOf<ExportedDeclaration>()
        val nestedClasses = mutableListOf<ExportedClass>()
        val defaultImplementations = mutableListOf<ExportedDeclaration>()
        val isImplicitlyExportedClass = klass.isJsImplicitExport()
        val isCompanionObject = klass.classKind == KaClassKind.COMPANION_OBJECT

        val memberScope = klass.combinedMemberScope

        if (!isImplicitlyExportedClass) {
            if (klass.classKind == KaClassKind.ENUM_CLASS) {
                exportEnumSpecificMembers(klass, members)
            }

            for (constructor in memberScope.constructors) {
                if (!shouldDeclarationBeExportedImplicitlyOrExplicitly(constructor)) continue
                members.addIfNotNull(exportConstructor(constructor, klass, typeParameterScope))
            }
            for (member in memberScope.callables) {
                if (!shouldDeclarationBeExportedImplicitlyOrExplicitly(member)) continue
                if (isCompanionObject && member.isJsStatic()) {
                    // @JsStatic companion members are exported below
                    continue
                }
                val implementationStatus by lazy { member.getImplementationStatus(klass) }

                fun hasDefaultImplementationIn(klass: KaClassSymbol) =
                    klass.classKind == KaClassKind.INTERFACE && implementationStatus == ImplementationStatus.INHERITED_OR_SYNTHESIZED

                val original = member.fakeOverrideOriginal
                val actualParent = original.containingDeclaration as? KaClassSymbol ?: continue
                // We include only declarations from the class itself, plus inherited interface members that have a default implementation.
                val shouldInclude =
                    actualParent == klass || (klass.modality != KaSymbolModality.ABSTRACT && hasDefaultImplementationIn(actualParent))
                if (!shouldInclude){
                    continue
                }
                when (member) {
                    is KaNamedFunctionSymbol -> {
                        if (klass.classKind == KaClassKind.ENUM_CLASS
                            && member.isStatic
                            && (member.name == StandardNames.ENUM_VALUES || member.name == StandardNames.ENUM_VALUE_OF)
                        ) {
                            // We've already exported these above
                            continue
                        }
                        if (klass.isData
                            && DataClassResolver.isComponentLike(member.name)
                            && member.allOverriddenSymbols.none { shouldDeclarationBeExported(it) }
                        ) {
                            // Synthetic `componentN` functions should not be exported unless they override user-defined exported functions.
                            continue
                        }
                        members.addIfNotNull(exportFunction(member, klass, typeParameterScope))
                        if (hasDefaultImplementationIn(klass)) {
                            defaultImplementations.addIfNotNull(
                                exportFunction(
                                    member,
                                    klass,
                                    typeParameterScope,
                                    isExportedDefaultImplementation = true,
                                )
                            )
                        }
                    }
                    is KaPropertySymbol -> {
                        if (klass.classKind == KaClassKind.ENUM_CLASS && member.isStatic && member.name == StandardNames.ENUM_ENTRIES) {
                            // The `entries` static property should not be exported.
                            continue
                        }
                        members.addAll(exportProperty(member, klass, typeParameterScope))
                        if (hasDefaultImplementationIn(klass)) {
                            defaultImplementations.addAll(
                                exportProperty(
                                    member,
                                    klass,
                                    typeParameterScope,
                                    isExportedDefaultImplementation = true,
                                )
                            )
                        }
                    }
                    else -> continue
                }
            }
        }

        for (nested in klass.combinedDeclaredMemberScope.classifiers) {
            when (nested) {
                is KaNamedClassSymbol -> {
                    if (nested.classKind == KaClassKind.COMPANION_OBJECT) {
                        // Export `@JsStatic`-annotated
                        for (companionMember in nested.declaredMemberScope.callables) {
                            if (companionMember.isJsStatic()) {
                                when (companionMember) {
                                    is KaNamedFunctionSymbol -> {
                                        members.addIfNotNull(exportFunction(companionMember, klass, emptyMap()))
                                    }
                                    is KaPropertySymbol -> {
                                        members.addAll(exportProperty(companionMember, klass, emptyMap()))
                                    }
                                    else -> continue
                                }
                            }
                        }
                    }
                    if (!shouldDeclarationBeExportedImplicitlyOrExplicitly(nested)) continue
                    if (nested.isInner && (nested.modality == KaSymbolModality.OPEN || nested.modality == KaSymbolModality.FINAL)) {
                        members.add(nested.toFactoryPropertyForInnerClass(typeParameterScope))
                    }
                    nestedClasses.addIfNotNull(exportClass(nested, klass, typeParameterScope))
                }
                is KaTypeAliasSymbol -> {
                    if (!shouldDeclarationBeExportedImplicitlyOrExplicitly(nested)) continue
                    // TODO(KT-49795): Export type aliases
                    continue
                }
                else -> continue
            }
        }

        val classHasNonExportedAbstractMember = klass.hasNonExportedAbstractMembers()

        if (klass.shouldContainImplementableSymbolProperty(classHasNonExportedAbstractMember)) {
            members.addOwnJsSymbolDeclaration()
            members.addImplementableSymbolProperty(klass, config)
        }

        if (!klass.isExternal) {
            members.addSuperTypesSpecialProperties(klass, superTypes, typeParameterScope, config, classHasNonExportedAbstractMember)
        }

        if (defaultImplementations.isNotEmpty()) {
            members.add(
                ExportedNamespace(
                    StandardNames.DEFAULT_IMPLS_CLASS_NAME.identifier,
                    defaultImplementations,
                )
            )
        }

        return ExportedClassDeclarationsInfo(members, nestedClasses)
    }

    context(_: KaSession)
    private fun exportEnumSpecificMembers(enumClass: KaNamedClassSymbol, members: MutableList<ExportedDeclaration>) {
        members.add(
            ExportedConstructor(
                parameters = emptyList(),
                visibility = ExportedVisibility.PRIVATE
            )
        )

        // In Kotlin, enum entries always precede other class members. Preserve this order.
        val enumEntries = enumClass.staticDeclaredMemberScope.callables.filterIsInstance<KaEnumEntrySymbol>().toList()
        enumEntries.mapIndexedTo(members) { ordinal, entry -> exportEnumEntry(entry, ordinal, enumClass) }

        // Then emit synthetic enum-specific functions and properties
        members.add(
            ExportedFunction(
                name = ExportedMemberName.Identifier(StandardNames.ENUM_VALUES.asString()),
                returnType = ExportedType.InlineArrayType(
                    enumEntries.memoryOptimizedMap {
                        ExportedType.TypeOf(
                            ExportedType.ClassType(
                                name = it.getExportedFqName(config.generateNamespacesForPackages, config),
                                arguments = emptyList(),
                            )
                        )
                    }
                ),
                parameters = emptyList(),
                isMember = true,
                isStatic = true,
                isProtected = false,
            )
        )
        members.add(
            ExportedFunction(
                name = ExportedMemberName.Identifier(StandardNames.ENUM_VALUE_OF.asString()),
                returnType = if (enumEntries.isEmpty()) {
                    ExportedType.Primitive.Nothing
                } else {
                    exportType(enumClass.defaultType, emptyMap())
                },
                parameters = listOf(ExportedParameter("value", ExportedType.Primitive.String)),
                isMember = true,
                isStatic = true,
                isProtected = false,
            )
        )
        members.add(
            ExportedPropertyGetter(
                name = ExportedMemberName.Identifier("name"),
                type = enumEntries
                    .map { ExportedType.LiteralType.StringLiteralType(it.name.asString()) }
                    .reduceOrNull(ExportedType::UnionType)
                    ?: ExportedType.Primitive.Nothing,
            )
        )
        members.add(
            ExportedPropertyGetter(
                name = ExportedMemberName.Identifier("ordinal"),
                type = enumEntries.indices
                    .map { ExportedType.LiteralType.NumberLiteralType(it) }
                    .reduceOrNull(ExportedType::UnionType)
                    ?: ExportedType.Primitive.Nothing,
            )
        )
    }

    /**
     * Generates a property in the outer class that can be used to construct an instance of an inner class using Kotlin-like syntax.
     */
    context(_: KaSession)
    private fun KaNamedClassSymbol.toFactoryPropertyForInnerClass(outerClassTypeParameterScope: TypeParameterScope): ExportedPropertyGetter {
        val typeParameterScope = TypeParameterScope(this, config, outerClassTypeParameterScope)
        val typeMembers = declaredMemberScope.constructors.mapNotNull {
            exportConstructor(it, this, typeParameterScope, isFactoryPropertyForInnerClass = true)
        }.toList().compactIfPossible()
        val name = getExportedIdentifier()
        return ExportedPropertyGetter(
            name = ExportedMemberName.Identifier(name),
            type = ExportedType.InlineInterfaceType(typeMembers),
        )
    }

    context(_: KaSession)
    private fun exportType(
        type: KaType,
        scope: TypeParameterScope,
        shouldCalculateExportedSupertypeForImplicit: Boolean = false,
        inlineClassesShouldBeUnboxed: Boolean = false,
    ): ExportedType = TypeExporter(config, scope).exportType(type, inlineClassesShouldBeUnboxed)

    context(_: KaSession)
    private fun functionExportability(function: KaNamedFunctionSymbol, parent: KaDeclarationSymbol?): Exportability {
        if (function.isInline && function.typeParameters.any { it.isReified })
            return Exportability.Prohibited("Inline reified function")

        val parentClass = parent as? KaClassSymbol

        // TODO: Use [] syntax instead of prohibiting
        val name = function.getExportedIdentifier()
        if (parentClass == null && name in allReservedWords) {
            return Exportability.Prohibited("Name is a reserved word")
        }

        return Exportability.Allowed
    }

    private data class ExportedClassDeclarationsInfo(
        val members: List<ExportedDeclaration>,
        val nestedClasses: List<ExportedClass>,
    )

    private fun KaNamedClassSymbol.shouldContainImplementableSymbolProperty(hasNotExportedAbstractMember: Boolean): Boolean =
        !hasNotExportedAbstractMember && config.implementableInterfaces && classKind == KaClassKind.INTERFACE && !isExternal && !isJsImplicitExport() && !isJsNoRuntime()
}
