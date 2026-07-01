/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.klib

import kotlinx.metadata.klib.KlibMetadataVersion
import kotlin.metadata.KmConstructor
import kotlin.metadata.KmFunction
import kotlin.metadata.KmPackage
import kotlin.metadata.KmProperty
import kotlin.metadata.internal.common.KmModuleFragment
import kotlinx.metadata.klib.KlibModuleMetadata
import kotlinx.metadata.klib.annotations
import kotlinx.metadata.klib.className
import kotlinx.metadata.klib.fqName
import org.jetbrains.kotlin.cli.klib.MetadataDumpMode.COMPACT_WITH_STABLE_ORDER
import org.jetbrains.kotlin.cli.klib.MetadataDumpMode.DEFAULT
import org.jetbrains.kotlin.cli.klib.MetadataDumpMode.ULTRACOMPACT_WITH_STABLE_ORDER
import org.jetbrains.kotlin.kotlinp.Kotlinp
import org.jetbrains.kotlin.kotlinp.Printer
import org.jetbrains.kotlin.kotlinp.Settings
import org.jetbrains.kotlin.kotlinp.klib.*
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.components.metadata
import org.jetbrains.kotlin.library.metadataVersion
import java.util.LinkedList
import java.util.Queue
import kotlin.metadata.KmAnnotation
import kotlin.metadata.KmClass
import kotlin.metadata.KmEnumEntry
import kotlin.metadata.KmType
import kotlin.metadata.KmTypeAlias
import kotlin.metadata.KmTypeParameter

internal class MetadataDumper(private val output: KlibToolOutput) {

    fun dumpLibrary(library: KotlinLibrary, metadataDumpMode: MetadataDumpMode) {
        val module = loadModuleMetadata(library)

        when (metadataDumpMode) {
            DEFAULT -> dumpMetadataInDefaultMode(module)
            COMPACT_WITH_STABLE_ORDER -> dumpMetadataInCompactMode(module)
            ULTRACOMPACT_WITH_STABLE_ORDER -> dumpMetadataInUltracompactMode(module)
        }
    }

    private fun loadModuleMetadata(library: KotlinLibrary) = KlibModuleMetadata.readLenient(
            object : KlibModuleMetadata.MetadataLibraryProvider {
                private val metadata = library.metadata
                override val moduleHeaderData get() = metadata.moduleHeaderData
                override val metadataVersion = KlibMetadataVersion((library.metadataVersion?.toArray()
                        ?: error("No metadata version specified in ${library.path}")))
                override fun packageMetadata(fqName: String, partName: String) = metadata.getPackageFragment(fqName, partName)
                override fun packageMetadataParts(fqName: String) = metadata.getPackageFragmentNames(fqName)
            }
    )

    private fun preprocessMetadataForTests(module: KlibModuleMetadata) = KlibModuleMetadata(
        name = module.name,
        fragments = module.fragments.groupBy { it.fqName }.mapNotNull { [packageFqName, fragments] ->
            val classNames = fragments.flatMap { it.className }.sorted()
            val classes = fragments.flatMap { it.classes }.sortedBy { it.name }
            val functions = fragments.flatMap { it.pkg?.functions.orEmpty() }.sortedBy { it.sortingKey() }
            val properties = fragments.flatMap { it.pkg?.properties.orEmpty() }.sortedBy { it.sortingKey() }
            val typeAliases = fragments.flatMap { it.pkg?.typeAliases.orEmpty() }.sortedBy { it.name }

            if (classNames.isEmpty() && classes.isEmpty() && functions.isEmpty() && properties.isEmpty() && typeAliases.isEmpty())
                return@mapNotNull null

            classes.forEach { clazz ->
                clazz.constructors.sortBy { it.sortingKey() }
                clazz.functions.sortBy { it.sortingKey() }
                clazz.properties.sortBy { it.sortingKey() }
                clazz.typeAliases.sortBy { it.name }
            }

            KmModuleFragment().apply {
                this.fqName = packageFqName
                this.className += classNames
                this.classes += classes

                if (functions.isNotEmpty() || properties.isNotEmpty() || typeAliases.isNotEmpty()) {
                    this.pkg = KmPackage().apply {
                        this.functions += functions
                        this.properties += properties
                        this.typeAliases += typeAliases
                    }
                }
            }
        }.sortedBy { it.fqName.orEmpty() },
        metadataVersion = module.metadataVersion,
    )

    private fun KmConstructor.sortingKey() = constructorId("")
    private fun KmFunction.sortingKey() = functionId("")
    private fun KmProperty.sortingKey() = propertyId("")

