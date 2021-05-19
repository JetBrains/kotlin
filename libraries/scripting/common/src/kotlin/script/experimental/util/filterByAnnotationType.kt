/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.util

import kotlin.reflect.KClass
import kotlin.script.experimental.api.*

fun <A : Annotation> Iterable<ScriptSourceAnnotation<*>>.filterByAnnotationType(
    annotationType: KClass<out A>
): List<ScriptSourceAnnotation<A>> = filter { annotationType.isInstance(it.annotation) }
    .map {
        @Suppress("UNCHECKED_CAST")
        it as ScriptSourceAnnotation<A>
    }

inline fun <reified A : Annotation> Iterable<ScriptSourceAnnotation<*>>.filterByAnnotationType(
): List<ScriptSourceAnnotation<A>> = filterByAnnotationType(A::class)