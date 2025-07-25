/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("UNCHECKED_CAST", "RedundantVisibilityModifier")

package kotlin

import kotlin.contracts.*
import kotlin.internal.InlineOnly
import kotlin.jvm.JvmField
import kotlin.jvm.JvmInline
import kotlin.jvm.JvmName

/**
 * A discriminated union that encapsulates a successful outcome with a value of type [T]
 * or a failure with an arbitrary [Throwable] exception.
 */
@SinceKotlin("1.3")
@JvmInline
public value class Result<out T> @PublishedApi internal constructor(
    @PublishedApi
    internal val value: Any?
) : Serializable {
    // discovery

    /**
     * Returns `true` if this instance represents a successful outcome.
     * In this case [isFailure] returns `false`.
     */
    public val isSuccess: Boolean get() = value !is Failure

    /**
     * Returns `true` if this instance represents a failed outcome.
     * In this case [isSuccess] returns `false`.
     */
    public val isFailure: Boolean get() = value is Failure

    // value & exception retrieval

    /**
     * Returns the encapsulated value if this instance represents [success][Result.isSuccess] or `null`
     * if it is [failure][Result.isFailure].
     *
     * This function is a shorthand for `getOrElse { null }` (see [getOrElse]) or
     * `fold(onSuccess = { it }, onFailure = { null })` (see [fold]).
     */
    @InlineOnly
    public inline fun getOrNull(): T? =
        when {
            isFailure -> null
            else -> value as T
        }

    /**
     * Returns the encapsulated [Throwable] exception if this instance represents [failure][isFailure] or `null`
     * if it is [success][isSuccess].
     *
     * This function is a shorthand for `fold(onSuccess = { null }, onFailure = { it })` (see [fold]).
     */
    public fun exceptionOrNull(): Throwable? =
        when (value) {
            is Failure -> value.exception
            else -> null
        }

    /**
     * Returns a string `Success(v)` if this instance represents [success][Result.isSuccess]
     * where `v` is a string representation of the value or a string `Failure(x)` if
     * it is [failure][isFailure] where `x` is a string representation of the exception.
     */
    public override fun toString(): String =
        when (value) {
            is Failure -> value.toString() // "Failure($exception)"
            else -> "Success($value)"
        }

    // companion with constructors

    /**
     * Companion object for [Result] class that contains its constructor functions
     * [success] and [failure].
     */
    public companion object {
        /**
         * Returns an instance that encapsulates the given [value] as successful value.
         */
        @Suppress("INAPPLICABLE_JVM_NAME")
        @InlineOnly
        @JvmName("success")
        public inline fun <T> success(value: T): Result<T> =
            Result(value)

        /**
         * Returns an instance that encapsulates the given [Throwable] [exception] as failure.
         */
        @Suppress("INAPPLICABLE_JVM_NAME")
        @InlineOnly
        @JvmName("failure")
        public inline fun <T> failure(exception: Throwable): Result<T> =
            Result(createFailure(exception))
    }

    internal class Failure(
        @JvmField
        val exception: Throwable
    ) : Serializable {
        override fun equals(other: Any?): Boolean = other is Failure && exception == other.exception
        override fun hashCode(): Int = exception.hashCode()
        override fun toString(): String = "Failure($exception)"
    }
}

/**
 * Creates an instance of internal marker [Result.Failure] class to
 * make sure that this class is not exposed in ABI.
 */
@PublishedApi
@SinceKotlin("1.3")
internal fun createFailure(exception: Throwable): Any =
    Result.Failure(exception)

/**
 * Throws exception if the result is failure. This internal function minimizes
 * inlined bytecode for [getOrThrow] and makes sure that in the future we can
 * add some exception-augmenting logic here (if needed).
 */
@PublishedApi
@SinceKotlin("1.3")
internal fun Result<*>.throwOnFailure() {
    if (value is Result.Failure) throw value.exception
}

