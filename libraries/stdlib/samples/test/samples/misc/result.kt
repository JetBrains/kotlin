package samples.misc

import samples.*
import kotlin.test.*

class Result {

    @Sample
    fun runCatching() {
        // Failure
        val resultLong = runCatching { "N".toLong() }
        assertTrue(resultLong.isFailure)

        // Success
        val resultChar = runCatching { "11".first() }
        assertTrue(resultChar.isSuccess)
    }

    @Sample
    fun runCatchingWithReceiver() {
        // Failure
        val resultLong = "N".runCatching { toLong() }
        assertTrue(resultLong.isFailure)

        // Success
        val resultChar = "11".runCatching { first() }
        assertTrue(resultChar.isSuccess)
    }

    @Sample
    fun getOrThrow() {
        // Failure
        val resultLong = runCatching { "N".toLong() }
        assertFailsWith<NumberFormatException> { resultLong.getOrThrow() }

        // Success
        val resultChar = runCatching { "11".first() }
        assertPrints(resultChar.getOrThrow(), "1")
    }

    @Sample
    fun getOrElse() {
        // Failure
        val resultLong = runCatching { "N".toLong() }
        val resultLongOrZero = resultLong.getOrElse { exception ->
            println(exception.message)
            0
        }

        assertPrints(resultLongOrZero, "0")

        // IllegalArgumentException thrown by onFailure function is rethrown
        val resultInt = runCatching { "N".toInt() }
        assertFailsWith<IllegalArgumentException> {
            resultInt.getOrElse { throw IllegalArgumentException() }
        }

        // Success
        val resultChar = runCatching { "11".first() }
        assertPrints(resultChar.getOrElse { throw IllegalArgumentException() }, "1")
    }

    @Sample
    fun getOrDefault() {
        // Failure
        val resultLong = runCatching { "N".toLong() }
        assertPrints(resultLong.getOrDefault(0), "0")

        // Success
        val resultChar = runCatching { "11".first() }
        assertPrints(resultChar.getOrDefault('0'), "1")
    }

    @Sample
    fun fold() {
        // Failure
        val resultLong = runCatching { "N".toLong() }
        val foldLong = resultLong.fold(
            { value ->
                println(value)
                value
            },
            { exception ->
                println(exception.message)
                0
            }
        )

        assertPrints(foldLong, 0)

        // Success
        val resultChar = runCatching { "11".first() }
        val foldChar = resultChar.fold(
            { value ->
                assertPrints(value, "1")
                value
            },
            { exception ->
                println(exception)
                '0'
            }
        )

        assertPrints(foldChar, "1")
    }

    @Sample
    fun map() {
        // Failure
        val resultLong = runCatching { "N".toLong() }
        val mapLong = resultLong.map { it.inc() }
        assertPrints(mapLong.getOrNull(), "null")

        // Success
        val resultChar = runCatching { "11".first() }
        val mapChar = resultChar.map { it.inc() }
        assertPrints(mapChar.getOrNull(), "2")

        // IllegalArgumentException thrown by transform function is rethrown
        val resultThrowable = runCatching { "11".first() }
        assertFailsWith<IllegalArgumentException> {
            resultThrowable.map { throw IllegalArgumentException() }
        }
    }

    @Sample
    fun mapCatching() {
        // Failure
        val resultLong = runCatching { "N".toLong() }
        val mapLong = resultLong.mapCatching { it.inc() }
        assertPrints(mapLong.getOrNull(), "null")

        // Success
        val resultChar = runCatching { "11".first() }
        val mapChar = resultChar.mapCatching { it.inc() }
        assertPrints(mapChar.getOrNull(), "2")

        // IllegalArgumentException thrown by transform function is catched
        val resultThrowable = runCatching { "11".first() }
        val mapThrowable = resultThrowable.mapCatching {
            throw IllegalArgumentException()
        }

        assertPrints(mapThrowable.getOrNull(), "null")
    }

    @Sample
    fun recover() {
        // Failure
        val resultLong = runCatching { "N".toLong() }
        val recoverLong = resultLong.recover { exception ->
            println(exception.message)
            0
        }

        assertPrints(recoverLong.getOrNull(), "0")

        // Success
        val resultChar = runCatching { "11".first() }
        val recoverChar = resultChar.recover { exception ->
            println(exception.message)
            '0'
        }

        assertPrints(recoverChar.getOrNull(), "1")

        // IllegalArgumentException thrown by transform function is rethrown
        val resultThrowable = runCatching { "N".toLong() }
        assertFailsWith<IllegalArgumentException> {
            resultThrowable.recover { throw IllegalArgumentException() }
        }
    }

    @Sample
    fun recoverCatching() {
        // Failure
        val resultLong = runCatching { "N".toLong() }
        val recoverLong = resultLong.recoverCatching { exception ->
            println(exception.message)
            0
        }

        assertPrints(recoverLong.getOrNull(), "0")

        // Success
        val resultChar = runCatching { "11".first() }
        val recoverChar = resultChar.recoverCatching { exception ->
            println(exception.message)
            '0'
        }

        assertPrints(recoverChar.getOrNull(), "1")

        // IllegalArgumentException thrown by transform function is catched
        val resultThrowable = runCatching { "N".toLong() }
        val recoverThrowable = resultThrowable.recoverCatching {
            throw IllegalArgumentException()
        }

        assertTrue(resultThrowable.isFailure)
    }

    @Sample
    fun onFailure() {
        // Failure
        val resultLong = runCatching { "N".toLong() }
        val originalResultLong = resultLong.onFailure {
            assertPrints(it, "java.lang.NumberFormatException: For input string: \"N\"")
        }

        assertTrue(originalResultLong.isFailure)

        // Success
        val resultChar = runCatching { "11".first() }
        val originalResultChar = resultChar.onFailure {
            println(it.message)
        }

        assertTrue(originalResultChar.isSuccess)
    }

    @Sample
    fun onSuccess() {
        // Failure
        val resultLong = runCatching { "N".toLong() }
        val originalResultLong = resultLong.onSuccess {
            println(it.inc())
        }

        assertTrue(originalResultLong.isFailure)

        // Success
        val resultChar = runCatching { "11".first() }
        val originalResultChar = resultChar.onSuccess {
            assertPrints(it.inc(), "2")
        }

        assertTrue(originalResultChar.isSuccess)
    }
}
