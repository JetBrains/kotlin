/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.descriptorUtils

import com.intellij.openapi.util.text.StringUtil
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.AnonymousFunctionDescriptor
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.js.config.JsConfig
import org.jetbrains.kotlin.js.translate.utils.AnnotationsUtils
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.types.KotlinType

val KotlinType.nameIfStandardType: Name?
    get() = constructor.declarationDescriptor?.takeIf(KotlinBuiltIns::isBuiltIn)?.name

fun KotlinType.getJetTypeFqName(printTypeArguments: Boolean): String {
    val declaration = requireNotNull(constructor.declarationDescriptor) {
        "declarationDescriptor is null for constructor = $constructor with ${constructor.javaClass}"
    }
    if (declaration is TypeParameterDescriptor) {
        return StringUtil.join(declaration.upperBounds, { type -> type.getJetTypeFqName(printTypeArguments) }, "&")
    }

    val typeArguments = arguments
    val typeArgumentsAsString = if (printTypeArguments && !typeArguments.isEmpty()) {
        val joinedTypeArguments = StringUtil.join(typeArguments, { projection -> projection.type.getJetTypeFqName(false) }, ", ")

        "<$joinedTypeArguments>"
    }
    else {
        ""
    }

    return DescriptorUtils.getFqName(declaration).asString() + typeArgumentsAsString
}

fun ClassDescriptor.hasPrimaryConstructor(): Boolean = unsubstitutedPrimaryConstructor != null

val DeclarationDescriptor.isCoroutineLambda: Boolean
    get() = this is AnonymousFunctionDescriptor && isSuspend


fun DeclarationDescriptor.shouldBeExported(config: JsConfig): Boolean =
        this !is DeclarationDescriptorWithVisibility || effectiveVisibility(visibility, true).shouldBeExported(config) ||
        AnnotationsUtils.getJsNameAnnotation(this) != null

private fun EffectiveVisibility.shouldBeExported(config: JsConfig): Boolean {
    if (publicApi) return true
    if (config.configuration.getBoolean(JSConfigurationKeys.FRIEND_PATHS_DISABLED)) return false
    return toVisibility() == Visibilities.INTERNAL
}