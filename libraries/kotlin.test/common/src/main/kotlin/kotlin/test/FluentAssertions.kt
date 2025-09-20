/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.test

/**
 * Asserts that this value is equal to the [expected] value.
 *
 * @param expected the expected value
 * @throws AssertionError if this value is not equal to the expected value
 */
@SinceKotlin("2.2.30")
public infix fun Any.shouldBe(expected: Any) {
    assertEquals(expected, this)
}

/**
 * Asserts that this value is not equal to the [expected] value.
 *
 * @param expected the value that this should not be equal to
 * @throws AssertionError if this value is equal to the expected value
 */
@SinceKotlin("2.2")
public infix fun Any.shouldNotBe(expected: Any) {
    assertNotEquals(expected, this)
}

@SinceKotlin("2.2")
@Deprecated(
    message = "Use assertEquals(...) for Doubles with absolute tolerance",
    replaceWith = ReplaceWith("assertEquals")
)
public infix fun Double.shouldBe(expected: Double) {
    throw UnsupportedOperationException("Use assertEquals(...) for Doubles with absolute tolerance")
}

@SinceKotlin("2.2")
@Deprecated(
    message = "Use assertNotEquals(...) for Doubles with absolute tolerance",
    replaceWith = ReplaceWith("assertNotEquals")
)
public infix fun Double.shouldNotBe(expected: Double) {
    throw UnsupportedOperationException("Use assertNotEquals(...) for Doubles with absolute tolerance")
}

@SinceKotlin("2.2")
@Deprecated(
    message = "Use assertEquals(...) for Float with absolute tolerance",
    replaceWith = ReplaceWith("assertEquals")
)
public infix fun Float.shouldBe(expected: Float) {
    throw UnsupportedOperationException("Use assertNotEquals(...) for Doubles with absolute tolerance")
}

@SinceKotlin("2.2")
@Deprecated(
    message = "Use assertNotEquals(...) for Float with absolute tolerance",
    replaceWith = ReplaceWith("assertNotEquals")
)
public infix fun Float.shouldNotBe(expected: Float) {
    throw UnsupportedOperationException("Use assertNotEquals(...) for Float with absolute tolerance")
}

/**
 * Asserts that this value is the same instance as the [expected] value.
 *
 * @param expected the expected instance
 * @throws AssertionError if this value is not the same instance as the expected value
 */
@SinceKotlin("2.2")
public infix fun Any.shouldBeSameAs(expected: Any) {
    assertSame(expected, this)
}

/**
 * Asserts that this value is not the same instance as the [expected] value.
 *
 * @param expected the value that this should not be the same instance as
 * @throws AssertionError if this value is the same instance as the expected value
 */
@SinceKotlin("2.2")
public infix fun Any.shouldNotBeSameAs(expected: Any) {
    assertNotSame(expected, this)
}

/**
 * Asserts that this sequence contains the [expected] element.
 *
 * @param expected the element that should be contained in this sequence
 * @throws AssertionError if this sequence does not contain the expected element
 */
@SinceKotlin("2.2")
public infix fun <T> Sequence<T>.shouldContain(expected: T?) {
    assertContains(this, expected)
}

/**
 * Asserts that this iterable contains the [expected] element.
 *
 * @param expected the element that should be contained in this iterable
 * @throws AssertionError if this iterable does not contain the expected element
 */
@SinceKotlin("2.2")
public infix fun <T> Iterable<T>.shouldContain(expected: T?) {
    assertContains(this, expected)
}

/**
 * Asserts that this character sequence contains the [expected] subsequence.
 *
 * @param expected the subsequence that should be contained in this character sequence
 * @throws AssertionError if this character sequence does not contain the expected subsequence
 */
@SinceKotlin("2.2")
public infix fun CharSequence.shouldContain(expected: CharSequence) {
    assertContains(this, expected)
}

/**
 * Executes the given [block] and adds a custom message to any thrown [AssertionError].
 * The message is computed lazily by [lazyMessage] only if an error occurs.
 *
 * @param lazyMessage a lambda that returns a custom message to be prepended to assertion failures, or null
 * @param block the code block to execute
 * @throws AssertionError if the block throws an AssertionError, with the custom message prepended
 */
@SinceKotlin("2.2")
public fun withClue(lazyMessage: () -> String? = { null }, block: () -> Any) {
    try {
        block.invoke()
    } catch (e: Throwable) {
        val message = lazyMessage()
        if (message != null) {
            throw AssertionError("$message. ${e.message}", e)
        } else if (e is AssertionError) {
            throw e
        } else {
            throw AssertionError(e)
        }
    }
}