    private fun dumpMetadataInDefaultMode(module: KlibModuleMetadata) {
        KlibKotlinp(
                settings = Settings(
                        isVerbose = true,
                        sortDeclarations = false,
                ),
                signatureComputer = null
        ).renderModule(module, Printer(output))
    }

    private fun dumpMetadataInCompactMode(module: KlibModuleMetadata) {
        KlibKotlinp(
                settings = Settings(
                        isVerbose = true,
                        sortDeclarations = true,
                ),
                signatureComputer = null
        ).renderModule(preprocessMetadataForTests(module), Printer(output))
    }

    private fun dumpMetadataInUltracompactMode(module: KlibModuleMetadata) {
        buildString { UltracompactKlibKotlinp().renderModule(preprocessMetadataForTests(module), Printer(this)) }.lineSequence()
                .dropBlankLines()
                .removeIndents()
                .dropSimpleComments()
                .dropBraces()
                .dropTrailingCommas()
                .joinTo(output, separator = "\n")
    }

    companion object {
        private fun Sequence<String>.dropBlankLines(): Sequence<String> = filterNot { line -> line.isBlank() }

        private fun Sequence<String>.removeIndents(): Sequence<String> = map { line -> line.trimStart() }

        private fun Sequence<String>.dropSimpleComments(): Sequence<String> = filterNot { line -> line.startsWith("//") }

        /**
         * Drop braces blocks, i.e. ".* {" & "}".
         */
        private fun Sequence<String>.dropBraces(): Sequence<String> = mapNotNull { line ->
            // drop opening and closing braces
            if (line == "}") return@mapNotNull null
            line.removeSuffix(" {")
        }

        /**
         * Drop trailing commas, which may remain after enum entries.
         */
        private fun Sequence<String>.dropTrailingCommas(): Sequence<String> = mapNotNull { line ->
            // drop opening and closing braces
            line.removeSuffix(",")
        }
    }
}

