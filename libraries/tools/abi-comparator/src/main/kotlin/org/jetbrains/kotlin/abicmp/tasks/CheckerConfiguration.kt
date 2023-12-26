/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.abicmp.tasks

import kotlinx.metadata.ExperimentalContextReceivers
import kotlinx.metadata.hasConstant
import kotlinx.metadata.isVar
import kotlinx.metadata.jvm.*
import kotlinx.metadata.visibility
import org.jetbrains.kotlin.abicmp.*
import org.jetbrains.kotlin.abicmp.checkers.*
import org.jetbrains.kotlin.kotlinp.*
import org.jetbrains.org.objectweb.asm.tree.ClassNode
import org.jetbrains.org.objectweb.asm.tree.FieldNode
import org.jetbrains.org.objectweb.asm.tree.MethodNode
import kotlin.contracts.ExperimentalContracts

private val allClassCheckers = listOf(
    classPropertyChecker(ClassNode::version),
    classPropertyChecker(ClassNode::access) { v -> "${v.toString(2)} ${v.classFlags()}" },
    classPropertyChecker("internalName", ClassNode::name),
    classPropertyChecker(ClassNode::signature),
    classPropertyChecker("superClassInternalName", ClassNode::superName),
    classPropertyChecker("superInterfaces") { (it.interfaces as List<String>).sorted() },
    classPropertyChecker(ClassNode::sourceFile),
    classPropertyChecker(ClassNode::outerClass),
    classPropertyChecker(ClassNode::outerMethod),
    classPropertyChecker(ClassNode::outerMethodDesc),
    ClassAnnotationsChecker(ClassNode::visibleAnnotations),
    ClassAnnotationsChecker(ClassNode::invisibleAnnotations),
    InnerClassesListChecker(),
    MethodsListChecker(),
    FieldsListChecker()
)

private val allMethodCheckers = listOf(
    methodPropertyChecker(MethodNode::access) { v -> "${v.toString(2)} ${v.methodFlags()}" },
    methodPropertyChecker("methodName", MethodNode::name),
    methodPropertyChecker(MethodNode::desc),
    methodPropertyChecker(MethodNode::signature),
    methodPropertyChecker("exceptions") { it.exceptions.listOfNotNull<String>().sorted() },
    methodPropertyChecker("annotationDefault") { it.annotationDefault?.toAnnotationArgumentValue() },
    MethodAnnotationsChecker(MethodNode::visibleAnnotations),
    MethodAnnotationsChecker(MethodNode::invisibleAnnotations),
    MethodParameterAnnotationsChecker(MethodNode::visibleParameterAnnotations),
    MethodParameterAnnotationsChecker(MethodNode::invisibleParameterAnnotations)
)

private val allConstructorMetadataCheckers = listOf(
    constructorMetadataPropertyChecker("versionRequirements") { it.versionRequirements.stringifyRelevantRequirements() },
    constructorMetadataPropertyChecker("modifiers") { printConstructorModifiers(it) },
    constructorMetadataPropertyChecker("valueParameters") { it.valueParameters.stringifyValueParameters() }
)

private val allFunctionMetadataCheckers = listOf(
    functionMetadataPropertyChecker("lambdaClassOriginName") { it.lambdaClassOriginName.toString() },
    functionMetadataPropertyChecker("versionRequirements") { it.versionRequirements.stringifyRelevantRequirements() },
    functionMetadataPropertyChecker("contextReceiverTypes") {
        @OptIn(ExperimentalContextReceivers::class)
        it.contextReceiverTypes.stringifyTypeListSorted()
    },
    functionMetadataPropertyChecker("modifiers") { printFunctionModifiers(it) },
    functionMetadataPropertyChecker("typeParameters") { it.typeParameters.stringifyTypeParameters() },
    functionMetadataPropertyChecker("receiverParameterType") {
        it.receiverParameterType?.let { type -> printType(type) } ?: PROPERTY_VAL_STUB
    },
    functionMetadataPropertyChecker("valueParameters") { it.valueParameters.stringifyValueParameters() },
    functionMetadataPropertyChecker("contract") {
        @OptIn(ExperimentalContracts::class)
        it.contract?.let { contract -> printContract(contract) } ?: PROPERTY_VAL_STUB
    }
)

