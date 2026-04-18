/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package kotlin.jvm

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CONSTRUCTOR, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
public expect annotation class JvmExposeBoxed(val jvmName: String = "")

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
public expect annotation class JvmRecord()

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CONSTRUCTOR)
@Retention(AnnotationRetention.BINARY)
public expect annotation class JvmOverloads()

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
@Retention(AnnotationRetention.RUNTIME)
public expect annotation class JvmStatic()

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER, AnnotationTarget.FILE)
@Retention(AnnotationRetention.BINARY)
public expect annotation class JvmName(val name: String)

@Target(AnnotationTarget.FILE)
@Retention(AnnotationRetention.SOURCE)
public expect annotation class JvmMultifileClass()

@Target(AnnotationTarget.FILE, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER, AnnotationTarget.FIELD)
@Retention(AnnotationRetention.SOURCE)
public expect annotation class JvmSynthetic()

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.BINARY)
public expect annotation class JvmField()

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY, AnnotationTarget.TYPE)
@Retention(AnnotationRetention.BINARY)
public expect annotation class JvmSuppressWildcards(val suppress: Boolean = true)

@Target(AnnotationTarget.TYPE)
@Retention(AnnotationRetention.BINARY)
public expect annotation class JvmWildcard()

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
public expect annotation class JvmInline()

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
public expect annotation class ImplicitlyActualizedByJvmDeclaration()

@Target(AnnotationTarget.FILE)
@Retention(AnnotationRetention.SOURCE)
internal expect annotation class JvmPackageName(val name: String)
