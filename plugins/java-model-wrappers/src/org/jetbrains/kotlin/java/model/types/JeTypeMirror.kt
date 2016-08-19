/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.java.model.types

import javax.lang.model.element.AnnotationMirror
import javax.lang.model.type.TypeMirror

//TODO support type annotations
interface JeTypeMirror : TypeMirror {
    override fun getAnnotationMirrors() = emptyList<AnnotationMirror>()

    override fun <A : Annotation> getAnnotation(annotationClass: Class<A>?) = null

    @Suppress("UNCHECKED_CAST")
    override fun <A : Annotation?> getAnnotationsByType(annotationType: Class<A>): Array<A> {
        return java.lang.reflect.Array.newInstance(annotationType, 0) as Array<A>
    }
}