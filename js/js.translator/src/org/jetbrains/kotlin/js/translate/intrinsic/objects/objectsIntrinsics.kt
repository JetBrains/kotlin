/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.js.translate.intrinsic.objects

import com.google.dart.compiler.backend.js.ast.JsExpression
import org.jetbrains.kotlin.builtins.CompanionObjectMapping
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.js.translate.context.Namer
import org.jetbrains.kotlin.js.translate.context.TranslationContext
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

class DefaultClassObjectIntrinsic(val fqName: FqName): ObjectIntrinsic {
    override fun apply(context: TranslationContext): JsExpression {
        val nameRef = context.getQualifiedReference(fqName)
        return JsAstUtils.replaceRootReference(nameRef, Namer.kotlinObject())
    }
}

class ObjectIntrinsics {
    private val companionObjectMapping = CompanionObjectMapping()

    fun getIntrinsic(classDescriptor: ClassDescriptor): ObjectIntrinsic {
        if (!companionObjectMapping.hasMappingToObject(classDescriptor)) return NO_OBJECT_INTRINSIC

        val containingDeclaration = classDescriptor.containingDeclaration
        val name = Name.identifier(containingDeclaration.name.asString() + "CompanionObject")

        return DefaultClassObjectIntrinsic(FqName("kotlin.js.internal").child(name));
    }
}

interface ObjectIntrinsic {
    fun apply(context: TranslationContext): JsExpression
    fun exists(): Boolean = true
}

object NO_OBJECT_INTRINSIC : ObjectIntrinsic {
    override fun apply(context: TranslationContext): JsExpression =
            throw UnsupportedOperationException("ObjectIntrinsic#NO_OBJECT_INTRINSIC_#apply")

    override fun exists(): Boolean = false
}
