/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.utils

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.commonizer.fqName
import org.jetbrains.kotlin.descriptors.commonizer.fqNameWithTypeParameters
import org.jetbrains.kotlin.descriptors.commonizer.isNull
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.getAbbreviation
import kotlin.contracts.ExperimentalContracts
import kotlin.reflect.KCallable
import kotlin.test.fail

@ExperimentalContracts
internal class ComparingDeclarationsVisitor(
    val designatorMessage: String
) : DeclarationDescriptorVisitor<Unit, ComparingDeclarationsVisitor.Context> {

    inner class Context private constructor(
        private val actual: DeclarationDescriptor?,
        private val path: List<String>
    ) {
        constructor(actual: DeclarationDescriptor?) : this(actual, listOf(actual.toString()))

        fun nextLevel(nextActual: DeclarationDescriptor?) = Context(nextActual, path + nextActual.toString())

        fun nextLevel(customPathElement: String) = Context(actual, path + customPathElement)

        inline fun <reified T> getActualAs() = actual as T

        override fun toString() =
            """
            |Context: ${this@ComparingDeclarationsVisitor.designatorMessage}
            |Path: ${path.joinToString(separator = " ->\n\t")}"
            """.trimMargin()
    }

    override fun visitModuleDeclaration(expected: ModuleDescriptor, context: Context) {
        val actual = context.getActualAs<ModuleDescriptor>()

        context.assertFieldsEqual(expected::getName, actual::getName)

        fun collectPackageMemberScopes(module: ModuleDescriptor): Map<FqName, MemberScope> = mutableMapOf<FqName, MemberScope>().also {
            module.collectNonEmptyPackageMemberScopes { packageFqName, memberScope ->
                if (memberScope.getContributedDescriptors().isNotEmpty())
                    it[packageFqName] = memberScope
            }
        }

        val expectedPackageMemberScopes = collectPackageMemberScopes(expected)
        val actualPackageMemberScopes = collectPackageMemberScopes(actual)

        context.assertSetsEqual(expectedPackageMemberScopes.keys, actualPackageMemberScopes.keys, "sets of packages")

        for (packageFqName in expectedPackageMemberScopes.keys) {
            val expectedMemberScope = expectedPackageMemberScopes.getValue(packageFqName)
            val actualMemberScope = actualPackageMemberScopes.getValue(packageFqName)

            visitMemberScopes(expectedMemberScope, actualMemberScope, context.nextLevel("package member scope [$packageFqName]"))
        }
    }

    private fun visitMemberScopes(expected: MemberScope, actual: MemberScope, context: Context) {
        val expectedProperties = mutableMapOf<PropertyApproximationKey, PropertyDescriptor>()
        val expectedFunctions = mutableMapOf<FunctionApproximationKey, SimpleFunctionDescriptor>()
        val expectedClasses = mutableMapOf<FqName, ClassDescriptor>()
        val expectedTypeAliases = mutableMapOf<FqName, TypeAliasDescriptor>()

        expected.collectMembers(
            CallableMemberCollector<PropertyDescriptor> { expectedProperties[PropertyApproximationKey(it)] = it },
            CallableMemberCollector<SimpleFunctionDescriptor> { expectedFunctions[FunctionApproximationKey(it)] = it },
            Collector<ClassDescriptor> { expectedClasses[it.fqNameSafe] = it },
            Collector<TypeAliasDescriptor> { expectedTypeAliases[it.fqNameSafe] = it }
        )

        val actualProperties = mutableMapOf<PropertyApproximationKey, PropertyDescriptor>()
        val actualFunctions = mutableMapOf<FunctionApproximationKey, SimpleFunctionDescriptor>()
        val actualClasses = mutableMapOf<FqName, ClassDescriptor>()
        val actualTypeAliases = mutableMapOf<FqName, TypeAliasDescriptor>()

        actual.collectMembers(
            CallableMemberCollector<PropertyDescriptor> { actualProperties[PropertyApproximationKey(it)] = it },
            CallableMemberCollector<SimpleFunctionDescriptor> { actualFunctions[FunctionApproximationKey(it)] = it },
            Collector<ClassDescriptor> { actualClasses[it.fqNameSafe] = it },
            Collector<TypeAliasDescriptor> { actualTypeAliases[it.fqNameSafe] = it }
        )

        context.assertSetsEqual(expectedProperties.keys, actualProperties.keys, "sets of properties")

        expectedProperties.forEach { (propertyKey, expectedProperty) ->
            val actualProperty = actualProperties.getValue(propertyKey)
            expectedProperty.accept(this, context.nextLevel(actualProperty))
        }

        context.assertSetsEqual(expectedFunctions.keys, actualFunctions.keys, "sets of functions")

        expectedFunctions.forEach { (functionKey, expectedFunction) ->
            val actualFunction = actualFunctions.getValue(functionKey)
            expectedFunction.accept(this, context.nextLevel(actualFunction))
        }

        context.assertSetsEqual(expectedClasses.keys, actualClasses.keys, "sets of classes")

        expectedClasses.forEach { (classFqName, expectedClass) ->
            val actualClass = actualClasses.getValue(classFqName)
            expectedClass.accept(this, context.nextLevel(actualClass))
        }

        context.assertSetsEqual(expectedTypeAliases.keys, actualTypeAliases.keys, "sets of type aliases")

        expectedTypeAliases.forEach { (typeAliasFqName, expectedTypeAlias) ->
            val actualTypeAlias = actualTypeAliases.getValue(typeAliasFqName)
            expectedTypeAlias.accept(this, context.nextLevel(actualTypeAlias))
        }
    }

    override fun visitFunctionDescriptor(expected: FunctionDescriptor, context: Context) {
        @Suppress("NAME_SHADOWING")
        val expected = expected as SimpleFunctionDescriptor
        val actual = context.getActualAs<SimpleFunctionDescriptor>()

        visitAnnotations(expected.annotations, actual.annotations, context.nextLevel("Function annotations"))
        context.assertFieldsEqual(expected::getName, actual::getName)
        context.assertFieldsEqual(expected::getVisibility, actual::getVisibility)
        context.assertFieldsEqual(expected::getModality, actual::getModality)
        context.assertFieldsEqual(expected::getKind, actual::getKind)
        context.assertFieldsEqual(expected::isOperator, actual::isOperator)
        context.assertFieldsEqual(expected::isInfix, actual::isInfix)
        context.assertFieldsEqual(expected::isInline, actual::isInline)
        context.assertFieldsEqual(expected::isTailrec, actual::isTailrec)
        context.assertFieldsEqual(expected::isSuspend, actual::isSuspend)
        context.assertFieldsEqual(expected::isExternal, actual::isExternal)
        context.assertFieldsEqual(expected::isExpect, actual::isExpect)
        context.assertFieldsEqual(expected::hasStableParameterNames, actual::hasStableParameterNames)
        context.assertFieldsEqual(expected::hasSynthesizedParameterNames, actual::hasSynthesizedParameterNames)

        if (!expected.isActual || actual.kind != CallableMemberDescriptor.Kind.DELEGATION) {
            context.assertFieldsEqual(expected::isActual, actual::isActual)
        } /* else {
            // don't check, because there can be any value in expect.isActual
            // see org.jetbrains.kotlin.resolve.DelegationResolver
        } */

        visitType(expected.returnType, actual.returnType, context.nextLevel("Function type"))

        visitValueParameterDescriptorList(expected.valueParameters, actual.valueParameters, context.nextLevel("Function value parameters"))

        visitReceiverParameterDescriptor(expected.extensionReceiverParameter, context.nextLevel(actual.extensionReceiverParameter))
        visitReceiverParameterDescriptor(expected.dispatchReceiverParameter, context.nextLevel(actual.dispatchReceiverParameter))

        visitTypeParameters(expected.typeParameters, actual.typeParameters, context.nextLevel("Function type parameters"))
    }

    private fun visitValueParameterDescriptorList(
        expected: List<ValueParameterDescriptor>,
        actual: List<ValueParameterDescriptor>,
        context: Context
    ) {
        context.assertEquals(expected.size, actual.size, "Size of value parameters list")

        expected.forEachIndexed { index, expectedParam ->
            val actualParam = actual[index]
            expectedParam.accept(this, context.nextLevel(actualParam))
        }
    }

    override fun visitValueParameterDescriptor(expected: ValueParameterDescriptor, context: Context) {
        val actual = context.getActualAs<ValueParameterDescriptor>()

        visitAnnotations(expected.annotations, actual.annotations, context.nextLevel("Value parameter annotations"))
        context.assertFieldsEqual(expected::getName, actual::getName)
        context.assertFieldsEqual(expected::index, actual::index)
        context.assertFieldsEqual(expected::declaresDefaultValue, actual::declaresDefaultValue)
        context.assertFieldsEqual(expected::isCrossinline, actual::isCrossinline)
        context.assertFieldsEqual(expected::isNoinline, actual::isNoinline)
        visitType(expected.type, actual.type, context.nextLevel("Value parameter type"))
        visitType(expected.varargElementType, actual.varargElementType, context.nextLevel("Value parameter vararg element type"))
    }

    private fun visitTypeParameters(expected: List<TypeParameterDescriptor>, actual: List<TypeParameterDescriptor>, context: Context) {
        context.assertEquals(expected.size, actual.size, "Type parameters list size")

        expected.forEachIndexed { index, expectedParam ->
            val actualParam = actual[index]
            visitTypeParameterDescriptor(expectedParam, context.nextLevel(actualParam))
        }
    }

    override fun visitTypeParameterDescriptor(expected: TypeParameterDescriptor, context: Context) {
        val actual = context.getActualAs<TypeParameterDescriptor>()

        visitAnnotations(expected.annotations, actual.annotations, context.nextLevel("Type parameter annotations"))
        context.assertFieldsEqual(expected::getName, actual::getName)
        context.assertFieldsEqual(expected::getIndex, actual::getIndex)
        context.assertFieldsEqual(expected::isCapturedFromOuterDeclaration, actual::isCapturedFromOuterDeclaration)
        context.assertFieldsEqual(expected::isReified, actual::isReified)
        context.assertFieldsEqual(expected::getVariance, actual::getVariance)

        val expectedUpperBounds = expected.upperBounds
        val actualUpperBounds = actual.upperBounds

        context.assertEquals(expectedUpperBounds.size, actualUpperBounds.size, "Size of upper bound types")

        expectedUpperBounds.forEachIndexed { index, expectedType ->
            val actualType = actualUpperBounds[index]
            visitType(expectedType, actualType, context.nextLevel("Type parameter type"))
        }
    }

    override fun visitClassDescriptor(expected: ClassDescriptor, context: Context) {
        val actual = context.getActualAs<ClassDescriptor>()

        visitAnnotations(expected.annotations, actual.annotations, context.nextLevel("Class annotations"))
        context.assertFieldsEqual(expected::getName, actual::getName)
        context.assertFieldsEqual(expected::getVisibility, actual::getVisibility)
        context.assertFieldsEqual(expected::getModality, actual::getModality)
        context.assertFieldsEqual(expected::getKind, actual::getKind)
        context.assertFieldsEqual(expected::isCompanionObject, actual::isCompanionObject)
        context.assertFieldsEqual(expected::isData, actual::isData)
        context.assertFieldsEqual(expected::isInline, actual::isInline)
        context.assertFieldsEqual(expected::isInner, actual::isInner)
        context.assertFieldsEqual(expected::isExternal, actual::isExternal)
        context.assertFieldsEqual(expected::isExpect, actual::isExpect)
        context.assertFieldsEqual(expected::isActual, actual::isActual)

        visitTypeParameters(
            expected.declaredTypeParameters,
            actual.declaredTypeParameters,
            context.nextLevel("Class declared type parameters")
        )

        if (expected.sealedSubclasses.isNotEmpty() || actual.sealedSubclasses.isNotEmpty()) {
            val expectedSealedSubclassesFqNames = expected.sealedSubclasses.mapTo(HashSet()) { it.fqNameSafe }
            val actualSealedSubclassesFqNames = actual.sealedSubclasses.mapTo(HashSet()) { it.fqNameSafe }

            context.assertSetsEqual(expectedSealedSubclassesFqNames, actualSealedSubclassesFqNames, "Sealed subclasses FQ names")
        }

        val expectedSupertypeFqNames = expected.typeConstructor.supertypes.mapTo(HashSet()) { it.fqNameWithTypeParameters }
        val actualSupertypeFqNames = actual.typeConstructor.supertypes.mapTo(HashSet()) { it.fqNameWithTypeParameters }

        context.assertSetsEqual(expectedSupertypeFqNames, actualSupertypeFqNames, "Supertypes FQ names")

        if (expected.constructors.isNotEmpty() || actual.constructors.isNotEmpty()) {
            val expectedConstructors = expected.constructors.associateBy { ConstructorApproximationKey(it) }
            val actualConstructors = actual.constructors.associateBy { ConstructorApproximationKey(it) }

            context.assertSetsEqual(expectedConstructors.keys, actualConstructors.keys, "sets of class constructors")

            for (key in expectedConstructors.keys) {
                val expectedConstructor = expectedConstructors.getValue(key)
                val actualConstructor = actualConstructors.getValue(key)

                visitConstructorDescriptor(expectedConstructor, context.nextLevel(actualConstructor))
            }
        }

        visitMemberScopes(
            expected.unsubstitutedMemberScope,
            actual.unsubstitutedMemberScope,
            context.nextLevel("class member scope [${expected.fqNameSafe}]")
        )
    }

    override fun visitConstructorDescriptor(expected: ConstructorDescriptor, context: Context) {
        val actual = context.getActualAs<ConstructorDescriptor>()

        visitAnnotations(expected.annotations, actual.annotations, context.nextLevel("Constructor annotations"))
        context.assertFieldsEqual(expected::getVisibility, actual::getVisibility)
        context.assertFieldsEqual(expected::isPrimary, actual::isPrimary)
        context.assertFieldsEqual(expected::getKind, actual::getKind)
        context.assertFieldsEqual(expected::hasStableParameterNames, actual::hasStableParameterNames)
        context.assertFieldsEqual(expected::hasSynthesizedParameterNames, actual::hasSynthesizedParameterNames)
        context.assertFieldsEqual(expected::isExpect, actual::isExpect)
        context.assertFieldsEqual(expected::isActual, actual::isActual)

        visitValueParameterDescriptorList(
            expected.valueParameters,
            actual.valueParameters,
            context.nextLevel("Constructor value parameters")
        )

        visitTypeParameters(expected.typeParameters, actual.typeParameters, context.nextLevel("Constructor type parameters"))
    }

    override fun visitTypeAliasDescriptor(expected: TypeAliasDescriptor, context: Context) {
        val actual = context.getActualAs<TypeAliasDescriptor>()

        visitAnnotations(expected.annotations, actual.annotations, context.nextLevel("Type alias annotations"))
        context.assertFieldsEqual(expected::getName, actual::getName)
        context.assertFieldsEqual(expected::getVisibility, actual::getVisibility)
        context.assertFieldsEqual(expected::isActual, actual::isActual)

        visitTypeParameters(
            expected.declaredTypeParameters,
            actual.declaredTypeParameters,
            context.nextLevel("Type alias declared type parameters")
        )

        visitType(expected.underlyingType, actual.underlyingType, context.nextLevel("Type alias underlying type"))
        visitType(expected.expandedType, actual.expandedType, context.nextLevel("Type alias expanded type"))
    }

    override fun visitPropertyDescriptor(expected: PropertyDescriptor, context: Context) {
        val actual = context.getActualAs<PropertyDescriptor>()

        visitAnnotations(expected.annotations, actual.annotations, context.nextLevel("Property annotations"))
        context.assertFieldsEqual(expected::getName, actual::getName)
        context.assertFieldsEqual(expected::getVisibility, actual::getVisibility)
        context.assertFieldsEqual(expected::getModality, actual::getModality)
        context.assertFieldsEqual(expected::isVar, actual::isVar)
        context.assertFieldsEqual(expected::getKind, actual::getKind)
        context.assertFieldsEqual(expected::isLateInit, actual::isLateInit)
        context.assertFieldsEqual(expected::isConst, actual::isConst)
        context.assertFieldsEqual(expected::isExternal, actual::isExternal)
        context.assertFieldsEqual(expected::isExpect, actual::isExpect)

        if (!expected.isActual || actual.kind != CallableMemberDescriptor.Kind.DELEGATION) {
            context.assertFieldsEqual(expected::isActual, actual::isActual)
        } /* else {
            // don't check, because there can be any value in expect.isActual
            // see org.jetbrains.kotlin.resolve.DelegationResolver
        } */

        @Suppress("DEPRECATION")
        context.assertFieldsEqual(expected::isDelegated, actual::isDelegated)

        visitAnnotations(
            expected.delegateField?.annotations,
            actual.delegateField?.annotations,
            context.nextLevel("Property delegate field annotations")
        )
        visitAnnotations(
            expected.backingField?.annotations,
            actual.backingField?.annotations,
            context.nextLevel("Property backing field annotations")
        )

        context.assertEquals(expected.compileTimeInitializer.isNull(), actual.compileTimeInitializer.isNull(), "compile-time initializers")
        visitType(expected.type, actual.type, context.nextLevel("Property type"))

        visitPropertyGetterDescriptor(expected.getter, context.nextLevel(actual.getter))
        visitPropertySetterDescriptor(expected.setter, context.nextLevel(actual.setter))

        visitReceiverParameterDescriptor(expected.extensionReceiverParameter, context.nextLevel(actual.extensionReceiverParameter))
        visitReceiverParameterDescriptor(expected.dispatchReceiverParameter, context.nextLevel(actual.dispatchReceiverParameter))

        visitTypeParameters(expected.typeParameters, actual.typeParameters, context.nextLevel("Property type parameters"))
    }

    override fun visitPropertyGetterDescriptor(expected: PropertyGetterDescriptor?, context: Context) {
        val actual = context.getActualAs<PropertyGetterDescriptor?>()
        if (expected === actual) return

        check(actual != null && expected != null)

        visitAnnotations(expected.annotations, actual.annotations, context.nextLevel("Property getter annotations"))
        context.assertFieldsEqual(expected::isDefault, actual::isDefault)
        context.assertFieldsEqual(expected::isExternal, actual::isExternal)
        context.assertFieldsEqual(expected::isInline, actual::isInline)
    }

    override fun visitPropertySetterDescriptor(expected: PropertySetterDescriptor?, context: Context) {
        val actual = context.getActualAs<PropertySetterDescriptor?>()
        if (expected === actual) return

        check(actual != null && expected != null)

        visitAnnotations(expected.annotations, actual.annotations, context.nextLevel("Property setter annotations"))
        context.assertFieldsEqual(expected::isDefault, actual::isDefault)
        context.assertFieldsEqual(expected::isExternal, actual::isExternal)
        context.assertFieldsEqual(expected::isInline, actual::isInline)
        context.assertFieldsEqual(expected::getVisibility, actual::getVisibility)
        visitAnnotations(
            expected.valueParameters.single().annotations,
            actual.valueParameters.single().annotations,
            context.nextLevel("Property setter value parameter annotations")
        )
    }

    override fun visitReceiverParameterDescriptor(expected: ReceiverParameterDescriptor?, context: Context) {
        val actual = context.getActualAs<ReceiverParameterDescriptor?>()
        if (expected === actual) return

        check(actual != null && expected != null)

        visitType(expected.type, actual.type, context.nextLevel("Receiver parameter type"))
        visitAnnotations(expected.annotations, actual.annotations, context.nextLevel("Receiver parameter annotations"))
    }


    private fun visitAnnotations(expected: Annotations?, actual: Annotations?, context: Context) {
        if (expected === actual || (expected?.isEmpty() != false && actual?.isEmpty() != false)) return

        val expectedAnnotationFqNames: Set<FqName?> = expected?.mapTo(HashSet()) { it.fqName } ?: emptySet()
        val actualAnnotationFqNames: Set<FqName?> = actual?.mapTo(HashSet()) { it.fqName } ?: emptySet()

        context.assertSetsEqual(expectedAnnotationFqNames, actualAnnotationFqNames, "annotations")
    }

    private fun visitType(expected: KotlinType?, actual: KotlinType?, context: Context) {
        if (expected === actual) return

        check(actual != null && expected != null)

        visitAnnotations(
            expected.annotations,
            actual.annotations,
            context.nextLevel("Type annotations")
        )

        val expectedUnwrapped = expected.getAbbreviation() ?: expected.unwrap()
        val actualUnwrapped = actual.getAbbreviation() ?: actual.unwrap()

        if (expectedUnwrapped === actualUnwrapped) return

        visitAnnotations(
            expectedUnwrapped.annotations,
            actualUnwrapped.annotations,
            context.nextLevel("Unwrapped/unabbreviated type annotations")
        )

        val expectedFqName = expectedUnwrapped.fqName
        val actualFqName = actualUnwrapped.fqName

        context.assertEquals(expectedFqName, actualFqName, "type FQN")

        val expectedArguments = expectedUnwrapped.arguments
        val actualArguments = actualUnwrapped.arguments

        context.assertEquals(expectedArguments.size, actualArguments.size, "size of type arguments list")

        expectedArguments.forEachIndexed { index, expectedArgument ->
            val actualArgument = actualArguments[index]

            context.assertFieldsEqual(expectedArgument::isStarProjection, actualArgument::isStarProjection)
            if (!expectedArgument.isStarProjection) {
                context.assertFieldsEqual(expectedArgument::getProjectionKind, actualArgument::getProjectionKind)
                visitType(expectedArgument.type, actualArgument.type, context.nextLevel("Type argument type"))
            }
        }
    }

    private fun <T> Context.assertEquals(expected: T?, actual: T?, subject: String) {
        if (expected != actual)
            fail(
                buildString {
                    append("Comparing $subject:\n")
                    append("$expected is not equal to $actual\n")
                    append(this@assertEquals.toString())
                }
            )
    }

    private fun <T> Context.assertFieldsEqual(expected: KCallable<T>, actual: KCallable<T>) {
        val expectedValue = expected.call()
        val actualValue = actual.call()

        assertEquals(expectedValue, actualValue, "fields \"$expected\"")
    }

    private fun <T> Context.assertSetsEqual(expected: Set<T>, actual: Set<T>, subject: String) {
        val expectedMinusActual = expected.subtract(actual)
        val actualMinusExpected = actual.subtract(expected)

        if (expectedMinusActual.isNotEmpty() || actualMinusExpected.isNotEmpty())
            fail(
                buildString {
                    append("Comparing $subject:\n")
                    append("$expected is not equal to $actual\n")
                    append("Expected size: ${expected.size}\n")
                    append("Actual size: ${actual.size}\n")
                    append("Expected minus actual: $expectedMinusActual\n")
                    append("Actual minus expected: $actualMinusExpected\n")
                    append(this@assertSetsEqual.toString())
                }
            )
    }

    override fun visitPackageViewDescriptor(expected: PackageViewDescriptor, context: Context) =
        fail("Comparison of package views not supported")

    override fun visitPackageFragmentDescriptor(expected: PackageFragmentDescriptor, context: Context) =
        fail("Comparison of package fragments not supported")

    override fun visitScriptDescriptor(expected: ScriptDescriptor, context: Context) =
        fail("Comparison of script descriptors not supported")

    override fun visitVariableDescriptor(expected: VariableDescriptor, context: Context) =
        fail("Comparison of variables not supported")
}
