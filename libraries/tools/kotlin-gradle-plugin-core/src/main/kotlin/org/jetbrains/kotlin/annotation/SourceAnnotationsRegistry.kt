package org.jetbrains.kotlin.annotation

import org.jetbrains.kotlin.incremental.components.SourceRetentionAnnotationHandler
import java.io.*
import java.util.*

class SourceAnnotationsRegistry(private val file: File) : SourceRetentionAnnotationHandler {
    private val mutableAnnotations: MutableSet<String> by lazy { readAnnotations() }
    val annotations: Set<String>
            get() = mutableAnnotations

    override fun register(internalName: String) {
        mutableAnnotations.add(internalName)
    }

    fun clear() {
        mutableAnnotations.clear()
        file.delete()
    }

    fun flush() {
        if (mutableAnnotations.isEmpty()) {
            file.delete()
            return
        }

        if (!file.exists()) {
            file.parentFile.mkdirs()
            file.createNewFile()
        }

        ObjectOutputStream(BufferedOutputStream(file.outputStream())).use { out ->
            out.writeInt(mutableAnnotations.size)
            mutableAnnotations.forEach { out.writeUTF(it) }
        }
    }

    private fun readAnnotations(): MutableSet<String> {
        val result = HashSet<String>()

        if (!file.exists()) return result

        ObjectInputStream(BufferedInputStream(file.inputStream())).use { input ->
            val size = input.readInt()
            repeat(size) {
                result.add(input.readUTF())
            }
        }

        return result
    }
}
