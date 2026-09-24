/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.resolve

// NOTE: this class is used for error reporting. But in order to pass plugin verification, it should derive directly from java's Annotation
// and implement annotationType method (see #KT-16621 for details).
@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
class InvalidScriptResolverAnnotation(val name: String, @Suppress("unused") val annParams: List<Pair<String?, Any?>>?, val error: Exception? = null) : Annotation, java.lang.annotation.Annotation {
    override fun annotationType(): Class<out Annotation> = InvalidScriptResolverAnnotation::class.java
}
