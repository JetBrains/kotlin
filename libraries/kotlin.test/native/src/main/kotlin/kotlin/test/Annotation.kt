/*
 * Copyright 2010-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin.test

/**
 * Marks a function as a test.
 *
 * Only class member functions could be annotated with [Test],
 * the annotated function should not accept any parameters and its return type has to be [Unit].
 *
 * Applying [Test] to top-level, extension, or object member functions, functions accepting any parameters
 * or functions with a return type other than [Unit]
 * may lead to compilation or runtime errors, and behavior may vary on different targets.
 *
 * ```kotlin
 * import kotlin.test.*
 *
 * public class ArithmeticsTest {
 *     @Test
 *     fun addition() {
 *         assertEquals(4, 2 + 2)
 *     }
 * }
 * ```
 */
@Target(AnnotationTarget.FUNCTION)
public actual annotation class Test

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
 * Marks a function to be invoked before each test.
 *
 *
 * Only class member functions should be annotated with [BeforeTest],
 * the annotated function should not accept any parameters and its return type has to be [Unit].
 *
 * Applying [BeforeTest] to top-level, extension, or object member functions, functions accepting any parameters
 * or functions with a return type other than [Unit]
 * may lead to compilation or runtime errors, and behavior may vary on different targets.
 *
 * ```kotlin
 * import kotlin.test.*
 * import kotlinx.coroutines.Dispatchers
 * import kotlinx.coroutines.test.*
 *
 * public class UiTest {
 *     @BeforeTest
 *     public fun setUp() {
 *        Dispatchers.setMain(StandardTestDispatcher)
 *     }
 *
 *     @AfterTest
 *     public fun tearDown() {
 *        Dispatchers.resetMain()
 *     }
 *
 *     @Test
 *     public fun uiTest() { /* ... */ }
 * }
 * ```
 */
@Target(AnnotationTarget.FUNCTION)
public actual annotation class BeforeTest

/**
 * Marks a function to be invoked after each test.
 *
 * Only class member functions should be annotated with [AfterTest],
 * the annotated function should not accept any parameters and its return type has to be [Unit].
 *
 * Applying [AfterTest] to top-level, extension, or object member functions, functions accepting any parameters
 * or functions with a return type other than [Unit]
 * may lead to compilation or runtime errors, and behavior may vary on different targets.
 *
 * ```kotlin
 * import kotlin.test.*
 * import kotlinx.coroutines.Dispatchers
 * import kotlinx.coroutines.test.*
 *
 * public class UiTest {
 *     @BeforeTest
 *     public fun setUp() {
 *        Dispatchers.setMain(StandardTestDispatcher)
 *     }
 *
 *     @AfterTest
 *     public fun tearDown() {
 *        Dispatchers.resetMain()
 *     }
 *
 *     @Test
 *     public fun uiTest() { /* ... */ }
 * }
 * ```
 */
@Target(AnnotationTarget.FUNCTION)
public actual annotation class AfterTest

/**
 * Marks a test or a suite as ignored.
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
public actual annotation class Ignore
