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

package org.jetbrains.k2js.analyze

import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.jet.lang.resolve.DescriptorUtils
import org.jetbrains.jet.lang.diagnostics.Errors
import org.jetbrains.jet.lang.resolve.DiagnosticsWithSuppression
import org.jetbrains.k2js.PredefinedAnnotation
import org.jetbrains.jet.lang.diagnostics.Diagnostic
import org.jetbrains.jet.lang.diagnostics.Severity
import org.jetbrains.jet.lang.psi.JetFile
import org.jetbrains.k2js.config.LibrarySourcesConfig

class SuppressUnusedParameterForJsNative : DiagnosticsWithSuppression.SuppressStringProvider {
    override fun get(annotationDescriptor: AnnotationDescriptor): List<String> {
        val descriptor = DescriptorUtils.getClassDescriptorForType(annotationDescriptor.getType())
        if (PredefinedAnnotation.NATIVE.fqName.asString() == DescriptorUtils.getFqName(descriptor).asString()) {
            return listOf(Errors.UNUSED_PARAMETER.getName().toLowerCase())
        }

        return listOf()
    }
}

class SuppressWarningsFromExternalModules : DiagnosticsWithSuppression.DiagnosticSuppressor {
    override fun isSuppressed(diagnostic: Diagnostic): Boolean {
        val file = diagnostic.getPsiFile()
        return diagnostic.getSeverity() == Severity.WARNING &&
               file is JetFile && file.getUserData(LibrarySourcesConfig.EXTERNAL_MODULE_NAME) != null
    }
}