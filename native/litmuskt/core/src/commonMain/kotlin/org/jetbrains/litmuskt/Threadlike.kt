package org.jetbrains.litmuskt

/**
 * A "thread-like" is a wrapper for something that can be used as a thread, for example, Worker or pthread API.
 * For now, returning a value from "threads" is not supported (as it is not currently needed).
 */
interface Threadlike {
    /**
     * Start running the function in a "thread".
     *
     * Notes:
     * 1. This function should be only called once.
     * 1. [function] should be non-capturing.
     * 1. Since returning a value is not currently supported, the resulting future returns a stub (Unit).
     *
     * @return a "future" handle that will block when called until the "thread" has completed.
     */
    fun <A : Any> start(args: A, function: (A) -> Unit): BlockingFuture<Unit>

    /**
     * Dispose of any resources the "thread" has allocated. Blocks until the resources are cleaned.
     */
    fun dispose()
}
