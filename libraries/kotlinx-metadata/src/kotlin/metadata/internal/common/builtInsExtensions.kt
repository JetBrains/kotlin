/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.metadata.internal.common

import kotlin.metadata.*

/**
 * This object serves as an internal accessor for extensions that are available only when metadata is loaded from .kotlin_builtins file.
 */
public object BuiltInExtensionsAccessor {
    public val KmClass.annotations: MutableList<KmAnnotation>
        get() = builtins.annotations

    public var KmPackage.fqName: String?
        get() = builtins.fqName
        set(value) {
            builtins.fqName = value
        }

    public val KmFunction.annotations: MutableList<KmAnnotation>
        get() = builtins.annotations

    public val KmProperty.annotations: MutableList<KmAnnotation>
        get() = builtins.annotations

    public val KmProperty.setterAnnotations: MutableList<KmAnnotation>
        get() = builtins.setterAnnotations

    public val KmProperty.getterAnnotations: MutableList<KmAnnotation>
        get() = builtins.getterAnnotations

    public var KmProperty.compileTimeValue: KmAnnotationArgument?
        get() = builtins.compileTimeValue
        set(value) {
            builtins.compileTimeValue = value
        }

    public val KmConstructor.annotations: MutableList<KmAnnotation>
        get() = builtins.annotations

    public val KmValueParameter.annotations: MutableList<KmAnnotation>
        get() = builtins.annotations

    public val KmTypeParameter.annotations: MutableList<KmAnnotation>
        get() = builtins.annotations

    public val KmType.annotations: MutableList<KmAnnotation>
        get() = builtins.annotations
}
