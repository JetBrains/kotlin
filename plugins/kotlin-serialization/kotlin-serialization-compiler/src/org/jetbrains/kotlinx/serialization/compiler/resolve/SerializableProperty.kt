/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlinx.serialization.compiler.resolve

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.fir.scopes.impl.overrides
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.module
import org.jetbrains.kotlin.psi.KtDeclarationWithInitializer
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.ValueArgument
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.source.getPsi
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.checker.SimpleClassicTypeSystemContext.asSimpleType
import org.jetbrains.kotlinx.serialization.compiler.backend.common.analyzeSpecialSerializers
import org.jetbrains.kotlinx.serialization.compiler.backend.common.genericIndex
import org.jetbrains.kotlinx.serialization.compiler.backend.common.overridenSerializer
import org.jetbrains.kotlinx.serialization.compiler.backend.common.serialNameValue

class SerializableProperty(
    override val descriptor: PropertyDescriptor,
    override val isConstructorParameterWithDefault: Boolean,
    hasBackingField: Boolean,
    declaresDefaultValue: Boolean
) : ISerializableProperty<PropertyDescriptor, KotlinType> {
    override val name = descriptor.annotations.serialNameValue ?: descriptor.name.asString()
    override val type = descriptor.type
    override val genericIndex = type.genericIndex
    val module = descriptor.module
    override val serializableWith = descriptor.serializableWith ?: analyzeSpecialSerializers(module, descriptor.annotations)?.defaultType
    override val optional = !descriptor.annotations.serialRequired && declaresDefaultValue
    override val transient = descriptor.annotations.serialTransient || !hasBackingField
    val annotationsWithArguments: List<Triple<ClassDescriptor, List<ValueArgument>, List<ValueParameterDescriptor>>> =
        descriptor.annotationsWithArguments()
}

interface ISerializableProperty<D, T> {
    val descriptor: D
    val isConstructorParameterWithDefault: Boolean
    val name: String
    val type: T
    val genericIndex: Int?
    val serializableWith: T?
    val optional: Boolean
    val transient: Boolean
}

class IrSerializableProperty(
    override val descriptor: IrProperty,
    override val isConstructorParameterWithDefault: Boolean,
    hasBackingField: Boolean,
    declaresDefaultValue: Boolean
) : ISerializableProperty<IrProperty, IrSimpleType> {
    override val name = descriptor.annotations.serialNameValue ?: descriptor.name.asString()
    override val type = descriptor.getter!!.returnType as IrSimpleType
    override val genericIndex = type.genericIndex
    override val serializableWith = type.overridenSerializer /* ?:analyzeSpecialSerializers(module, descriptor.annotations)?.defaultType */ // TODO
    override val optional = !descriptor.annotations.hasAnnotation(SerializationAnnotations.requiredAnnotationFqName) && declaresDefaultValue
    override val transient = descriptor.annotations.hasAnnotation(SerializationAnnotations.serialTransientFqName) || !hasBackingField
}