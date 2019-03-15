/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.jvm

// these are used in common generated code in stdlib

// TODO: find how to deprecate these ones

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.SOURCE)
public actual annotation class Volatile

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
@Retention(AnnotationRetention.SOURCE)
public actual annotation class Synchronized
