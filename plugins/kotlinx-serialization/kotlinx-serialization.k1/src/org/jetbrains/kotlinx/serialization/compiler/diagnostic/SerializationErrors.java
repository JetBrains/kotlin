/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.compiler.diagnostic;

import com.intellij.psi.PsiElement;
import org.jetbrains.kotlin.descriptors.ClassDescriptor;
import org.jetbrains.kotlin.diagnostics.*;
import org.jetbrains.kotlin.psi.KtAnnotationEntry;
import org.jetbrains.kotlin.types.KotlinType;

import static org.jetbrains.kotlin.diagnostics.Severity.ERROR;
import static org.jetbrains.kotlin.diagnostics.Severity.WARNING;

public interface SerializationErrors {
    DiagnosticFactory2<PsiElement, String, String> INLINE_CLASSES_NOT_SUPPORTED = DiagnosticFactory2.create(ERROR);
    DiagnosticFactory0<PsiElement> PLUGIN_IS_NOT_ENABLED = DiagnosticFactory0.create(WARNING);
    DiagnosticFactory0<PsiElement> ANONYMOUS_OBJECTS_NOT_SUPPORTED = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<PsiElement> INNER_CLASSES_NOT_SUPPORTED = DiagnosticFactory0.create(ERROR);


    DiagnosticFactory1<PsiElement, ClassDescriptor> COMPANION_OBJECT_AS_CUSTOM_SERIALIZER_DEPRECATED = DiagnosticFactory1.create(WARNING);

    DiagnosticFactory2<PsiElement, KotlinType, KotlinType> COMPANION_OBJECT_SERIALIZER_INSIDE_OTHER_SERIALIZABLE_CLASS = DiagnosticFactory2.create(WARNING);

    DiagnosticFactory2<PsiElement, KotlinType, KotlinType> COMPANION_OBJECT_SERIALIZER_INSIDE_NON_SERIALIZABLE_CLASS = DiagnosticFactory2.create(WARNING);

    DiagnosticFactory0<PsiElement> EXPLICIT_SERIALIZABLE_IS_REQUIRED = DiagnosticFactory0.create(WARNING);

    DiagnosticFactory0<KtAnnotationEntry> SERIALIZABLE_ANNOTATION_IGNORED = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtAnnotationEntry> NON_SERIALIZABLE_PARENT_MUST_HAVE_NOARG_CTOR = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtAnnotationEntry> PRIMARY_CONSTRUCTOR_PARAMETER_IS_NOT_A_PROPERTY = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory1<KtAnnotationEntry, String> DUPLICATE_SERIAL_NAME = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory3<PsiElement, KotlinType, String, String> DUPLICATE_SERIAL_NAME_ENUM = DiagnosticFactory3.create(ERROR);
    DiagnosticFactory1<PsiElement, KotlinType> SERIALIZER_NOT_FOUND = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory2<PsiElement, KotlinType, KotlinType> SERIALIZER_NULLABILITY_INCOMPATIBLE = DiagnosticFactory2.create(ERROR);
    DiagnosticFactory2<PsiElement, KotlinType, KotlinType> ABSTRACT_SERIALIZER_TYPE = DiagnosticFactory2.create(ERROR);
    DiagnosticFactory3<PsiElement, KotlinType, KotlinType, KotlinType> SERIALIZER_TYPE_INCOMPATIBLE = DiagnosticFactory3.create(WARNING);
    DiagnosticFactory1<PsiElement, KotlinType> LOCAL_SERIALIZER_USAGE = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory0<PsiElement> TRANSIENT_MISSING_INITIALIZER = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory0<PsiElement> GENERIC_ARRAY_ELEMENT_NOT_SUPPORTED = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory0<PsiElement> TRANSIENT_IS_REDUNDANT = DiagnosticFactory0.create(WARNING);
    DiagnosticFactory0<PsiElement> JSON_FORMAT_REDUNDANT_DEFAULT = DiagnosticFactory0.create(WARNING);
    DiagnosticFactory0<PsiElement> JSON_FORMAT_REDUNDANT = DiagnosticFactory0.create(WARNING);

    DiagnosticFactory0<PsiElement> INCORRECT_TRANSIENT = DiagnosticFactory0.create(WARNING);

    DiagnosticFactory3<KtAnnotationEntry, String, String, String> REQUIRED_KOTLIN_TOO_HIGH = DiagnosticFactory3.create(ERROR);
    DiagnosticFactory3<KtAnnotationEntry, String, String, String> PROVIDED_RUNTIME_TOO_LOW = DiagnosticFactory3.create(ERROR);

    DiagnosticFactory2<PsiElement, KotlinType, KotlinType> INCONSISTENT_INHERITABLE_SERIALINFO = DiagnosticFactory2.create(ERROR);
    DiagnosticFactory0<PsiElement> META_SERIALIZABLE_NOT_APPLICABLE = DiagnosticFactory0.create(WARNING);

    DiagnosticFactory0<PsiElement> INHERITABLE_SERIALINFO_CANT_BE_REPEATABLE = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory1<PsiElement, KotlinType> EXTERNAL_SERIALIZER_USELESS = DiagnosticFactory1.create(WARNING);

    DiagnosticFactory2<PsiElement, KotlinType, KotlinType> EXTERNAL_CLASS_NOT_SERIALIZABLE = DiagnosticFactory2.create(ERROR);
    DiagnosticFactory2<PsiElement, KotlinType, KotlinType> EXTERNAL_CLASS_IN_ANOTHER_MODULE = DiagnosticFactory2.create(ERROR);

    @SuppressWarnings("UnusedDeclaration")
    Object _initializer = new Object() {
        {
            Errors.Initializer
                    .initializeFactoryNamesAndDefaultErrorMessages(SerializationErrors.class, SerializationPluginErrorsRendering.INSTANCE);
        }
    };
}
