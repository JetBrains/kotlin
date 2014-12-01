/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.k2js.resolve

import org.jetbrains.jet.lang.resolve.AdditionalCheckerProvider
import org.jetbrains.jet.lang.resolve.AnnotationChecker
import org.jetbrains.jet.lang.psi.JetDeclaration
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor
import org.jetbrains.jet.lang.diagnostics.DiagnosticSink
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor
import org.jetbrains.jet.lang.psi.JetNamedFunction
import org.jetbrains.k2js.resolve.diagnostics.ErrorsJs
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns
import org.jetbrains.jet.lang.types.TypeUtils
import org.jetbrains.k2js.PredefinedAnnotation
import org.jetbrains.jet.lang.resolve.DescriptorUtils
import org.jetbrains.k2js.translate.utils.AnnotationsUtils
import org.jetbrains.jet.lang.descriptors.Visibilities

public object KotlinJsDeclarationCheckerProvider : AdditionalCheckerProvider {
    override val annotationCheckers: List<AnnotationChecker> = listOf(NativeInvokeChecker(), NativeGetterChecker(), NativeSetterChecker())
}

private abstract class AbstractNativeAnnotationsChecker(private val requiredAnnotation: PredefinedAnnotation) : AnnotationChecker {

    open fun additionalCheck(declaration: JetNamedFunction, descriptor: FunctionDescriptor, diagnosticHolder: DiagnosticSink) {}

    override fun check(declaration: JetDeclaration, descriptor: DeclarationDescriptor, diagnosticHolder: DiagnosticSink) {
        val annotationDescriptor = descriptor.getAnnotations().findAnnotation(requiredAnnotation.fqName)
        if (annotationDescriptor == null) return

        if (declaration !is JetNamedFunction || descriptor !is FunctionDescriptor) {
            diagnosticHolder.report(ErrorsJs.NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN.on(declaration, annotationDescriptor.getType()))
            return
        }

        val isMember = !DescriptorUtils.isTopLevelDeclaration(descriptor) && descriptor.getVisibility() != Visibilities.LOCAL
        val isExtension = DescriptorUtils.isExtension(descriptor)

        if (isMember && (isExtension || !AnnotationsUtils.isNativeObject(descriptor)) ||
            !isMember && !isExtension
        ) {
            diagnosticHolder.report(ErrorsJs.NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN.on(declaration, annotationDescriptor.getType()))
        }

        additionalCheck(declaration, descriptor, diagnosticHolder)
    }
}

public class NativeInvokeChecker : AbstractNativeAnnotationsChecker(PredefinedAnnotation.NATIVE_INVOKE)

private abstract class AbstractNativeIndexerChecker(
        requiredAnnotation: PredefinedAnnotation,
        private val indexerKind: String,
        private val requiredParametersCount: Int
) : AbstractNativeAnnotationsChecker(requiredAnnotation) {

    override fun additionalCheck(declaration: JetNamedFunction, descriptor: FunctionDescriptor, diagnosticHolder: DiagnosticSink) {
        val parameters = descriptor.getValueParameters()
        if (parameters.size() > 0) {
            val firstParamClassDescriptor = DescriptorUtils.getClassDescriptorForType(parameters.get(0).getType())
            if (firstParamClassDescriptor != KotlinBuiltIns.getInstance().getString() &&
                !DescriptorUtils.isSubclass(firstParamClassDescriptor, KotlinBuiltIns.getInstance().getNumber())
            ) {
                diagnosticHolder.report(ErrorsJs.NATIVE_INDEXER_KEY_SHOULD_BE_STRING_OR_NUMBER.on(declaration.getValueParameters().first, indexerKind))
            }
        }

        if (parameters.size() != requiredParametersCount) {
            diagnosticHolder.report(ErrorsJs.NATIVE_INDEXER_WRONG_PARAMETER_COUNT.on(declaration, requiredParametersCount, indexerKind))
        }

        for (parameter in declaration.getValueParameters()) {
            if (parameter.hasDefaultValue()) {
                diagnosticHolder.report(ErrorsJs.NATIVE_INDEXER_CAN_NOT_HAVE_DEFAULT_ARGUMENTS.on(parameter, indexerKind))
            }
        }
    }
}

public class NativeGetterChecker : AbstractNativeIndexerChecker(PredefinedAnnotation.NATIVE_GETTER, "getter", requiredParametersCount = 1) {
    override fun additionalCheck(declaration: JetNamedFunction, descriptor: FunctionDescriptor, diagnosticHolder: DiagnosticSink) {
        super.additionalCheck(declaration, descriptor, diagnosticHolder)

        if (!TypeUtils.isNullableType(descriptor.getReturnType())) {
            diagnosticHolder.report(ErrorsJs.NATIVE_GETTER_RETURN_TYPE_SHOULD_BE_NULLABLE.on(declaration))
        }
    }
}

public class NativeSetterChecker : AbstractNativeIndexerChecker(PredefinedAnnotation.NATIVE_SETTER, "setter", requiredParametersCount = 2)
