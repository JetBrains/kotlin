/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.jvm

import kotlin.annotation.AnnotationTarget.*

@Target(FUNCTION, PROPERTY_GETTER, PROPERTY_SETTER, FILE)
expect annotation class JvmName(val name: String)

@Target(FILE)
expect annotation class JvmMultifileClass()

expect annotation class JvmField()

@Target(FIELD)
expect annotation class Volatile()