/**
 * Calls the specified function [block] and returns its encapsulated result if invocation was successful,
 * catching any [Throwable] exception that was thrown from the [block] function execution and encapsulating it as a failure.
 *
 * Example:
 * ```
 * // Basic usage to handle exceptions
 * val result = runCatching {
 *     // Code that might throw an exception
 *     fetchData()
 * }
 * 
 * // Process the result
 * when {
 *     result.isSuccess -> println("Data: ${result.getOrNull()}")
 *     result.isFailure -> println("Error: ${result.exceptionOrNull()?.message}")
 * }
 *
 * // Combine with other Result functions for elegant error handling
 * val processedData = runCatching { fetchData() }
 *     .map { data -> processData(data) }
 *     .recover { exception -> 
 *         when (exception) {
 *             is IOException -> "No data available (IO Error)"
 *             is SecurityException -> "Access denied"
 *             else -> "Unknown error: ${exception.message}"
 *         }
 *     }
 *
 * // Use in functions that need to handle exceptions
 * fun safeOperation(): Result<Data> = runCatching {
 *     val resource = acquireResource()
 *     try {
 *         processResource(resource)
 *     } finally {
 *         resource.close() // Ensures resource is closed even if an exception occurs
 *     }
 * }
 * ```
 */
@InlineOnly
@SinceKotlin("1.3")
public inline fun <R> runCatching(block: () -> R): Result<R> {
    return try {
        Result.success(block())
    } catch (e: Throwable) {
        Result.failure(e)
    }
}

/**
 * Calls the specified function [block] with `this` value as its receiver and returns its encapsulated result if invocation was successful,
 * catching any [Throwable] exception that was thrown from the [block] function execution and encapsulating it as a failure.
 *
 * Example:
 * ```
 * // Using runCatching with a receiver to safely call methods on an object
 * val user: User? = getUser()
 * val nameResult = user.runCatching { 
 *     // 'this' is the receiver (User)
 *     requireNotNull(name) // Throws if name is null
 * }
 * 
 * // Safe operations on nullable objects
 * val file: File? = getFile()
 * val contents = file.runCatching { 
 *     // Only executed if file is not null
 *     readText() // Might throw IOException
 * }.getOrDefault("No content available")
 *
 * // Safely chain multiple operations on an object
 * val response: Response = api.getResponse()
 * val parsedData = response.runCatching {
 *     // These operations are performed on the response object
 *     check(isSuccessful) { "API call failed with status $statusCode" }
 *     val body = requireNotNull(body) { "Response body was null" }
 *     jsonParser.parse(body)
 * }.getOrElse { exception ->
 *     logger.error("Failed to parse response", exception)
 *     JsonObject()  // Return empty object on failure
 * }
 *
 * // Extension function for safe operations on collections
 * fun <T, R> List<T>.safeTransform(transform: (List<T>) -> R): Result<R> = 
 *     runCatching { transform(this) }
 * ```
 */
@InlineOnly
@SinceKotlin("1.3")
public inline fun <T, R> T.runCatching(block: T.() -> R): Result<R> {
    return try {
        Result.success(block())
    } catch (e: Throwable) {
        Result.failure(e)
    }
}

// -- extensions ---

/**
 * Returns the encapsulated value if this instance represents [success][Result.isSuccess] or throws the encapsulated [Throwable] exception
 * if it is [failure][Result.isFailure].
 *
 * This function is a shorthand for `getOrElse { throw it }` (see [getOrElse]).
 *
 * Example:
 * ```
 * // When you want to get the value or propagate the exception
 * fun processUserData(): UserData {
 *     val result = runCatching { fetchUserData() }
 *     // If fetchUserData() succeeded, return the user data
 *     // If it failed, the exception is thrown from getOrThrow()
 *     return result.getOrThrow()
 * }
 *
 * // In a try-catch block to handle specific exceptions
 * fun getUserDataSafely(): UserData {
 *     val result = runCatching { fetchUserData() }
 *     try {
 *         return result.getOrThrow()
 *     } catch (e: NetworkException) {
 *         log.error("Network error while fetching user data", e)
 *         return UserData.createDefault()
 *     } catch (e: Exception) {
 *         log.error("Unknown error while fetching user data", e)
 *         throw e // rethrow other exceptions
 *     }
 * }
 * ```
 */
@InlineOnly
@SinceKotlin("1.3")
public inline fun <T> Result<T>.getOrThrow(): T {
    throwOnFailure()
    return value as T
}

