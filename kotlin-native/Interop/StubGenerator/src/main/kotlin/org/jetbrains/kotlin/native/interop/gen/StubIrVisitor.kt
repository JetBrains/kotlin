/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
package org.jetbrains.kotlin.native.interop.gen

import org.jetbrains.kotlin.native.interop.indexer.StructDef

interface StubIrVisitor<T, R> {

    fun visitClass(element: ClassStub, data: T): R

    fun visitTypealias(element: TypealiasStub, data: T): R

    fun visitFunction(element: FunctionStub, data: T): R

    fun visitProperty(element: PropertyStub, data: T): R

    fun visitConstructor(constructorStub: ConstructorStub, data: T): R

    fun visitPropertyAccessor(propertyAccessor: PropertyAccessor, data: T): R

    fun visitSimpleStubContainer(simpleStubContainer: SimpleStubContainer, data: T): R
}


interface StubIrTransformer<T> {
    fun visitClass(element: ClassStub): ClassStub

    fun visitTypealias(element: TypealiasStub): TypealiasStub

    fun visitFunction(element: FunctionStub): FunctionStub

    fun visitProperty(element: PropertyStub): PropertyStub

    fun visitConstructor(constructorStub: ConstructorStub): ConstructorStub

    fun visitPropertyAccessor(propertyAccessor: PropertyAccessor): PropertyAccessor

    fun visitSimpleStubContainer(simpleStubContainer: SimpleStubContainer): SimpleStubContainer

    fun visitFunctionParameter(element: FunctionParameterStub): FunctionParameterStub

    fun visitAnnotation(element: AnnotationStub): AnnotationStub

    fun visitReceiverParameter(element: ReceiverParameterStub): ReceiverParameterStub

}

class DeepCopyForManagedWrapper(val originalClass: ClassStub, val context: StubsBuildingContext) : StubIrTransformer<Unit> {
    override fun visitClass(element: ClassStub): ClassStub = error("Not implemented")

    override fun visitTypealias(element: TypealiasStub): TypealiasStub = error("Not implemented")

    override fun visitFunction(element: FunctionStub): FunctionStub {
        return FunctionStub(
                name = element.name,
                returnType = transformCPointerToManaged(element.returnType),
                parameters = element.parameters.map { visitFunctionParameter(it) },
                origin = element.origin,
                annotations = emptyList(),
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
                annotations = emptyList(),
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
        // TODO: it'd be nice to check inheritance from SkiaRefCnt or CPlusPlusClass,
        // but unable to do that at StubType level.
        if (!argument.type.classifier.topLevelName.let { it.startsWith("Sk") || it.startsWith("Gr")}) return type
        if (argument.type.classifier.pkg != "org.jetbrains.skiko.skia.native") return type

        return ClassifierStubType(managedWrapperClassifier(argument.type.classifier) ?: return type, nullable = type.nullable)
    }

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

    private val knownPrefixes = listOf("Sk", "Gr", "skia::textlayout::")

    fun managedWrapperClassifier(cppClassifier: Classifier): Classifier? {
        val prefix = knownPrefixes.singleOrNull {
            cppClassifier.topLevelName.startsWith(it)
        } ?: return null
        if (cppClassifier.topLevelName == "SkString") return null
        // TODO: We only manage C++ classes, not structs for now.
        if (!(context as StubsBuildingContextImpl).isClass(cppClassifier.topLevelName)) return null

        return Classifier.topLevel(cppClassifier.pkg, cppClassifier.topLevelName.drop(prefix.length))
    }

}
