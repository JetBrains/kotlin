/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.resolve

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtUserType
import org.jetbrains.kotlin.resolve.BindingTraceContext
import org.jetbrains.kotlin.resolve.constants.evaluate.ConstantExpressionEvaluator
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.utils.tryCreateCallableMappingFromNamedArgs
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.full.primaryConstructor

internal val KtAnnotationEntry.typeName: String get() = (typeReference?.typeElement as? KtUserType)?.referencedName.orAnonymous()

internal fun String?.orAnonymous(kind: String = ""): String =
        this ?: "<anonymous" + (if (kind.isNotBlank()) " $kind" else "") + ">"

internal fun constructAnnotation(psi: KtAnnotationEntry, targetClass: KClass<out Annotation>, project: Project): Annotation {
    val module = ModuleDescriptorImpl(Name.special("<script-annotations-preprocessing>"), LockBasedStorageManager("scriptAnnotationsPreprocessing"), DefaultBuiltIns.Instance)
    val evaluator = ConstantExpressionEvaluator(module, LanguageVersionSettingsImpl.DEFAULT, project)
    val trace = BindingTraceContext()

    val valueArguments = psi.valueArguments.map { arg ->
        val result = evaluator.evaluateToConstantValue(arg.getArgumentExpression()!!, trace, TypeUtils.NO_EXPECTED_TYPE)
        // TODO: consider inspecting `trace` to find diagnostics reported during the computation (such as division by zero, integer overflow, invalid annotation parameters etc.)
        val argName = arg.getArgumentName()?.asName?.toString()
        argName to result?.value
    }
    val mappedArguments: Map<KParameter, Any?> =
        tryCreateCallableMappingFromNamedArgs(targetClass.constructors.first(), valueArguments)
        ?: return InvalidScriptResolverAnnotation(psi.typeName, valueArguments)

    try {
        return targetClass.primaryConstructor!!.callBy(mappedArguments)
    }
    catch (ex: Exception) {
        return InvalidScriptResolverAnnotation(psi.typeName, valueArguments, ex)
    }
}

// NOTE: this class is used for error reporting. But in order to pass plugin verification, it should derive directly from java's Annotation
// and implement annotationType method (see #KT-16621 for details).
// TODO: instead of the workaround described above, consider using a sum-type for returning errors from constructAnnotation
@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
class InvalidScriptResolverAnnotation(val name: String, val annParams: List<Pair<String?, Any?>>?, val error: Exception? = null) : Annotation, java.lang.annotation.Annotation {
    override fun annotationType(): Class<out Annotation> = InvalidScriptResolverAnnotation::class.java
}