/**
 * Returns the encapsulated value if this instance represents [success][Result.isSuccess] or the
 * result of [onFailure] function for the encapsulated [Throwable] exception if it is [failure][Result.isFailure].
 *
 * Note, that this function rethrows any [Throwable] exception thrown by [onFailure] function.
 *
 * This function is a shorthand for `fold(onSuccess = { it }, onFailure = onFailure)` (see [fold]).
 *
 * Example:
 * ```
 * // Provide a computed fallback value based on the exception
 * fun getUserName(): String {
 *     val result = runCatching { fetchUser() }
 *     return result.getOrElse { exception ->
 *         when (exception) {
 *             is UserNotFoundException -> "Guest"
 *             is NetworkException -> "Offline User"
 *             else -> "Unknown User"
 *         }
 *     }
 * }
 *
 * // Log the exception and provide a default value
 * fun getConfig(): Config {
 *     val result = runCatching { loadConfig() }
 *     return result.getOrElse { exception ->
 *         logger.error("Failed to load config", exception)
 *         Config.createDefault()
 *     }
 * }
 *
 * // The lambda can also throw exceptions if needed
 * fun getCriticalData(): Data {
 *     val result = runCatching { fetchData() }
 *     return result.getOrElse { exception ->
 *         if (exception is AuthException) {
 *             // Handle auth errors by redirecting to login
 *             redirectToLogin()
 *             throw exception // Rethrow to stop further processing
 *         } else {
 *             // For other errors, use a cached version
 *             loadFromCache() ?: throw exception
 *         }
 *     }
 * }
 * ```
 */
@InlineOnly
@SinceKotlin("1.3")
public inline fun <R, T : R> Result<T>.getOrElse(onFailure: (exception: Throwable) -> R): R {
    contract {
        callsInPlace(onFailure, InvocationKind.AT_MOST_ONCE)
    }
    return when (val exception = exceptionOrNull()) {
        null -> value as T
        else -> onFailure(exception)
    }
}

/**
 * Returns the encapsulated value if this instance represents [success][Result.isSuccess] or the
 * [defaultValue] if it is [failure][Result.isFailure].
 *
 * This function is a shorthand for `getOrElse { defaultValue }` (see [getOrElse]).
 *
 * Example:
 * ```
 * // Provide a simple default value when the operation fails
 * fun getUserCount(): Int {
 *     val result = runCatching { fetchUserCount() }
 *     // If fetchUserCount() fails, default to 0
 *     return result.getOrDefault(0)
 * }
 *
 * // Use with nullable types
 * fun getUsername(): String {
 *     val result = runCatching { user?.name }
 *     // If user is null or name is null, use "Guest" as default
 *     return result.getOrDefault("Guest")
 * }
 *
 * // Use in a chain of operations
 * val displayName = runCatching { fetchUser() }
 *     .map { user -> user.displayName }
 *     .getOrDefault("Unknown User")
 * ```
 */
@InlineOnly
@SinceKotlin("1.3")
public inline fun <R, T : R> Result<T>.getOrDefault(defaultValue: R): R {
    if (isFailure) return defaultValue
    return value as T
}

/**
 * Returns the result of [onSuccess] for the encapsulated value if this instance represents [success][Result.isSuccess]
 * or the result of [onFailure] function for the encapsulated [Throwable] exception if it is [failure][Result.isFailure].
 *
 * Note, that this function rethrows any [Throwable] exception thrown by [onSuccess] or by [onFailure] function.
 *
 * Example:
 * ```
 * // Use fold to handle both success and failure cases and map to a common return type
 * val result: Result<Int> = runCatching { fetchUserCount() }
 * 
 * val message = result.fold(
 *     onSuccess = { count -> "Successfully fetched $count users" },
 *     onFailure = { exception -> "Failed to fetch users: ${exception.message}" }
 * )
 * println(message) // Either "Successfully fetched X users" or "Failed to fetch users: ..."
 *
 * // Fold can be used to convert a Result to any other type
 * val userCount = runCatching { fetchUserCount() }
 *     .fold(
 *         onSuccess = { it }, // return the count directly on success
 *         onFailure = { 0 }   // default to 0 on failure
 *     )
 * 
 * // Fold can also be used for more complex transformations
 * val apiResult = runCatching { api.fetchData() }
 * val uiModel = apiResult.fold(
 *     onSuccess = { data -> UiModel.Content(data) },
 *     onFailure = { error -> 
 *         when (error) {
 *             is NetworkException -> UiModel.Error("Network error. Check your connection.")
 *             is AuthException -> UiModel.Error("Authentication failed. Please log in again.")
 *             else -> UiModel.Error("Unknown error occurred.")
 *         }
 *     }
 * )
 * ```
 */
