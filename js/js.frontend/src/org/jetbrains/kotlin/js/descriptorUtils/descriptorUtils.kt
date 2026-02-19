/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.descriptorUtils

import com.intellij.openapi.util.text.StringUtil
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.types.KotlinType

val KotlinType.nameIfStandardType: Name?
    get() = constructor.declarationDescriptor?.takeIf(KotlinBuiltIns::isBuiltIn)?.name

@Deprecated(message = "Use getKotlinTypeFqName(Boolean) instead")
fun KotlinType.getJetTypeFqName(printTypeArguments: Boolean): String = getKotlinTypeFqName(printTypeArguments)

fun KotlinType.getKotlinTypeFqName(printTypeArguments: Boolean): String {
    val declaration = requireNotNull(constructor.declarationDescriptor) {
        "declarationDescriptor is null for constructor = $constructor with ${constructor.javaClass}"
    }
    if (declaration is TypeParameterDescriptor) {
        return StringUtil.join(declaration.upperBounds, { type -> type.getKotlinTypeFqName(printTypeArguments) }, "&")
    }

    val typeArguments = arguments
    val typeArgumentsAsString = if (printTypeArguments && !typeArguments.isEmpty()) {
        val joinedTypeArguments = StringUtil.join(typeArguments, { projection -> projection.type.getKotlinTypeFqName(false) }, ", ")

        "<$joinedTypeArguments>"
    }
    else {
        ""
    }

    return DescriptorUtils.getFqName(declaration).asString() + typeArgumentsAsString
}
