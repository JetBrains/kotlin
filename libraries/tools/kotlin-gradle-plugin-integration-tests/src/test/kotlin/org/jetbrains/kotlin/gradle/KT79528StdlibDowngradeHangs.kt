package org.jetbrains.kotlin.gradle

import kotlin.test.Test
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.jetbrains.kotlin.util.assertThrows

class KT79528StdlibDowngradeHangs {

    @Test
    fun `test KT-79528 - test Gradle doesn't downgrade stdlib and cause uncaught exceptions in coroutines to hang coroutines machinery`() {
        class Foo : RuntimeException()
        assertThrows<Foo> {
            runBlocking {
                async {
                    throw Foo()
                }.await()
            }
        }
    }

}