@InlineOnly
@SinceKotlin("1.3")
public inline fun <R, T> Result<T>.fold(
    onSuccess: (value: T) -> R,
    onFailure: (exception: Throwable) -> R
): R {
    contract {
        callsInPlace(onSuccess, InvocationKind.AT_MOST_ONCE)
        callsInPlace(onFailure, InvocationKind.AT_MOST_ONCE)
    }
    return when (val exception = exceptionOrNull()) {
        null -> onSuccess(value as T)
        else -> onFailure(exception)
    }
}

// transformation

/**
 * Returns the encapsulated result of the given [transform] function applied to the encapsulated value
 * if this instance represents [success][Result.isSuccess] or the
 * original encapsulated [Throwable] exception if it is [failure][Result.isFailure].
 *
 * Note, that this function rethrows any [Throwable] exception thrown by [transform] function.
 * See [mapCatching] for an alternative that encapsulates exceptions.
 *
 * Example:
 * ```
 * // Transforms a successful Result containing a number to a Result containing the number doubled
 * val result: Result<Int> = Result.success(10)
 * val doubled: Result<Int> = result.map { it * 2 }
 * println(doubled) // Success(20)
 *
 * // If the original Result is a failure, the transformation is not applied
 * val failed: Result<Int> = Result.failure(ArithmeticException("Division by zero"))
 * val failedMapped: Result<Int> = failed.map { it * 2 }
 * println(failedMapped) // Failure(ArithmeticException: Division by zero)
 * ```
 */
@InlineOnly
@SinceKotlin("1.3")
public inline fun <R, T> Result<T>.map(transform: (value: T) -> R): Result<R> {
    contract {
        callsInPlace(transform, InvocationKind.AT_MOST_ONCE)
    }
    return when {
        isSuccess -> Result.success(transform(value as T))
        else -> Result(value)
    }
}

/**
 * Returns the encapsulated result of the given [transform] function applied to the encapsulated value
 * if this instance represents [success][Result.isSuccess] or the
 * original encapsulated [Throwable] exception if it is [failure][Result.isFailure].
 *
 * This function catches any [Throwable] exception thrown by [transform] function and encapsulates it as a failure.
 * See [map] for an alternative that rethrows exceptions from `transform` function.
 *
 * Example:
 * ```
 * // Safely transforms a successful Result, catching any exceptions that might occur during transformation
 * val result: Result<Int> = Result.success(10)
 * val safelyDivided: Result<Double> = result.mapCatching { 100.0 / it }
 * println(safelyDivided) // Success(10.0)
 *
 * // If the transformation throws an exception, it's caught and encapsulated as a failure
 * val zero: Result<Int> = Result.success(0)
 * val divideByZero: Result<Double> = zero.mapCatching { 100.0 / it }
 * println(divideByZero) // Failure(ArithmeticException: Division by zero)
 *
 * // If the original Result is a failure, the transformation is not applied
 * val failed: Result<Int> = Result.failure(IllegalArgumentException("Invalid input"))
 * val failedTransformed: Result<Double> = failed.mapCatching { 100.0 / it }
 * println(failedTransformed) // Failure(IllegalArgumentException: Invalid input)
 * ```
 */
@InlineOnly
@SinceKotlin("1.3")
public inline fun <R, T> Result<T>.mapCatching(transform: (value: T) -> R): Result<R> {
    return when {
        isSuccess -> runCatching { transform(value as T) }
        else -> Result(value)
    }
}

/**
 * Returns the encapsulated result of the given [transform] function applied to the encapsulated [Throwable] exception
 * if this instance represents [failure][Result.isFailure] or the
 * original encapsulated value if it is [success][Result.isSuccess].
 *
 * Note, that this function rethrows any [Throwable] exception thrown by [transform] function.
 * See [recoverCatching] for an alternative that encapsulates exceptions.
 *
 * Example:
 * ```
 * // Recover from a specific exception by providing a default value
 * fun fetchData(): Result<String> = runCatching { 
 *     throw NetworkException("No internet connection")
 *     "Data" 
 * }
 * 
 * val result = fetchData()
 *     .recover { exception ->
 *         when (exception) {
 *             is NetworkException -> "Cached data"
 *             else -> throw exception // rethrow any other exceptions
 *         }
 *     }
 * println(result) // Success(Cached data)
 *
 * // If the original Result is a success, the recovery function is not applied
 * val success: Result<String> = Result.success("Original data")
 * val unchanged = success.recover { "Recovery value" }
 * println(unchanged) // Success(Original data)
 * ```
 */
