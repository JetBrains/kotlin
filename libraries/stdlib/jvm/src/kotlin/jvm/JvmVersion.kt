/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */
@file:JvmVersion
package kotlin.jvm

@Target(AnnotationTarget.FILE, AnnotationTarget.CLASS, AnnotationTarget.PROPERTY, AnnotationTarget.CONSTRUCTOR, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
internal annotation class JvmVersion(public val minimum: Int = 6, public val maximum: Int = 100)
