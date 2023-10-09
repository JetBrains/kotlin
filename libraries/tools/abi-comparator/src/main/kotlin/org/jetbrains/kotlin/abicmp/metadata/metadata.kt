package org.jetbrains.kotlin.abicmp.metadata

import kotlinx.metadata.jvm.KotlinClassMetadata
import org.jetbrains.kotlin.abicmp.listOfNotNull
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.tree.AnnotationNode
import org.jetbrains.org.objectweb.asm.tree.ClassNode
import java.util.*

// Based on 'kotlinp' tool code (https://github.com/JetBrains/kotlin/tree/master/libraries/tools/kotlinp)

fun ClassNode.renderKotlinMetadata(): String? {
    val ann = visibleAnnotations.listOfNotNull<AnnotationNode>()
        .find { it.desc == Type.getDescriptor(Metadata::class.java) }
        ?: return null

    val header = ann.getKotlinClassHeader() ?: return null

    val metadata = try {
        KotlinClassMetadata.read(header)
    } catch (e: Exception) {
        return null
    }

    return when (metadata) {
        is KotlinClassMetadata.Class ->
            ClassPrinter().print(metadata)

        is KotlinClassMetadata.FileFacade ->
            FileFacadePrinter().print(metadata)

        is KotlinClassMetadata.SyntheticClass -> {
            if (metadata.isLambda)
                LambdaPrinter().print(metadata)
            else
                buildString { appendLine("synthetic class") }
        }

        is KotlinClassMetadata.MultiFileClassFacade ->
            MultiFileClassFacadePrinter().print(metadata)

        is KotlinClassMetadata.MultiFileClassPart ->
            MultiFileClassPartPrinter().print(metadata)

        is KotlinClassMetadata.Unknown ->
            buildString { appendLine("unknown file (k=${header.kind})") }

        else ->
            // NB unsupported metadata version would always produce unique metadata string
            "[${UUID.randomUUID()}] unsupported file: " +
                    "${header.metadataVersion.toList()}.${header.extraInt and (1 shl 3)}"
    }
}

@Suppress("UNCHECKED_CAST")
private fun AnnotationNode.getKotlinClassHeader(): Metadata? {
    var kind: Int? = null
    var metadataVersion: IntArray? = null
    var bytecodeVersion: IntArray? = null
    var data1: Array<String>? = null
    var data2: Array<String>? = null
    var extraString: String? = null
    var packageName: String? = null
    var extraInt: Int? = null

    var i = 0
    while (i < values.size) {
        val name = values[i++]
        val value = values[i++]

        when (name) {
            "k" -> kind = value as? Int
            "mv" -> metadataVersion = (value as List<Int>).toIntArray()
            "bv" -> bytecodeVersion = (value as List<Int>).toIntArray()
            "xs" -> extraString = value as? String
            "xi" -> extraInt = value as? Int
            "pn" -> packageName = value as? String
            "d1" -> data1 = (value as List<String>).toTypedArray()
            "d2" -> data2 = (value as List<String>).toTypedArray()
        }
    }

    return Metadata(
        kind ?: 1,
        bytecodeVersion ?: intArrayOf(1, 0, 3),
        metadataVersion ?: intArrayOf(),
        data1 ?: emptyArray(),
        data2 ?: emptyArray(),
        extraString ?: "",
        packageName ?: "",
        extraInt ?: 0
    )
}

private fun List<Int>.toIntArray(): IntArray? =
    IntArray(size) { this[it] }