private val allPropertyMetadataCheckers = listOf(
    propertyMetadataPropertyChecker("versionRequirements") { it.versionRequirements.stringifyRelevantRequirements() },
    propertyMetadataPropertyChecker("fieldSignature") { it.fieldSignature?.toString() ?: PROPERTY_VAL_STUB },
    propertyMetadataPropertyChecker("getterSignature") { it.getterSignature?.toString() ?: PROPERTY_VAL_STUB },
    propertyMetadataPropertyChecker("setterSignature") { it.setterSignature?.toString() ?: PROPERTY_VAL_STUB },
    propertyMetadataPropertyChecker("syntheticMethodForAnnotations") { it.syntheticMethodForAnnotations?.toString() ?: PROPERTY_VAL_STUB },
    propertyMetadataPropertyChecker("syntheticMethodForDelegate") { it.syntheticMethodForDelegate?.toString() ?: PROPERTY_VAL_STUB },
    propertyMetadataPropertyChecker("isMovedFromInterfaceCompanion") { it.isMovedFromInterfaceCompanion.toString() },
    propertyMetadataPropertyChecker("contextReceiverTypes") {
        @OptIn(ExperimentalContextReceivers::class)
        it.contextReceiverTypes.stringifyTypeListSorted()
    },
    propertyMetadataPropertyChecker("modifiers") { printPropertyModifiers(it) },
    propertyMetadataPropertyChecker("isVar") { it.isVar.toString() },
    propertyMetadataPropertyChecker("typeParameters") { it.typeParameters.stringifyTypeParameters() },
    propertyMetadataPropertyChecker("receiverParameterType") {
        it.receiverParameterType?.let { type -> printType(type) } ?: PROPERTY_VAL_STUB
    },
    propertyMetadataPropertyChecker("returnType") { printType(it.returnType) },
    propertyMetadataPropertyChecker("hasConstant") { it.hasConstant.toString() },
    propertyMetadataPropertyChecker("getterModifiers") { printPropertyAccessorModifiers(it.getter) },
    propertyMetadataPropertyChecker("setterModifiers") {
        it.setter?.let { setter -> printPropertyAccessorModifiers(setter) } ?: PROPERTY_VAL_STUB
    },
    propertyMetadataPropertyChecker("setterValueParameter") {
        it.setterParameter?.let { param -> printValueParameter(param) } ?: PROPERTY_VAL_STUB
    }
)

private val allTypeAliasMetadataCheckers = listOf(
    typeAliasMetadataPropertyChecker("versionRequirements") { it.versionRequirements.stringifyRelevantRequirements() },
    typeAliasMetadataPropertyChecker("annotations") { it.annotations.stringifyAnnotations() },
    typeAliasMetadataPropertyChecker("visibility") { it.visibility.toString() },
    typeAliasMetadataPropertyChecker("typeParameters") { it.typeParameters.stringifyTypeParameters() },
    typeAliasMetadataPropertyChecker("underlyingType") { printType(it.underlyingType) },
    typeAliasMetadataPropertyChecker("expandedType") { printType(it.expandedType) }
)

private val allFieldCheckers = listOf(
    fieldPropertyChecker(FieldNode::access) { v -> "${v.toString(2)} ${v.fieldFlags()}" },
    fieldPropertyChecker("fieldName", FieldNode::name),
    fieldPropertyChecker(FieldNode::desc),
    fieldPropertyChecker(FieldNode::signature),
    fieldPropertyChecker("initialValue", FieldNode::value),
    FieldAnnotationsChecker(FieldNode::visibleAnnotations),
    FieldAnnotationsChecker(FieldNode::invisibleAnnotations)
)

private val allClassMetadataCheckers = listOf(
    classMetadataListChecker("constructors") { loadConstructors(it).keys.toList() },
    classMetadataListChecker("functions") { loadFunctions(it.kmClass).keys.toList() },
    classMetadataListChecker("properties") { loadProperties(it.kmClass).keys.toList() },
    classMetadataListChecker("typeAliases") { it.kmClass.typeAliases.map { typeAlias -> typeAlias.name } },
    classMetadataListChecker("nestedClasses") { it.kmClass.nestedClasses },
    classMetadataListChecker("enumEntries") { it.kmClass.enumEntries },
    classMetadataListChecker("sealedSubclasses") { it.kmClass.sealedSubclasses },
    classMetadataListChecker("localDelegatedProperties") { loadLocalDelegatedProperties(it).keys.toList() },

    classMetadataPropertyChecker("typeParameters") { it.kmClass.typeParameters.stringifyTypeParameters() },
    classMetadataPropertyChecker("superTypes") {
        it.kmClass.supertypes.map { type -> printType(type) }.sorted().joinToString(prefix = "[", postfix = "]")
    },
    classMetadataPropertyChecker("companionObject") { it.kmClass.companionObject.toString() },
    classMetadataPropertyChecker("inlineClassUnderlyingPropertyName") { it.kmClass.inlineClassUnderlyingPropertyName.toString() },
    classMetadataPropertyChecker("inlineClassUnderlyingType") {
        it.kmClass.inlineClassUnderlyingType?.let { type -> printType(type) } ?: "---"
    },
    classMetadataPropertyChecker("contextReceiverTypes") {
        @OptIn(ExperimentalContextReceivers::class)
        it.kmClass.contextReceiverTypes.stringifyTypeListSorted()
    },
    classMetadataPropertyChecker("versionRequirements") { it.kmClass.versionRequirements.stringifyRelevantRequirements() }
)

