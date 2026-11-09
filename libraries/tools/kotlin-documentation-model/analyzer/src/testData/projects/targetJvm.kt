// JVM-specific declarations: JVM annotations, Java stdlib types, and checked exceptions.

// MODULE: jvmMain
// PLATFORM: JVM
// FILE: pkg/Jvm.kt
@file:JvmName("JvmUtils")

package pkg

import java.io.File
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger

class JvmService @JvmOverloads constructor(val name: String, val retries: Int = 3) {
    @JvmField
    val counter: AtomicInteger = AtomicInteger()

    @Volatile
    var state: Int = 0

    @Synchronized
    @Throws(IOException::class)
    fun read(file: File): ByteArray = file.readBytes()

    companion object {
        @JvmStatic
        fun create(): JvmService = JvmService("default")
    }
}

@JvmInline
value class JvmId(val raw: Long)

fun javaClassName(value: Any): String = value.javaClass.name
