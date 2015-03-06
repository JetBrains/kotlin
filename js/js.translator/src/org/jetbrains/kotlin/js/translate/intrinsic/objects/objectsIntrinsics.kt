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

import com.google.dart.compiler.backend.js.ast.JsArrayAccess
import com.google.dart.compiler.backend.js.ast.JsExpression
import org.jetbrains.kotlin.backend.common.builtins.DefaultObjectMapping
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.js.config.LibrarySourcesConfig
import org.jetbrains.kotlin.js.translate.context.TranslationContext
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

class DefaultClassObjectIntrinsic(val fqName: FqName, val moduleName: String): ObjectIntrinsic {
    override fun apply(context: TranslationContext): JsExpression {
        val nameRef = context.getQualifiedReference(fqName)
        return JsAstUtils.replaceRootReference(
                nameRef,
                JsArrayAccess(context.namer().kotlin("modules"), context.program().getStringLiteral(moduleName)))
    }
}

public class ObjectIntrinsics : DefaultObjectMapping() {
    public fun getIntrinsic(classDescriptor: ClassDescriptor): ObjectIntrinsic {
        if (!hasMappingToObject(classDescriptor)) return NO_OBJECT_INTRINSIC

        val containingDeclaration = classDescriptor.getContainingDeclaration()
        val name = Name.identifier(containingDeclaration.getName().asString() + "DefaultObject")

        return DefaultClassObjectIntrinsic(FqName("kotlin.js.internal").child(name), LibrarySourcesConfig.STDLIB_JS_MODULE_NAME)
    }
}

public trait ObjectIntrinsic {
    fun apply(context: TranslationContext): JsExpression
    fun exists(): Boolean = true
}

object NO_OBJECT_INTRINSIC : ObjectIntrinsic {
    override fun apply(context: TranslationContext): JsExpression =
            throw UnsupportedOperationException("ObjectIntrinsic#NO_OBJECT_INTRINSIC_#apply")

    override fun exists(): Boolean = false
}