@InlineOnly
@SinceKotlin("1.3")
public inline fun <R, T : R> Result<T>.recover(transform: (exception: Throwable) -> R): Result<R> {
    contract {
        callsInPlace(transform, InvocationKind.AT_MOST_ONCE)
    }
    return when (val exception = exceptionOrNull()) {
        null -> this
        else -> Result.success(transform(exception))
    }
}

/**
 * Returns the encapsulated result of the given [transform] function applied to the encapsulated [Throwable] exception
 * if this instance represents [failure][Result.isFailure] or the
 * original encapsulated value if it is [success][Result.isSuccess].
 *
 * This function catches any [Throwable] exception thrown by [transform] function and encapsulates it as a failure.
 * See [recover] for an alternative that rethrows exceptions.
 *
 * Example:
 * ```
 * // Safely recover from a failure, catching any exceptions that might occur during recovery
 * fun fetchData(): Result<String> = runCatching { 
 *     throw NetworkException("No internet connection")
 *     "Data" 
 * }
 * 
 * val result = fetchData()
 *     .recoverCatching { exception ->
 *         // This recovery operation might also fail
 *         val backupData = readFromBackup() // could throw IOException
 *         "Backup data: $backupData"
 *     }
 * 
 * // If readFromBackup() throws an exception, it will be caught and encapsulated
 * // result will be a failure containing the new exception
 * 
 * // If the original Result is a success, the recovery function is not applied
 * val success: Result<String> = Result.success("Original data")
 * val unchanged = success.recoverCatching { "Recovery value" }
 * println(unchanged) // Success(Original data)
 * ```
 */
@InlineOnly
@SinceKotlin("1.3")
public inline fun <R, T : R> Result<T>.recoverCatching(transform: (exception: Throwable) -> R): Result<R> {
    return when (val exception = exceptionOrNull()) {
        null -> this
        else -> runCatching { transform(exception) }
    }
}

// "peek" onto value/exception and pipe

/**
 * Performs the given [action] on the encapsulated [Throwable] exception if this instance represents [failure][Result.isFailure].
 * Returns the original `Result` unchanged.
 *
 * Example:
 * ```
 * // Log errors while processing a chain of operations
 * val result = runCatching { fetchData() }
 *     .map { processData(it) }
 *     .onFailure { exception -> 
 *         logger.error("Operation failed", exception)
 *         metrics.incrementErrorCount()
 *     }
 *
 * // onFailure can be used in the middle of a chain
 * val processed = runCatching { fetchData() }
 *     .onFailure { println("Failed to fetch data: $it") }
 *     .map { processData(it) }
 *     .onFailure { println("Failed to process data: $it") }
 *
 * // If the Result is a success, the action is not performed
 * val success = Result.success("Data")
 * success.onFailure { println("This won't be printed") }
 * ```
 */
@InlineOnly
@SinceKotlin("1.3")
@IgnorableReturnValue
public inline fun <T> Result<T>.onFailure(action: (exception: Throwable) -> Unit): Result<T> {
    contract {
        callsInPlace(action, InvocationKind.AT_MOST_ONCE)
    }
    exceptionOrNull()?.let { action(it) }
    return this
}

/**
 * Performs the given [action] on the encapsulated value if this instance represents [success][Result.isSuccess].
 * Returns the original `Result` unchanged.
 *
 * Example:
 * ```
 * // Log or process successful results while chaining operations
 * val result = runCatching { fetchData() }
 *     .onSuccess { data -> 
 *         println("Data fetched successfully: $data")
 *         saveToCache(data)
 *     }
 *     .map { processData(it) }
 *     .onSuccess { processed ->
 *         println("Data processed successfully: $processed")
 *     }
 *
 * // onSuccess can be used for side effects without changing the result
 * val processed = runCatching { fetchData() }
 *     .onSuccess { metrics.incrementSuccessCount() }
 *     .map { processData(it) }
 *
 * // If the Result is a failure, the action is not performed
 * val failure = Result.failure<String>(Exception("Failed to fetch data"))
 * failure.onSuccess { println("This won't be printed") }
 * ```
 */
@InlineOnly
@SinceKotlin("1.3")
@IgnorableReturnValue
public inline fun <T> Result<T>.onSuccess(action: (value: T) -> Unit): Result<T> {
    contract {
        callsInPlace(action, InvocationKind.AT_MOST_ONCE)
    }
    if (isSuccess) action(value as T)
    return this
}

// -------------------
