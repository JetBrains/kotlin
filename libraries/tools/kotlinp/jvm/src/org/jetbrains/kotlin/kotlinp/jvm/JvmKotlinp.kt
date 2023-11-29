/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kotlinp.jvm

import kotlin.metadata.*
import kotlin.metadata.jvm.*
import org.jetbrains.kotlin.kotlinp.*

class JvmKotlinp(settings: Settings) : Kotlinp(settings) {
    fun printClassFile(classFile: KotlinClassMetadata): String = printString {
        when (classFile) {
            is KotlinClassMetadata.Class -> renderClass(classFile.kmClass, this)
            is KotlinClassMetadata.FileFacade -> renderPackage(classFile.kmPackage, this)
            is KotlinClassMetadata.SyntheticClass -> renderSyntheticClass(classFile, this)
            is KotlinClassMetadata.MultiFileClassFacade -> renderMultiFileClassFacade(classFile, this)
            is KotlinClassMetadata.MultiFileClassPart -> renderMultiFileClassPart(classFile, this)
            is KotlinClassMetadata.Unknown -> appendLine("unknown file")
        }
    }

    @OptIn(UnstableMetadataApi::class)
    fun printModuleFile(metadata: KotlinModuleMetadata?): String = printString {
        if (metadata != null) renderModuleFile(metadata, this)
        else appendLine("unsupported file")
    }

    private fun renderSyntheticClass(clazz: KotlinClassMetadata.SyntheticClass, printer: Printer): Unit = with(printer) {
        if (clazz.isLambda) {
            appendLine("lambda {")
            withIndent {
                val lambda = clazz.kmLambda ?: throw KotlinpException("Synthetic class $clazz is not a lambda")
                renderFunction(lambda.function, printer)
            }
            appendLine("}")
        } else {
            appendLine("synthetic class")
        }
    }

    private fun renderMultiFileClassFacade(clazz: KotlinClassMetadata.MultiFileClassFacade, printer: Printer): Unit = with(printer) {
        appendLine("multi-file class {")
        withIndent {
            for (part in clazz.partClassNames) {
                appendCommentedLine(part)
            }
        }
        appendLine("}")
    }

    private fun renderMultiFileClassPart(clazz: KotlinClassMetadata.MultiFileClassPart, printer: Printer) {
        renderPackage(clazz.kmPackage, printer) {
            printer.appendCommentedLine("facade: ", clazz.facadeClassName)
        }
    }

    @OptIn(UnstableMetadataApi::class)
    fun renderModuleFile(metadata: KotlinModuleMetadata, printer: Printer): Unit = with(printer) {
        appendLine("module {")

        withIndent {
            val module = metadata.kmModule
            module.packageParts.forEach { (fqName, kmPackageParts) ->
                val presentableFqName = fqName.ifEmpty { "<root>" }
                appendLine("package ", presentableFqName, " {")
                withIndent {
                    for (fileFacade in kmPackageParts.fileFacades) {
                        appendLine(fileFacade)
                    }
                    for ((multiFileClassPart, facade) in kmPackageParts.multiFileClassParts) {
                        appendLine(multiFileClassPart, " (", facade, ")")
                    }
                }
                appendLine("}")
            }

            if (module.optionalAnnotationClasses.isNotEmpty()) {
                appendLine()
                appendCommentedLine("Optional annotations")
                appendLine()
                module.optionalAnnotationClasses.forEach { renderClass(it, printer) }
            }
        }

        appendLine("}")
    }

    override fun getAnnotations(typeParameter: KmTypeParameter) = typeParameter.annotations
    override fun getAnnotations(type: KmType) = type.annotations

    override fun sortConstructors(constructors: List<KmConstructor>) = constructors.sortedBy { it.signature.toString() }
    override fun sortFunctions(functions: List<KmFunction>) = functions.sortedBy { it.signature.toString() }
    override fun sortProperties(properties: List<KmProperty>) = properties.sortedBy { it.getterSignature?.toString() ?: it.name }

    override fun Printer.appendSignatures(constructor: KmConstructor) {
        constructor.signature?.let {
            appendCommentedLine("signature: ", it)
        }
    }

    override fun Printer.appendSignatures(function: KmFunction) {
        function.signature?.let {
            appendCommentedLine("signature: ", it)
        }
    }

    override fun Printer.appendSignatures(property: KmProperty) {
        property.fieldSignature?.let {
            appendCommentedLine("field: ", it)
        }
        property.getterSignature?.let {
            appendCommentedLine("getter: ", it)
        }
        property.setterSignature?.let {
            appendCommentedLine("setter: ", it)
        }
    }

    override fun Printer.appendCustomAttributes(property: KmProperty) {
        property.syntheticMethodForAnnotations?.let {
            appendCommentedLine("synthetic method for annotations: ", it)
        }
        property.syntheticMethodForDelegate?.let {
            appendCommentedLine("synthetic method for delegate: ", it)
        }
        if (property.isMovedFromInterfaceCompanion) {
            appendCommentedLine("is moved from interface companion")
        }
    }

    override fun Printer.appendOrigin(clazz: KmClass) {
        clazz.anonymousObjectOriginName?.let {
            appendCommentedLine("anonymous object origin: ", it)
        }
    }

    override fun Printer.appendOrigin(function: KmFunction) {
        function.lambdaClassOriginName?.let {
            appendCommentedLine("lambda class origin: ", it)
        }
    }

    override fun Printer.appendCustomAttributes(clazz: KmClass) {
        appendExtensions(clazz.localDelegatedProperties, clazz.moduleName)

        if (clazz.hasMethodBodiesInInterface) {
            appendLine()
            appendCommentedLine("has method bodies in interface")
        }
        if (clazz.isCompiledInCompatibilityMode) {
            appendLine()
            appendCommentedLine("is compiled in compatibility mode")
        }
    }

    override fun Printer.appendCustomAttributes(pkg: KmPackage) {
        appendExtensions(pkg.localDelegatedProperties, pkg.moduleName)
    }

    private fun Printer.appendExtensions(localDelegatedProperties: List<KmProperty>, moduleName: String?) {
        localDelegatedProperties.sortIfNeeded(::sortProperties).forEachIndexed { index, property ->
            appendLine()
            appendCommentedLine("local delegated property #", index)

            // Comment all uncommented lines to not make it look like these properties are declared here
            printString { renderProperty(property, this) }
                .lineSequence()
                .filter { it.isNotBlank() }
                .forEach { appendCommentedLine(it) }
        }

        if (settings.isVerbose) {
            moduleName?.let {
                appendLine()
                appendCommentedLine("module name: ", it)
            }
        }
    }

    override fun Printer.appendEnumEntries(clazz: KmClass) {
        clazz.enumEntries.forEach { enumEntry ->
            appendLine()
            appendLine(enumEntry, ",")
        }
    }

    override fun Printer.appendCompileTimeConstant(property: KmProperty): Printer {
        return append("...")
    }

    override fun isRaw(type: KmType) = type.isRaw

    override fun renderFlexibleTypeUpperBound(flexibleTypeUpperBound: KmFlexibleTypeUpperBound): String? {
        @Suppress("DEPRECATION_ERROR")
        return if (flexibleTypeUpperBound.typeFlexibilityId == JvmTypeExtensionVisitor.PLATFORM_TYPE_ID)
            printString { appendType(flexibleTypeUpperBound.type) }
        else
            null
    }
}
