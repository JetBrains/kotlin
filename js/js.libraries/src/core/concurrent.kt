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

package kotlin


// Note:
// Right now we don't want to have neither 'volatile' nor 'synchronized' at runtime,
// so they annotated as `external` to avoid warnings/errors from some minifiers.
// They was reserved word in ECMAScript 2, but is not since ECMAScript 5.

// Additional note:
// Although it's reasonable to mark these annotations as `@native` (`external` since 1.1),
// we prohibit marking annotations this way.
// TODO: Another workaround is required to remove these annotations from kotlin.js

@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FIELD)
@Retention(AnnotationRetention.SOURCE)
public annotation class Volatile

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
@Retention(AnnotationRetention.SOURCE)
public annotation class Synchronized

@kotlin.internal.InlineOnly
public inline fun <R> synchronized(@Suppress("UNUSED_PARAMETER") lock: Any, crossinline block: () -> R): R = block()