private val allPackageMetadataCheckers = listOf(
    fileFacadeMetadataListChecker("functions") { loadFunctions(it).keys.toList() },
    fileFacadeMetadataListChecker("properties") { loadProperties(it).keys.toList() },
    fileFacadeMetadataListChecker("typeAliases") { loadTypeAliases(it).keys.toList() },
    fileFacadeMetadataListChecker("localDelegatedProperties") { loadLocalDelegatedProperties(it).keys.toList() }
)

private val allMultifileClassFacadeMetadataCheckers = listOf(
    multiFileClassFacadeMetadataListChecker("partClassNames") { it.partClassNames }
)

private val allMultifileClassPartMetadataCheckers = listOf(
    multiFileClassPartMetadataPropertyChecker("facadeClassName") { it.facadeClassName }
)

private val allSyntheticClassMetadataCheckers = listOf(
    syntheticClassMetadataPropertyChecker("isLambda") { it.isLambda.toString() },
    syntheticClassMetadataPropertyChecker("function") {
        it.kmLambda?.function?.let { function ->
            printFunction(
                function,
                KotlinpSettings(
                    isVerbose = true,
                    sortDeclarations = true
                )
            )
        } ?: PROPERTY_VAL_STUB
    }
)


class CheckerConfigurationBuilder {
    private val enabledExclusively = HashSet<String>()
    private val disabled = HashSet<String>()

    fun enableExclusively(name: String) {
        enabledExclusively.add(name)
    }

    fun disable(name: String) {
        disabled.add(name)
    }

    fun build() = CheckerConfiguration(enabledExclusively, disabled)
}

inline fun checkerConfiguration(b: CheckerConfigurationBuilder.() -> Unit): CheckerConfiguration {
    val builder = CheckerConfigurationBuilder()
    builder.b()
    return builder.build()
}

class CheckerConfiguration(private val enabledExclusively: Set<String>, private val disabled: Set<String>) {

    private fun <T : Checker> List<T>.filterOutDisabled() = filter { it.isEnabled() }

    val enabledClassCheckers: List<ClassChecker> = allClassCheckers.filterOutDisabled()
    val enabledMethodCheckers: List<MethodChecker> = allMethodCheckers.filterOutDisabled()
    val enabledFieldCheckers: List<FieldChecker> = allFieldCheckers.filterOutDisabled()
    val enabledPropertyMetadataCheckers = allPropertyMetadataCheckers.filterOutDisabled()
    val enabledConstructorMetadataCheckers = allConstructorMetadataCheckers.filterOutDisabled()
    val enabledFunctionMetadataCheckers = allFunctionMetadataCheckers.filterOutDisabled()
    val enabledTypeAliasMetadataCheckers = allTypeAliasMetadataCheckers.filterOutDisabled()
    val enabledClassMetadataCheckers = allClassMetadataCheckers.filterOutDisabled()
    val enabledPackageMetadataCheckers = allPackageMetadataCheckers.filterOutDisabled()
    val enabledMultifileClassFacadeMetadataCheckers = allMultifileClassFacadeMetadataCheckers.filterOutDisabled()
    val enabledMultifileClassPartMetadataCheckers = allMultifileClassPartMetadataCheckers.filterOutDisabled()
    val enabledAllSyntheticClassMetadataCheckers = allSyntheticClassMetadataCheckers.filterOutDisabled()

    private fun Checker.isEnabled(): Boolean {
        if (enabledExclusively.isNotEmpty() && name !in enabledExclusively) return false
        return name !in disabled
    }
}