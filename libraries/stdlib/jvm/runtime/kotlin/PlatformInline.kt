/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin

// About this suppression: we should support this typealias separately
// in JvmSupertypeUpdater to do things properly
@Suppress("TYPEALIAS_EXPANDS_TO_COMPILER_REQUIRED_ANNOTATION_ERROR")
public actual typealias PlatformInline = kotlin.jvm.JvmInline
