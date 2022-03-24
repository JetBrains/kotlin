package org.jetbrains.kotlin.gradle.targets.js.internal

import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import com.google.gson.stream.MalformedJsonException
import org.slf4j.LoggerFactory
import java.io.*
import kotlin.math.min

open class RewriteSourceMapFilterReader(
    val input: Reader
) : FilterReader(input) {
    // This implementation works only when source map contents starts
    // with prolog `{"version":3,"file":"...","sources":[...],"sourcesContent":...`
    // This is always true for Kotlin/JS and many other tools that produces JS source maps.
    //
    // Full implementation (without loading file contents entirely into memory) requires
    // streaming read of string literals inside "mappings", which is not supported by any
    // known JSON parsers. We cannot store "mappings" into memory since it can reach tens
    // of megabytes for large projects.
    //
    // Implementation: read contents before [PROLOG_END], parse it as JSON and transform source paths.
    // All rest contents will be passed directly from [input].

    companion object {
        internal const val PROLOG_END = "],\"sourcesContent\":"
        const val UNSUPPORTED_FORMAT_MESSAGE =
            "Unsupported format. Contents should starts with `{\"version\":3,\"file\":\"...\",\"sources\":[...],\"sourcesContent\":...`"

        private val log = LoggerFactory.getLogger("kotlin")
    }

    private var wasReadFirst = false
    private val prologLimit = 0xfffff

    // buffer with transformed prolog, that wil be emitted first
    private lateinit var bufferWriter: StringWriter
    private lateinit var bufferJsonWriter: JsonWriter
    private val buffer: StringBuffer get() = bufferWriter.buffer
    private var bufferReadPos = 0
    private val bufferAvailable get() = buffer.length - bufferReadPos

    private var inputEof = false

    lateinit var srcSourceRoot: String
    lateinit var targetSourceRoot: String

    private fun maybeReadFirst() {
        if (!wasReadFirst) {
            wasReadFirst = true
            readFirst()
        }
    }

    private fun readFirst() {
        // read 1Kb chunks from [input] until [PROLOG_END] will be found
        val jsonString = StringBuilder()
        val readBuffer = CharArray(1024)
        var lastRead: Int
        var jsonPrologPos: Int
        while (true) {
            lastRead = input.read(readBuffer)
            if (lastRead == -1) {
                inputEof = true
                writeBackUnsupported(jsonString, "$UNSUPPORTED_FORMAT_MESSAGE. \"sourcesContent\" or \"sources\" not found")
                return
            }

            // Try find PROLOG_END. Note the case when prolog contents is splitted between reads.
            // Like, one buffer ends with `],"sourc`, and the other starts with `esContent"`
            val prevEnd = jsonString.length
            jsonString.append(readBuffer, 0, lastRead)
            jsonPrologPos = jsonString.indexOf(PROLOG_END, prevEnd - PROLOG_END.length)
            if (jsonPrologPos == -1) {
                if (jsonString.length + lastRead > prologLimit) {
                    writeBackUnsupported(jsonString, "Too many sources or format is not supported")
                    return
                }
            } else break
        }

        // create StringWriter to write transformed prolog and contents that was read after PROLOG_END
        bufferWriter = StringWriter(jsonString.length)
        bufferJsonWriter = JsonWriter(bufferWriter)

        // parse json in prolog and write it back to bufferJsonWriter with transformed source paths
        val json = JsonReader(StringReader(jsonString.toString()))
        try {
            json.beginObject()
            bufferJsonWriter.beginObject()

            reading@ while (true) {
                val token = json.peek()
                check(token == JsonToken.NAME) { "JSON key expected, but $token found" }
                val key = json.nextName()
                when (key) {
                    "sources" -> {
                        json.beginArray()
                        bufferJsonWriter.name("sources").beginArray()
                        while (json.peek() != JsonToken.END_ARRAY) {
                            val path = json.nextString()
                            bufferJsonWriter.value(transformString(path))
                        }
                        json.endArray()
                    }
                    "version" -> bufferJsonWriter.name(key).value(json.nextInt())
                    "file" -> bufferJsonWriter.name(key).value(json.nextString())
                    "sourcesContent" -> break@reading
                    else -> throw IllegalStateException("Unknown key \"$key\"")
                }
            }

            // leave bufferJsonWriter unclosed

            // push back contents that was read after PROLOG_END
            bufferWriter.append(jsonString.substring(jsonPrologPos))
        } catch (e: IllegalStateException) {
            writeBackUnsupported(jsonString, json, e.message!!)
        } catch (e: MalformedJsonException) {
            writeBackUnsupported(jsonString, json, "Malformed JSON")
        }
    }

    private fun writeBackUnsupported(jsonString: StringBuilder, reader: JsonReader, message: String) =
        writeBackUnsupported(
            jsonString,
            "$UNSUPPORTED_FORMAT_MESSAGE. $message at ${reader.toString().replace("JsonReader at ", "")} in `$jsonString"
        )

    private fun writeBackUnsupported(jsonString: StringBuilder, reason: String) =
        writeBackUnsupported(jsonString.toString(), reason)

    private fun writeBackUnsupported(jsonString: String, reason: String) {
        warnUnsupported(reason)
        bufferWriter = StringWriter(jsonString.length)
        bufferWriter.append(jsonString)
    }

    protected open fun warnUnsupported(reason: String) {
        log.warn("Cannot rewrite paths in JavaScript source maps: $reason")
    }

    protected open fun transformString(value: String): String =
        File(srcSourceRoot)
            .resolve(value)
            .canonicalFile
            .relativeToOrSelf(File(targetSourceRoot))
            .path

    override fun read(): Int {
        maybeReadFirst()
        if (bufferAvailable == 0) {
            if (inputEof) return -1
            val read = input.read()
            if (read == -1) {
                inputEof = true
                return -1
            } else {
                return read
            }
        }
        return buffer[bufferReadPos++].toInt()
    }

    override fun read(dest: CharArray, initialDestOffset: Int, n: Int): Int {
        maybeReadFirst()

        var todo = n
        var destOffset = initialDestOffset
        while (todo > 0) {
            if (bufferAvailable == 0) {
                if (inputEof) return if (n == todo) -1 else n - todo
                val read = input.read(dest, destOffset, todo)
                if (read == -1) {
                    inputEof = true
                    return n - todo
                } else {
                    return read + (n - todo)
                }
            }

            val toRead = min(todo, bufferAvailable)
            buffer.getChars(bufferReadPos, bufferReadPos + toRead, dest, destOffset)
            bufferReadPos += toRead
            destOffset += toRead
            todo -= toRead
        }

        return n - todo
    }

    override fun skip(n: Long): Long {
        maybeReadFirst()

        var todo = n.toInt()
        while (todo > 0) {
            if (bufferAvailable == 0) {
                if (inputEof) return n - todo
                val read = input.skip(todo.toLong())
                if (read == 0L && todo != 0) {
                    inputEof = true
                    return n - todo
                } else {
                    return read + (n - todo)
                }
            }

            val toRead = min(todo, bufferAvailable)
            bufferReadPos += toRead
            todo -= toRead
        }
        return n - todo
    }
}
