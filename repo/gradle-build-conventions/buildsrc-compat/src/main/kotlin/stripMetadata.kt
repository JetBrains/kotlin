@file:Suppress("unused") // usages in build scripts are not tracked properly

import org.gradle.api.logging.Logger
import org.jetbrains.org.objectweb.asm.*
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.*
import java.util.jar.JarFile
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

private val CONSTANT_TIME_FOR_ZIP_ENTRIES = GregorianCalendar(1980, 1, 1, 0, 0, 0).timeInMillis

/**
 * Removes @kotlin.Metadata annotations from compiled Kotlin classes
 */
fun stripMetadata(logger: Logger, classNamePattern: String, inFile: File, outFile: File, preserveFileTimestamps: Boolean = true) {
    val classRegex = classNamePattern.toRegex()

    assert(inFile.exists()) { "Input file not found at $inFile" }

    fun transform(entryName: String, bytes: ByteArray): ByteArray {
        if (!entryName.endsWith(".class")) return bytes
        if (!classRegex.matches(entryName.removeSuffix(".class"))) return bytes

        var changed = false
        val classWriter = ClassWriter(0)
        val classVisitor = object : ClassVisitor(Opcodes.API_VERSION, classWriter) {
            override fun visitAnnotation(desc: String, visible: Boolean): AnnotationVisitor? {
                if (Type.getType(desc).internalName == "kotlin/Metadata") {
                    changed = true
                    return null
                }
                return super.visitAnnotation(desc, visible)
            }
        }
        ClassReader(bytes).accept(classVisitor, 0)
        if (!changed) return bytes

        return classWriter.toByteArray()
    }

    ZipOutputStream(BufferedOutputStream(FileOutputStream(outFile))).use { outJar ->
        JarFile(inFile).use { inJar ->
            for (entry in inJar.entries()) {
                val inBytes = inJar.getInputStream(entry).readBytes()
                val outBytes = transform(entry.name, inBytes)

                if (inBytes.size < outBytes.size) {
                    error("Size increased for ${entry.name}: was ${inBytes.size} bytes, became ${outBytes.size} bytes")
                }

                val newEntry = ZipEntry(entry.name)
                if (!preserveFileTimestamps) {
                    newEntry.time = CONSTANT_TIME_FOR_ZIP_ENTRIES
                }
                outJar.putNextEntry(newEntry)
                outJar.write(outBytes)
                outJar.closeEntry()
            }
        }
    }

    logger.info("Stripping @kotlin.Metadata annotations from all classes in $inFile")
    logger.info("Class name pattern: $classNamePattern")
    logger.info("Input file size: ${inFile.length()} bytes")
    logger.info("Output written to $outFile")
    logger.info("Output file size: ${outFile.length()} bytes")
}