private class UltracompactKlibKotlinp : Kotlinp(
        settings = Settings(
                isVerbose = false,
                sortDeclarations = true,
                showAnnotationArguments = false,
                annotationsInOneLine = true,
                showTypeAbbreviations = false,
                showVarargTypes = false,
        )
) {
    private val fullyQualifiedDeclarationNamesQueue: Queue<Pair<DeclarationKind, String>> = LinkedList()

    fun renderModule(moduleMetadata: KlibModuleMetadata, printer: Printer) {
        for (fragment in moduleMetadata.fragments) {
            for (clazz in fragment.classes) {
                withFullyQualifiedDeclarationNames(clazz.fullyQualifiedDeclarationNames()) {
                    renderClass(clazz, printer)
                }
            }

            fragment.pkg?.let { pkg ->
                val prefix = fragment.fqName?.replace('.', '/')?.let { if (it.isEmpty()) "" else "$it/" }.orEmpty()

                for (function in pkg.functions)
                    withFullyQualifiedDeclarationNames(function.fullyQualifiedDeclarationNames(prefix)) {
                        renderFunction(function, printer)
                    }

                for (property in pkg.properties)
                    withFullyQualifiedDeclarationNames(property.fullyQualifiedDeclarationNames(prefix)) {
                        renderProperty(property, printer)
                    }

                for (typeAlias in pkg.typeAliases)
                    withFullyQualifiedDeclarationNames(typeAlias.fullyQualifiedDeclarationNames(prefix)) {
                        renderTypeAlias(typeAlias, printer)
                    }
            }
        }
    }

    private inline fun withFullyQualifiedDeclarationNames(
            newFullyQualifiedDeclarationNames: List<Pair<DeclarationKind, String>>,
            block: () -> Unit
    ) {
        check(fullyQualifiedDeclarationNamesQueue.isEmpty())
        fullyQualifiedDeclarationNamesQueue += newFullyQualifiedDeclarationNames
        block()
        check(fullyQualifiedDeclarationNamesQueue.isEmpty())
    }

    override fun Printer.appendName(clazz: KmClass): Printer {
        val [declarationKind, fullName] = fullyQualifiedDeclarationNamesQueue.poll()

        check(declarationKind == DeclarationKind.CLASS)
        check(fullName == clazz.name)

        return append(fullName)
    }

    override fun Printer.appendName(constructor: KmConstructor): Printer {
        val [declarationKind, fullName] = fullyQualifiedDeclarationNamesQueue.poll()

        check(declarationKind == DeclarationKind.CONSTRUCTOR)

        return append("constructor $fullName")
    }

    override fun Printer.appendName(function: KmFunction): Printer {
        val [declarationKind, fullName] = fullyQualifiedDeclarationNamesQueue.poll()

        check(declarationKind == DeclarationKind.FUNCTION)
        check(fullName.endsWithSimpleName(function.name))

        return append(fullName)
    }

    override fun Printer.appendName(property: KmProperty): Printer {
        val [declarationKind, fullName] = fullyQualifiedDeclarationNamesQueue.poll()

        check(declarationKind == DeclarationKind.PROPERTY)
        check(fullName.endsWithSimpleName(property.name))

        return append(fullName)
    }

    override fun Printer.appendGetterName(property: KmProperty): Printer {
        val [declarationKind, fullName] = fullyQualifiedDeclarationNamesQueue.poll()

        check(declarationKind == DeclarationKind.PROPERTY_ACCESSOR)
        check(fullName.endsWith(".<get-${property.name}>"))

        return append("/* getter */ $fullName")
    }

    override fun Printer.appendSetterName(property: KmProperty): Printer {
        val [declarationKind, fullName] = fullyQualifiedDeclarationNamesQueue.poll()

        check(declarationKind == DeclarationKind.PROPERTY_ACCESSOR)
        check(fullName.endsWith(".<set-${property.name}>"))

        return append("/* setter */ $fullName")
    }

    override fun Printer.appendName(typeAlias: KmTypeAlias): Printer {
        val [declarationKind, fullName] = fullyQualifiedDeclarationNamesQueue.poll()

        check(declarationKind == DeclarationKind.TYPEALIAS)
        check(fullName.endsWithSimpleName(typeAlias.name))

        return append(fullName)
    }

    override fun Printer.appendName(enumEntry: KmEnumEntry): Printer {
        val [declarationKind, fullName] = fullyQualifiedDeclarationNamesQueue.poll()

        check(declarationKind == DeclarationKind.ENUM_ENTRY)
        check(fullName.endsWith(".${enumEntry.name}"))

        return append("enum entry $fullName")
    }

    override fun getAnnotations(typeParameter: KmTypeParameter): List<KmAnnotation> = typeParameter.annotations
    override fun getAnnotations(type: KmType): List<KmAnnotation> = type.annotations

    override fun Printer.appendCompileTimeConstant(property: KmProperty) = append("...")

    private enum class DeclarationKind { CLASS, CONSTRUCTOR, ENUM_ENTRY, FUNCTION, PROPERTY, PROPERTY_ACCESSOR, TYPEALIAS }

    companion object {
        private fun String.endsWithSimpleName(simpleName: String): Boolean = when {
            length == simpleName.length -> this == simpleName
            length > simpleName.length -> endsWith(".$simpleName") || endsWith("/$simpleName")
            else -> false
        }

        private fun KmClass.fullyQualifiedDeclarationNames(): List<Pair<DeclarationKind, String>> = buildList {
            val className = name
            this += DeclarationKind.CLASS to className

            val prefix = "$className."

            constructors.mapTo(this) { DeclarationKind.CONSTRUCTOR to "$prefix<init>" }
            functions.flatMapTo(this) { it.fullyQualifiedDeclarationNames(prefix) }
            properties.flatMapTo(this) { it.fullyQualifiedDeclarationNames(prefix) }
            typeAliases.flatMapTo(this) { it.fullyQualifiedDeclarationNames(prefix) }
            kmEnumEntries.mapTo(this) { DeclarationKind.ENUM_ENTRY to prefix + it.name }
        }

        private fun KmFunction.fullyQualifiedDeclarationNames(prefix: String): List<Pair<DeclarationKind, String>> =
                listOf(DeclarationKind.FUNCTION to prefix + name)

        private fun KmProperty.fullyQualifiedDeclarationNames(prefix: String): List<Pair<DeclarationKind, String>> = buildList {
            val propertyName = prefix + name
            this += DeclarationKind.PROPERTY to propertyName

            this += DeclarationKind.PROPERTY_ACCESSOR to "$propertyName.<get-$name>"
            if (setter != null) {
                this += DeclarationKind.PROPERTY_ACCESSOR to "$propertyName.<set-$name>"
            }
        }

        private fun KmTypeAlias.fullyQualifiedDeclarationNames(prefix: String): List<Pair<DeclarationKind, String>> =
                listOf(DeclarationKind.TYPEALIAS to prefix + name)
    }
}
