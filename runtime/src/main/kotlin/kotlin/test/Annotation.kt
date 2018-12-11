/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin.test

/**
 * Marks a function as a test.
 */
@Target(AnnotationTarget.FUNCTION)
public annotation class Test

/**
 * Marks a function to be executed before a suite. Not supported in Kotlin/Common.
 */
@Target(AnnotationTarget.FUNCTION)
public annotation class BeforeClass

/**
 * Marks a function to be executed after a suite. Not supported in Kotlin/Common.
 */
@Target(AnnotationTarget.FUNCTION)
public annotation class AfterClass

/**
 * Marks a function to be executed before a test.
 */
@Target(AnnotationTarget.FUNCTION)
public annotation class BeforeEach


/**
 * Marks a function to be executed after a test.
 */
@Target(AnnotationTarget.FUNCTION)
public annotation class AfterEach

/**
 * Marks a test or a suite as ignored.
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
public annotation class Ignore

public typealias AfterTest = AfterEach
public typealias BeforeTest = BeforeEach
