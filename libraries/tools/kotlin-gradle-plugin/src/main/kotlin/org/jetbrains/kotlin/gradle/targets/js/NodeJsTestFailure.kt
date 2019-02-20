package org.jetbrains.kotlin.gradle.targets.js

/**
 * Class to be shown in default Gradle tests console reporter.
 *
 * Example console output:
 * ```
 *  clientTest.CommonTest.test1 FAILED
 *     org.jetbrains.kotlin.gradle.targets.js.NodeJsTestFailure
 * ```
 */
class NodeJsTestFailure(message: String, val stackTrace: String?) : Throwable(message) {
    override fun fillInStackTrace(): Throwable = this
    override fun toString(): String = stackTrace ?: ""
}