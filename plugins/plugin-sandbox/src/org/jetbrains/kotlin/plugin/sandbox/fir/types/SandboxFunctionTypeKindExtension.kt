/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.plugin.sandbox.fir.types

import org.jetbrains.kotlin.builtins.functions.FunctionTypeKind
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.extensions.FirFunctionTypeKindExtension
import org.jetbrains.kotlin.plugin.sandbox.fir.fqn
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

class SandboxFunctionTypeKindExtension(session: FirSession) : FirFunctionTypeKindExtension(session) {
    override fun FunctionTypeKindRegistrar.registerKinds() {
        registerKind(InlineablePluginFunction, KInlineableFunction)
        registerKind(NotInlineablePluginFunction, KNotInlineableFunction)
    }
}

object PluginFunctionalNames {
    val BASE_PACKAGE = FqName.topLevel(Name.identifier("some"))
    val MY_INLINEABLE_ANNOTATION_CLASS_ID = ClassId.topLevel("MyInlineable".fqn())
    val INLINEABLE_NAME_PREFIX = "MyInlineableFunction"
    val KINLINEABLE_NAME_PREFIX = "KMyInlineableFunction"

    val FULL_INLINEABLE_NAME_PREFIX = BASE_PACKAGE.child(Name.identifier(INLINEABLE_NAME_PREFIX)).asString()

    val MY_NOT_INLINEABLE_ANNOTATION_CLASS_ID = ClassId.topLevel("MyNotInlineable".fqn())
    val NOT_INLINEABLE_NAME_PREFIX = "MyNotInlineableFunction"
    val KNOT_INLINEABLE_NAME_PREFIX = "KMyNotInlineableFunction"

    val FULL_NOT_INLINEABLE_NAME_PREFIX = BASE_PACKAGE.child(Name.identifier(INLINEABLE_NAME_PREFIX)).asString()
}

object InlineablePluginFunction : FunctionTypeKind(
    PluginFunctionalNames.BASE_PACKAGE,
    PluginFunctionalNames.INLINEABLE_NAME_PREFIX,
    annotationOnInvokeClassId = PluginFunctionalNames.MY_INLINEABLE_ANNOTATION_CLASS_ID,
    isReflectType = false,
    isInlineable = true,
) {
    override val prefixForTypeRender: String
        get() = "@MyInlineable"

    override val serializeAsFunctionWithAnnotationUntil: String
        get() = LanguageVersion.KOTLIN_2_1.versionString

    override fun reflectKind(): FunctionTypeKind = KInlineableFunction
}

object KInlineableFunction : FunctionTypeKind(
    PluginFunctionalNames.BASE_PACKAGE,
    PluginFunctionalNames.KINLINEABLE_NAME_PREFIX,
    PluginFunctionalNames.MY_INLINEABLE_ANNOTATION_CLASS_ID,
    isReflectType = true,
    isInlineable = false,
) {
    override val serializeAsFunctionWithAnnotationUntil: String
        get() = LanguageVersion.KOTLIN_2_1.versionString

    override fun nonReflectKind(): FunctionTypeKind = InlineablePluginFunction
}

object NotInlineablePluginFunction : FunctionTypeKind(
    PluginFunctionalNames.BASE_PACKAGE,
    PluginFunctionalNames.NOT_INLINEABLE_NAME_PREFIX,
    PluginFunctionalNames.MY_NOT_INLINEABLE_ANNOTATION_CLASS_ID,
    isReflectType = false,
    isInlineable = false,
) {
    override val prefixForTypeRender: String
        get() = "@MyInlineable"

    override val serializeAsFunctionWithAnnotationUntil: String
        get() = LanguageVersion.KOTLIN_2_1.versionString

    override fun reflectKind(): FunctionTypeKind = KNotInlineableFunction
}

object KNotInlineableFunction : FunctionTypeKind(
    PluginFunctionalNames.BASE_PACKAGE,
    PluginFunctionalNames.KNOT_INLINEABLE_NAME_PREFIX,
    PluginFunctionalNames.MY_NOT_INLINEABLE_ANNOTATION_CLASS_ID,
    isReflectType = true,
    isInlineable = false,
) {
    override val serializeAsFunctionWithAnnotationUntil: String
        get() = LanguageVersion.KOTLIN_2_1.versionString

    override fun nonReflectKind(): FunctionTypeKind = NotInlineablePluginFunction
}
