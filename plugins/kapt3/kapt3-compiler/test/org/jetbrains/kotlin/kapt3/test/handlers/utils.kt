/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt3.test.handlers

import com.sun.tools.javac.tree.JCTree.*
import com.sun.tools.javac.tree.Pretty
import org.jetbrains.kotlin.kotlinp.Settings
import org.jetbrains.kotlin.kotlinp.jvm.JvmKotlinp
import org.jetbrains.kotlin.load.java.JvmAnnotationNames
import org.jetbrains.kotlin.test.Assertions
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.utils.withExtension
import kotlin.metadata.KmDeclarationContainer
import kotlin.metadata.KmVersionRequirement
import kotlin.metadata.hasConstant
import kotlin.metadata.isConst
import kotlin.metadata.jvm.KotlinClassMetadata

fun Assertions.checkTxtAccordingToBackend(module: TestModule, actual: String, fileSuffix: String = "") {
    val testDataFile = module.files.first().originalFile
    val expectedFile = testDataFile.withExtension("$fileSuffix.txt")
    assertEqualsToFile(expectedFile, actual)
}

private val KOTLIN_METADATA_REGEX = "@kotlin\\.Metadata\\(.*\\)".toRegex()

fun removeMetadataAnnotationContents(s: String): String =
    s.replace(KOTLIN_METADATA_REGEX, "@kotlin.Metadata()")

internal fun renderMetadata(pretty: Pretty, tree: JCAnnotation): String {
    val args = tree.args.filterIsInstance<JCAssign>().associate { (it.lhs as JCIdent).name.toString() to it.rhs }
    val metadata = Metadata(
        kind = args[JvmAnnotationNames.KIND_FIELD_NAME].intValue() ?: 1,
        metadataVersion = args[JvmAnnotationNames.METADATA_VERSION_FIELD_NAME].arrayValue()?.map { it.intValue()!! }?.toIntArray() ?: intArrayOf(),
        data1 = args[JvmAnnotationNames.METADATA_DATA_FIELD_NAME].arrayValue()?.map { it.stringValue()!! }?.toTypedArray() ?: arrayOf(),
        data2 = args[JvmAnnotationNames.METADATA_STRINGS_FIELD_NAME].arrayValue()?.map { it.stringValue()!! }?.toTypedArray() ?: arrayOf(),
        extraInt = args[JvmAnnotationNames.METADATA_EXTRA_INT_FIELD_NAME].intValue() ?: 0,
        extraString = args[JvmAnnotationNames.METADATA_EXTRA_STRING_FIELD_NAME].stringValue() ?: "",
        packageName = args[JvmAnnotationNames.METADATA_PACKAGE_NAME_FIELD_NAME].stringValue() ?: "",
    )
    val text = renderNormalizedMetadata(metadata)
    // Add "*" at the beginning of each line to make it look prettier.
    val result = text.joinToString("\n * ", "/**\n * ", "\n */\n")
    // Indent the rendered metadata for inner classes to make it look prettier. Unfortunately for that we need to read a private field.
    val indent = Pretty::class.java.getDeclaredField("lmargin").apply { isAccessible = true }.get(pretty) as Int
    return result.replace("\n", "\n" + " ".repeat(indent))
}

private fun JCExpression?.intValue(): Int? =
    (this as? JCLiteral)?.value as? Int

private fun JCExpression?.stringValue(): String? =
    (this as? JCLiteral)?.value as? String

private fun JCExpression?.arrayValue(): List<JCExpression>? =
    (this as? JCNewArray)?.elems

fun renderNormalizedMetadata(metadataAnnotation: Metadata): List<String> {
    val metadata = KotlinClassMetadata.readLenient(metadataAnnotation)
    processMetadata(metadata)
    val text = JvmKotlinp(Settings(isVerbose = true, sortDeclarations = true)).printClassFile(metadata)
    // "/*" and "*/" delimiters are used in kotlinp, for example to render type parameter names. Replace them with something else
    // to avoid them being interpreted as Java comments.
    return text.split('\n').dropLast(1).map { it.replace("/*", "(*").replace("*/", "*)") }
}

// Remove non-significant stuff from the metadata to ease comparison of generated stubs in K1 and K2 kapt implementations.
private fun processMetadata(metadata: KotlinClassMetadata) {
    when (metadata) {
        is KotlinClassMetadata.Class -> {
            val kmClass = metadata.kmClass

            kmClass.nestedClasses.sort()

            kmClass.versionRequirements.removeOldVersionRequirements()
            cleanupOldVersionRequirements(kmClass)

            for (property in kmClass.properties) {
                if (!property.isConst) {
                    // hasConstant is irrelevant for non-const properties and will be removed in KT-61138.
                    property.hasConstant = false
                }
            }
        }
        is KotlinClassMetadata.FileFacade -> {
            val kmPackage = metadata.kmPackage

            cleanupOldVersionRequirements(kmPackage)
        }
        is KotlinClassMetadata.MultiFileClassFacade -> {}
        is KotlinClassMetadata.MultiFileClassPart -> {}
        is KotlinClassMetadata.SyntheticClass -> {}
        is KotlinClassMetadata.Unknown -> error("Unknown metadata")
    }
}

private fun cleanupOldVersionRequirements(kmClass: KmDeclarationContainer) {
    for (function in kmClass.functions) {
        function.versionRequirements.removeOldVersionRequirements()
    }
    for (property in kmClass.properties) {
        property.versionRequirements.removeOldVersionRequirements()
    }
    for (typeAlias in kmClass.typeAliases) {
        typeAlias.versionRequirements.removeOldVersionRequirements()
    }
}

private fun MutableList<KmVersionRequirement>.removeOldVersionRequirements() {
    removeIf { it.version.major == 1 && it.version.minor < 7 }
}
