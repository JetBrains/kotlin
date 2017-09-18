package kotlin.test

/**
 * Marks a function as a test.
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.FUNCTION)
annotation class Test

/**
 * Marks a function to be executed before a suite. Not supported in Kotlin/Common.
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.FUNCTION)
annotation class BeforeClass

/**
 * Marks a function to be executed after a suite. Not supported in Kotlin/Common.
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.FUNCTION)
annotation class AfterClass

/**
 * Marks a function to be executed before a test.
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.FUNCTION)
annotation class BeforeEach


/**
 * Marks a function to be executed after a test.
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.FUNCTION)
annotation class AfterEach

/**
 * Marks a test or a suite as ignored/pending.
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class Ignore
