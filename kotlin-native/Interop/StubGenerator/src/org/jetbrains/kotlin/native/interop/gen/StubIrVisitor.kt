/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
package org.jetbrains.kotlin.native.interop.gen

interface StubIrVisitor<T, R> {

    fun visitClass(element: ClassStub, data: T): R

    fun visitTypealias(element: TypealiasStub, data: T): R

    fun visitFunction(element: FunctionStub, data: T): R

    fun visitProperty(element: PropertyStub, data: T): R

    fun visitConstructor(constructorStub: ConstructorStub, data: T): R

    fun visitPropertyAccessor(propertyAccessor: PropertyAccessor, data: T): R

    fun visitSimpleStubContainer(simpleStubContainer: SimpleStubContainer, data: T): R
}


interface StubIrTransformer {
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
