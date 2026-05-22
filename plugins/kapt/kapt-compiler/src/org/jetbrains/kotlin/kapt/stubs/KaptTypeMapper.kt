/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt.stubs

import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.constant.AnnotationValue
import org.jetbrains.kotlin.constant.KClassValue
import org.jetbrains.kotlin.ir.types.impl.IrErrorClassImpl
import org.jetbrains.kotlin.ir.util.classId
import org.jetbrains.kotlin.load.kotlin.internalName
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.resolve.jvm.JvmPrimitiveType
import org.jetbrains.org.objectweb.asm.Type

internal object KaptTypeMapper {
    fun mapAnnotationClassId(annotationValue: AnnotationValue.Value): Type {
        val classId = annotationValue.classId
        if (classId == IrErrorClassImpl.classId)
            return Type.getObjectType(ClassId.topLevel(KaptStubConverter.NON_EXISTENT_CLASS_NAME).internalName)
        return Type.getObjectType(classId.internalName)
    }

    fun mapKClassValue(kClassValue: KClassValue): Type {
        val value = kClassValue.value
        require(value is KClassValue.Value.NormalClass) { "Local classes are not supported here: $value" }

        (val classId, val arrayDimensions = arrayNestedness) = value.value
        if (arrayDimensions > 0) {
            return Type.getType("[" + mapKClassValue(KClassValue(classId, arrayDimensions - 1)).descriptor)
        }

        if (classId.packageFqName == StandardNames.BUILT_INS_PACKAGE_FQ_NAME) {
            val primitiveType = PrimitiveType.getByShortName(classId.relativeClassName.asString())
            if (primitiveType != null) {
                return Type.getType(JvmPrimitiveType.get(primitiveType).desc)
            }
            val primitiveArrayType = PrimitiveType.getByShortArrayName(classId.relativeClassName.asString())
            if (primitiveArrayType != null) {
                return Type.getType("[" + JvmPrimitiveType.get(primitiveArrayType).desc)
            }
        }

        return Type.getType("L" + classId.internalName + ";")
    }
}