/**
 * Executes the given [block] and adds a custom [message] to any thrown [AssertionError].
 *
 * @param message a custom message to be prepended to assertion failures, or null
 * @param block the code block to execute
 * @throws AssertionError if the block throws an AssertionError, with the custom message prepended
 */
@SinceKotlin("2.2")
public fun withClue(message: String? = null, block: () -> Unit) {
    withClue(lazyMessage = { message }, block = block)
}

/**
 * Asserts that this character sequence starts with the [expected] subsequence.
 *
 * @param expected the subsequence that this character sequence should start with
 * @throws AssertionError if this character sequence does not start with the expected subsequence
 */
@SinceKotlin("2.2")
public infix fun CharSequence.shouldStartWith(expected: CharSequence) {
    assertTrue(this.startsWith(expected), "Expected <$this> to start with <$expected>.")
}

/**
 * Asserts that this character sequence ends with the [expected] subsequence.
 *
 * @param expected the subsequence that this character sequence should end with
 * @throws AssertionError if this character sequence does not end with the expected subsequence
 */
@SinceKotlin("2.2")
public infix fun CharSequence.shouldEndWith(expected: CharSequence) {
    assertTrue(this.endsWith(expected), "Expected <$this> to end with <$expected>.")
}


/**
 * Asserts that this iterable starts with the [expected] element.
 *
 * @param expected the element that this iterable should start with
 * @throws AssertionError if this iterable does not start with the expected element
 */
@Suppress("ReplaceAssertBooleanWithAssertEquality")
@SinceKotlin("2.2")
public infix fun <T> Iterable<T>.shouldStartWith(expected: T) {
    assertTrue(expected == this.firstOrNull(), "Expected <$this> to start with <$expected>.")
}

/**
 * Asserts that this iterable starts with the [expected] elements in the same order.
 *
 * @param expected the elements that this iterable should start with
 * @throws AssertionError if this iterable does not start with the expected elements
 */
@SinceKotlin("2.2")
public fun <T> Iterable<T>.shouldStartWith(vararg expected: T) {
    val thisList = this.toList()
    if (thisList.size < expected.size) {
        fail("Expected <$this> to start with <${expected.contentToString()}>, but actual size ${thisList.size} is less than expected size ${expected.size}.")
    }
    for (i in expected.indices) {
        if (thisList[i] != expected[i]) {
            fail("Expected <$this> to start with <${expected.contentToString()}>, but differs at index $i: expected <${expected[i]}>, actual <${thisList[i]}>.")
        }
    }
}

/**
 * Asserts that this iterable ends with the [expected] elements in the same order.
 *
 * @param expected the elements that this iterable should end with
 * @throws AssertionError if this iterable does not end with the expected elements
 */
@SinceKotlin("2.2")
public fun <T> Iterable<T>.shouldEndWith(vararg expected: T) {
    val thisList = this.toList()
    if (thisList.size < expected.size) {
        fail("Expected <$this> to end with <${expected.contentToString()}>, but actual size ${thisList.size} is less than expected size ${expected.size}.")
    }
    val offset = thisList.size - expected.size
    for (i in expected.indices) {
        if (thisList[offset + i] != expected[i]) {
            fail("Expected <$this> to end with <${expected.contentToString()}>, but differs at index ${offset + i}: expected <${expected[i]}>, actual <${thisList[offset + i]}>.")
        }
    }
}

/**
 * Asserts that this iterable ends with the [expected] element.
 *
 * @param expected the element that this iterable should end with
 * @throws AssertionError if this iterable does not end with the expected element
 */
@Suppress("ReplaceAssertBooleanWithAssertEquality")
@SinceKotlin("2.2")
public infix fun <T> Iterable<T>.shouldEndWith(expected: T) {
    assertTrue(expected == this.lastOrNull(), "Expected <$this> to end with <$expected>.")
}

public fun main() {

    "Hello" shouldBe "Hello"
    println("✅ 1")

    withClue({ "Should be equal" }) {
        "Hello" shouldBe "Hello"
    }
    println("✅ 2")

    val err = assertFailsWith<AssertionError> {
        withClue("Should fail") {
            "Hello" shouldBe "Goodbye"
        }
    }
    assertEquals("Should fail. Expected <Goodbye>, actual <Hello>.", err.message)
    println("✅ 3, ${err.message}")

    val error = assertFailsWith<AssertionError> {
        withClue({ "Should fail" }) {
            "Hello" shouldBe "Goodbye"
        }
    }
    assertEquals("Should fail. Expected <Goodbye>, actual <Hello>.", error.message)
    println("✅ 4")

    assertFailsWith<AssertionError>("shouldBe should fail when expected") {
        "Hello" shouldBe "Goodbye"
    }

    println("✅ 5")

    "Hello" shouldNotBe 1
    println("✅ 6")
}
