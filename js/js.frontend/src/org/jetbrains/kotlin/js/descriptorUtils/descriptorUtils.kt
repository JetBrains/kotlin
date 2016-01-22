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

package org.jetbrains.kotlin.js.descriptorUtils

import com.intellij.openapi.util.text.StringUtil
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.utils.addToStdlib.check

val KotlinType.nameIfStandardType: Name?
    get() {
        return constructor.declarationDescriptor
                ?.check { descriptor ->
                    descriptor.builtIns.isBuiltInPackageFragment(descriptor.containingDeclaration as? PackageFragmentDescriptor)
                }
                ?.name
    }

fun KotlinType.getJetTypeFqName(printTypeArguments: Boolean): String {
    val declaration = requireNotNull(constructor.declarationDescriptor)
    if (declaration is TypeParameterDescriptor) {
        return StringUtil.join(declaration.upperBounds, { type -> type.getJetTypeFqName(printTypeArguments) }, "&")
    }

    val typeArguments = arguments
    val typeArgumentsAsString: String

    if (printTypeArguments && !typeArguments.isEmpty()) {
        val joinedTypeArguments = StringUtil.join(typeArguments, { projection -> projection.type.getJetTypeFqName(false) }, ", ")

        typeArgumentsAsString = "<" + joinedTypeArguments + ">"
    } else {
        typeArgumentsAsString = ""
    }

    return DescriptorUtils.getFqName(declaration).asString() + typeArgumentsAsString
}

fun ClassDescriptor.hasPrimaryConstructor(): Boolean = unsubstitutedPrimaryConstructor != null
