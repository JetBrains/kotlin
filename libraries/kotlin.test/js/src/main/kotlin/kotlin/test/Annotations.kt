/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.test

/**
 * Marks a function as a test.
 */
@Target(AnnotationTarget.FUNCTION)
public actual annotation class Test

/**
 * Marks a test or a suite as ignored.
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
public actual annotation class Ignore

/**
 * Marks a function to be invoked before each test.
 */
@Target(AnnotationTarget.FUNCTION)
public actual annotation class BeforeTest

/**
 * Marks a function to be invoked after each test.
 */
@Target(AnnotationTarget.FUNCTION)
public actual annotation class AfterTest
