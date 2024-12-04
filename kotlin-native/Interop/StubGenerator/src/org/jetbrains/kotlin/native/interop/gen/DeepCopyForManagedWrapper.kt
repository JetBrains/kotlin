/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.native.interop.gen

class DeepCopyForManagedWrapper(val originalClass: ClassStub, val context: StubsBuildingContext) : StubIrTransformer {
    override fun visitClass(element: ClassStub): ClassStub = error("Not implemented")

    override fun visitTypealias(element: TypealiasStub): TypealiasStub = error("Not implemented")

    override fun visitFunction(element: FunctionStub): FunctionStub {
        return FunctionStub(
                name = element.name,
                returnType = transformCPointerToManaged(element.returnType),
                parameters = element.parameters.map { visitFunctionParameter(it) },
                origin = element.origin,
                annotations = mutableListOf(),
                external = false,
                receiver = element.receiver?.let { visitReceiverParameter(it) },
                modality = element.modality,
                typeParameters = element.typeParameters,
                isOverride = element.isOverride,
                hasStableParameterNames = element.hasStableParameterNames
        )
    }

    override fun visitProperty(element: PropertyStub): PropertyStub {
        return PropertyStub(
                name = element.name,
                type = element.type,
                origin = element.origin,
                annotations = mutableListOf(),
                receiverType = element.receiverType,
                modality = element.modality,
                isOverride = element.isOverride,
                kind = when(element.kind) {
                    is PropertyStub.Kind.Val -> PropertyStub.Kind.Val(
                            visitPropertyAccessor(element.kind.getter) as PropertyAccessor.Getter
                    )
                    is PropertyStub.Kind.Var -> PropertyStub.Kind.Var(
                            visitPropertyAccessor(element.kind.getter) as PropertyAccessor.Getter,
                            visitPropertyAccessor(element.kind.setter) as PropertyAccessor.Setter
                    )
                    is PropertyStub.Kind.Constant -> PropertyStub.Kind.Constant(element.kind.constant)
                }
        )
    }

    override fun visitConstructor(constructorStub: ConstructorStub): ConstructorStub {
        return if (constructorStub.isPrimary)
            ConstructorStub(
                    parameters = listOf(
                            FunctionParameterStub(
                                    name = "cpp",
                                    type = ClassifierStubType(originalClass.classifier)
                            ),
                            FunctionParameterStub(
                                    name = "managed",
                                    type = ClassifierStubType(Classifier.topLevel("kotlin", "Boolean"))
                            )
                    ),
                    isPrimary = true,
                    visibility = constructorStub.visibility,
                    origin = constructorStub.origin
            )
        else ConstructorStub(
                parameters = constructorStub.parameters.map { visitFunctionParameter(it) },
                annotations = emptyList(),
                isPrimary = constructorStub.isPrimary,
                visibility = constructorStub.visibility,
                origin = constructorStub.origin
        )
    }

    override fun visitPropertyAccessor(propertyAccessor: PropertyAccessor): PropertyAccessor {
        return when (propertyAccessor) {
            is PropertyAccessor.Getter.SimpleGetter ->
                PropertyAccessor.Getter.SimpleGetter(
                        propertyAccessor.annotations.map { visitAnnotation(it) },
                        constant = propertyAccessor.constant
                )
            is PropertyAccessor.Getter.ExternalGetter ->
                PropertyAccessor.Getter.SimpleGetter( // TODO: is it right?
                        propertyAccessor.annotations.map { visitAnnotation(it) },
                        constant = null
                )
            is PropertyAccessor.Setter.SimpleSetter ->
                PropertyAccessor.Setter.SimpleSetter(
                        propertyAccessor.annotations.map { visitAnnotation(it) }
                )
            is PropertyAccessor.Setter.ExternalSetter ->
                PropertyAccessor.Setter.SimpleSetter( // TODO: is it right?
                        propertyAccessor.annotations.map { visitAnnotation(it) }
                )
            else -> error("Not implemented yet $propertyAccessor")
        }
    }

    override fun visitSimpleStubContainer(simpleStubContainer: SimpleStubContainer): SimpleStubContainer = SimpleStubContainer()

    private fun transformCPointerToManaged(type: StubType): StubType {
        if (type !is ClassifierStubType) return type
        if (type.classifier.topLevelName != "CPointer" && type.classifier.topLevelName != "CValuesRef") return type
        if (type.classifier.pkg != "kotlinx.cinterop") return type
        val argument = type.typeArguments.single() as? TypeArgumentStub ?: return type
        if (argument.type !is ClassifierStubType) return type
        val newClassifier = managedWrapperClassifier(argument.type.classifier) ?: return type

        return ClassifierStubType(newClassifier, nullable = type.nullable)
    }

    fun managedWrapperClassifier(cppClassifier: Classifier): Classifier? =
            (context as StubsBuildingContextImpl).managedWrapperClassifier(cppClassifier)

    override fun visitFunctionParameter(element: FunctionParameterStub): FunctionParameterStub {
        return FunctionParameterStub(
                name = element.name,
                type = transformCPointerToManaged(element.type),
                annotations = element.annotations.map { visitAnnotation(it) },
                isVararg = element.isVararg
        )
    }
    override fun visitReceiverParameter(element: ReceiverParameterStub): ReceiverParameterStub {
        return ReceiverParameterStub(
                type = element.type
        )
    }

    override fun visitAnnotation(element: AnnotationStub): AnnotationStub = element
}
