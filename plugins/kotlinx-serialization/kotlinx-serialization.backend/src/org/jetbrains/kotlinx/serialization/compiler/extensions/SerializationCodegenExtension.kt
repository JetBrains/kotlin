/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.compiler.extensions

import org.jetbrains.kotlin.codegen.ImplementationBodyCodegen
import org.jetbrains.kotlin.codegen.StackValue
import org.jetbrains.kotlin.codegen.extensions.ExpressionCodegenExtension
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapperBase
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.types.TypeSystemCommonBackendContext
import org.jetbrains.kotlin.types.model.KotlinTypeMarker
import org.jetbrains.kotlinx.serialization.compiler.backend.jvm.*
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.tree.InsnList
import org.jetbrains.org.objectweb.asm.tree.MethodInsnNode

open class SerializationCodegenExtension @JvmOverloads constructor(val metadataPlugin: SerializationDescriptorSerializerPlugin? = null) : ExpressionCodegenExtension {
    override fun generateClassSyntheticParts(codegen: ImplementationBodyCodegen) {
        SerialInfoCodegenImpl.generateSerialInfoImplBody(codegen)
        SerializableCodegenImpl.generateSerializableExtensions(codegen)
        SerializerCodegenImpl.generateSerializerExtensions(codegen, metadataPlugin)
        SerializableCompanionCodegenImpl.generateSerializableExtensions(codegen)
    }

    override val shouldGenerateClassSyntheticPartsInLightClassesMode: Boolean
        get() = false
}