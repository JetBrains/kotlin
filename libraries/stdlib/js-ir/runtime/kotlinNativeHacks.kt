/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.native.concurrent

// Current serialization removes expect declarations, so some dummy annotations are needed
// Expect declarations: libraries/stdlib/common/src/kotlin/NativeAnnotationsH.kt

@Target(AnnotationTarget.PROPERTY, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
@OptionalExpectation
public annotation class ThreadLocal()

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.BINARY)
@OptionalExpectation
public annotation class SharedImmutable()
