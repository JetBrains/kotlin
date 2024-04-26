/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer

import kotlin.metadata.*
import kotlinx.metadata.klib.KlibModuleMetadata
import kotlinx.metadata.klib.annotations
import kotlinx.metadata.klib.fqName
import kotlinx.metadata.klib.getterAnnotations
import org.jetbrains.kotlin.commonizer.metadata.utils.MetadataDeclarationsComparator
import org.jetbrains.kotlin.commonizer.metadata.utils.MetadataDeclarationsComparator.*
import org.jetbrains.kotlin.commonizer.metadata.utils.MetadataDeclarationsComparator.EntityKind.*
import org.jetbrains.kotlin.commonizer.metadata.utils.SerializedMetadataLibraryProvider
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.SerializedMetadata
import org.jetbrains.kotlin.library.metadata.parseModuleHeader
import org.jetbrains.kotlin.library.resolveSingleFileKlib
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.utils.addToStdlib.filterIsInstanceWithChecker
import org.jetbrains.kotlin.utils.addToStdlib.flattenTo
import org.junit.Test
import java.io.File
import kotlin.test.fail
import org.jetbrains.kotlin.konan.file.File as KFile

class MetadataCompatibilityForK2Test {
    @Test
    fun testK2diff() {
        val baseDir = File("/Users/Dmitriy.Dolovov/Downloads/libs-for-k1-k2-comparison") // TODO: edit the base directory
        val (k1LibraryFiles, k2LibraryFiles) = findLibraries(baseDir)

        // IMPORTANT: Preserve order of libraries in `k1LibraryFiles` and `k2LibraryFiles`!
        @Suppress("TestFailedLine", "RedundantSuppression")
        assertLibrariesAreEqual(
            k1LibraryFiles = k1LibraryFiles,
            k2LibraryFiles = k2LibraryFiles,
        )
    }
}

class MetadataCompatibilityDebugForK2Test {
    @Test
    fun testK2diffSingleFile() {
        assertLibrariesAreEqual(
            k1LibraryFiles = listOf(File("/Users/Andrei.Tyrin/IdeaProjects/samples/klibs/k1/samples.klib")),
            k2LibraryFiles = listOf(File("/Users/Andrei.Tyrin/IdeaProjects/samples/klibs/k2/samples.klib"))
        )
    }

    @Test
    fun testK2diffSingleJSFile() {
        assertLibrariesAreEqual(
            k1LibraryFiles = listOf(File("/Users/Andrei.Tyrin/IdeaProjects/_jb/ktor/ktor-utils/build19/libs/ktor-utils-js-3.0.0-beta-1.klib")),
            k2LibraryFiles = listOf(File("/Users/Andrei.Tyrin/IdeaProjects/_jb/ktor/ktor-utils/build20/libs/ktor-utils-js-3.0.0-beta-1.klib"))
        )
    }




    @Test
    fun flysto() {
        val dirs = File(location).listFiles()
        dirs?.forEach {
            val buildDir = it.listFiles().filter { it.name == "build19" }.firstOrNull()
            if (buildDir == null) {
                val newDirs = it.listFiles()
                newDirs?.forEach { new ->
                    println("Working on nested $new")
                    val newBuildDir = new.listFiles()?.filter { it.name == "build19" }?.firstOrNull()
                    if (newBuildDir != null) {
                        println("it has build dir")
                        fun jsPath(ver: String) = File("$new/build$ver/libs/").listFiles()
                            ?.filter { it.extension == "klib" }
                            ?.filterNot { it.name.contains("wasm") }

                        val klibs19 = jsPath("19")
                        val klibs20 = jsPath("20")
                        if (!klibs19.isNullOrEmpty() && !klibs20.isNullOrEmpty()) {
                            assertLibrariesAreEqual(
                                k1LibraryFiles = klibs19,
                                k2LibraryFiles = klibs20
                            )
                        }
                    }
                }
            } else {
                println("Working on $buildDir")
                if (it.name !in arrayOf(
//                        "log-details",
                        "aircraft",
//                        "admin",
//                        "browser",
//                        "components",
//                        "common",
//                        "action", "table", "commands", "columns", "fleet-insights",
//                        "log-core",
                    )
                ) return@forEach

                fun jsPath(ver: String) = File("$it/build$ver/libs/").listFiles()
                    ?.filter { it.extension == "klib" }
                    ?.filterNot { it.name.contains("wasm") }

                val klibs19 = jsPath("19")
                val klibs20 = jsPath("20")
                if (!klibs19.isNullOrEmpty() && !klibs20.isNullOrEmpty()) {
                    assertLibrariesAreEqual(
                        k1LibraryFiles = klibs19,
                        k2LibraryFiles = klibs20
                    )
                }
            }
        }
    }


    @Test
    fun ktor_prep() {
//        val ktorLocation = "/Users/Andrei.Tyrin/IdeaProjects/_jb/ktor"
//        val ktorLocation = "/Users/Andrei.Tyrin/IdeaProjects/_jb/ktor/ktor-client"
        val ktorLocation = "/Users/Andrei.Tyrin/IdeaProjects/_jb/ktor/ktor-client/ktor-client-plugins"
        val suffix = "19"
        val dirs = File(ktorLocation).listFiles().filter { it.name.startsWith("ktor-") }
        for (it in dirs) {
            val buildDir = it.listFiles().filter { it.name == "build" }.firstOrNull()
            if (buildDir != null) {
                buildDir.renameTo(File(buildDir.absolutePath + suffix))
                continue
            }

            val newDirs = it.listFiles().filter { it.name.startsWith("ktor-") }
            for (new in newDirs) {
                println("Working on nested $new")
                val newstedBuildDir = it.listFiles().filter { it.name == "build" }.firstOrNull()
                if (newstedBuildDir != null) {
                    newstedBuildDir.renameTo(File(newstedBuildDir.absolutePath + suffix))
                    continue
                }
            }
        }
    }

    @Test
    fun ktor_compare() {
//        val ktorLocation = "/Users/Andrei.Tyrin/IdeaProjects/_jb/ktor"
//        val ktorLocation = "/Users/Andrei.Tyrin/IdeaProjects/_jb/ktor/ktor-client"
        val ktorLocation = "/Users/Andrei.Tyrin/IdeaProjects/_jb/ktor/ktor-client/ktor-client-plugins"
        val suffix = "20"
        val modulesToSkip = setOf(
            "ktor-network",
            "ktor-utils",
            "ktor-io",
            "ktor-client-core",
            "ktor-client-tests",
        )
        val dirs = File(ktorLocation).listFiles().filter { it.name.startsWith("ktor-") }
        for (it in dirs) {
            println("Working on $it")
            if(it.name in modulesToSkip) continue
            val buildDir = it.listFiles().filter { it.name == "build$suffix" }.firstOrNull()
            if (buildDir != null) {
                println("Working on build $it")
                testByLocation(it.absolutePath)
                continue
            }

            val newDirs = it.listFiles().filter { it.name.startsWith("ktor-") }
            for (new in newDirs) {
                println("Working on build nested $new")
                val newstedBuildDir = it.listFiles().filter { it.name == "build$suffix" }.firstOrNull()
                if (newstedBuildDir != null) {
                    testByLocation(newstedBuildDir.absolutePath)
                    continue
                }
            }
        }
    }

    val location = "/Users/Andrei.Tyrin/IdeaProjects/_jb/space/app/app-web"
    @Test
    fun testK2diffClassFolder() {
        testByLocation(location)
    }
}

private fun testByLocation(location: String){
    fun path(ver: String) = "$location/build$ver/classes/kotlin"
    assertClassesFoldersEqual(
        k1ClassedFolderPath = path("19"),
        k2ClassedFolderPath = path("20"),
    )
    fun jsPath(ver: String) = File("$location/build$ver/libs/").listFiles()
        ?.filter { it.extension == "klib" }
        ?.filterNot { it.name.contains("wasm") }

    val klibs19 = jsPath("19")
    val klibs20 = jsPath("20")
    if (!klibs19.isNullOrEmpty() && !klibs20.isNullOrEmpty()) {
        assertLibrariesAreEqual(
            k1LibraryFiles = klibs19,
            k2LibraryFiles = klibs20
        )
    }
}

private fun assertClassesFoldersEqual(
    k1ClassedFolderPath: String,
    k2ClassedFolderPath: String,
) {
    val k1klibs = k1ClassedFolderPath.toKlibMap()
    val k2klibs = k2ClassedFolderPath.toKlibMap()

    assert(k1klibs.size == k2klibs.size)
    k1klibs.forEach {
        println(it.key)
        println("Compare: ${it.value} and ${k2klibs[it.key]}")
        assertLibrariesAreEqual(
            k1LibraryFiles = listOf(it.value),
            k2LibraryFiles = listOf(k2klibs[it.key]!!)
        )
    }
}

private fun String.toKlibMap() = File(this).listFiles()!!
    .filter { it.name !in setOf("jvm", "commonizer", "wasmJs", "wasmWasi", "native", "metadata", "js", "jsIr") }
    .associate { Pair(it.name, File(it.absolutePath + "/main/klib").listFiles()!!.first()) }

private fun findLibraries(baseDir: File): Pair<List<File>, List<File>> {
    val libraryFiles = baseDir.walkTopDown()
        .filter { it.isFile && it.extension == "klib" }
        .toList()

    fun File.relative(): File = relativeTo(baseDir)

    fun File.key(prefix: String): String = with(relative()) {
        val parentFile: File? = parentFile
        val newName = name.removePrefix(prefix)
        parentFile?.resolve(newName)?.path ?: newName
    }

    val k1Libraries = libraryFiles.filter { it.name.startsWith("k1-") }.associateBy { it.key("k1-") }
    val k2Libraries = libraryFiles.filter { it.name.startsWith("k2-") }.associateBy { it.key("k2-") }

    check(k1Libraries.keys == k2Libraries.keys) {
        """
            Mismatching sets of K1 and K2 libraries
            K1: ${k1Libraries.values.map { it.relative().path }.sorted()}
            K2: ${k2Libraries.values.map { it.relative().path }.sorted()}
        """.trimIndent()
    }

    return k1Libraries.entries.sortedBy { it.key }.map { it.value } to k2Libraries.entries.sortedBy { it.key }.map { it.value }
}

@Suppress("SameParameterValue")
private fun assertLibrariesAreEqual(
    k1LibraryFiles: List<File>,
    k2LibraryFiles: List<File>,
) {
    println("compare: $k1LibraryFiles and $k2LibraryFiles")
    val k1LibraryPaths = k1LibraryFiles.map { it.canonicalPath }
    val k2LibraryPaths = k2LibraryFiles.map { it.canonicalPath }

    check(k1LibraryPaths.size == k2LibraryPaths.size)
    check(k1LibraryPaths.size == k1LibraryPaths.toSet().size)
    check(k2LibraryPaths.size == k2LibraryPaths.toSet().size)
    check((k1LibraryPaths intersect k2LibraryPaths.toSet()).isEmpty())

    val k1Modules = loadKlibModulesMetadata(k1LibraryPaths)
    val k2Modules = loadKlibModulesMetadata(k2LibraryPaths)

    val results = (k1Modules zip k2Modules).map { (k1Module, k2Module) ->
        MetadataDeclarationsComparator.compare(k1Module, k2Module)
    }

    val mismatches = results.flatMap { result ->
        when (result) {
            is Result.Success -> emptyList()
            is Result.Failure -> result.mismatches
        }
    }.sortedBy { it::class.java.simpleName + "_" + it.kind }

    val unexpectedMismatches = MismatchesFilter(k1Resolver = Resolver(k1Modules), k2Resolver = Resolver(k2Modules)).filter(mismatches)
    if (unexpectedMismatches.isNotEmpty()) {
        val failureMessage = buildString {
            appendLine("${unexpectedMismatches.size} mismatches found while comparing K1 (A) module with K2 (B) module:")
            unexpectedMismatches.forEachIndexed { index, mismatch -> appendLine("${(index + 1)}.\n$mismatch") }
        }

        fail(failureMessage)
    }
}

private fun loadKlibModulesMetadata(libraryPaths: List<String>): List<KlibModuleMetadata> = libraryPaths.map { libraryPath ->
    val library = resolveSingleFileKlib(KFile(libraryPath))
    val metadata = loadBinaryMetadata(library)
    KlibModuleMetadata.read(SerializedMetadataLibraryProvider(metadata))
}

private fun loadBinaryMetadata(library: KotlinLibrary): SerializedMetadata {
    val moduleHeader = library.moduleHeaderData
    val fragmentNames = parseModuleHeader(moduleHeader).packageFragmentNameList.toSet()
    val fragments = fragmentNames.map { fragmentName ->
        val partNames = library.packageMetadataParts(fragmentName)
        partNames.map { partName -> library.packageMetadata(fragmentName, partName) }
    }

    return SerializedMetadata(
        module = moduleHeader,
        fragments = fragments,
        fragmentNames = fragmentNames.toList()
    )
}

private class Resolver(modules: Collection<KlibModuleMetadata>) {
    private val packageFqNames = hashSetOf<String>()
    private val typeAliases = hashMapOf<String, KmTypeAlias>()
    private val classes = hashMapOf<String, KmClass>()

    init {
        modules.forEach { module ->
            module.fragments.forEach fragment@{ fragment ->
                fragment.classes.forEach { clazz ->
                    classes[clazz.name] = clazz
                }

                val pkg = fragment.pkg ?: return@fragment
                val packageFqName = pkg.fqName?.replace('.', '/') ?: return@fragment
                packageFqNames += packageFqName

                pkg.typeAliases.forEach { typeAlias ->
                    val typeAliasFqName = if (packageFqName.isNotEmpty()) packageFqName + "/" + typeAlias.name else typeAlias.name
                    typeAliases[typeAliasFqName] = typeAlias
                }
            }
        }
    }

    fun getClass(classFqName: String): KmClass? = classes[classFqName]
    fun hasClass(classFqName: String): Boolean = getClass(classFqName) != null

    fun getTypeAlias(typeAliasFqName: String): KmTypeAlias? = typeAliases[typeAliasFqName]
    fun hasTypeAlias(typeAliasFqName: String): Boolean = getTypeAlias(typeAliasFqName) != null

    fun isDefinitelyNotResolvedTypeAlias(typeAliasFqName: String, classFqName: String): Boolean =
        when (val resolvedClassFqName = (getTypeAlias(typeAliasFqName)?.expandedType?.classifier as? KmClassifier.Class)?.name) {
            null -> {
                val typeAliasPackageFqName = typeAliasFqName.substringBeforeLast('/')
                typeAliasPackageFqName in packageFqNames
            }
            else -> resolvedClassFqName != classFqName
        }
}

// TODO: add filtering of mismatches consciously and ON DEMAND, don't use the filter that is used in commonizer tests!!!
private class MismatchesFilter(
    private val k1Resolver: Resolver,
    private val k2Resolver: Resolver,
) {
    fun filter(input: List<Mismatch>): List<Mismatch> {
        return input
            /* --- FILTER OUT: MISMATCHES THAT ARE OK --- */
            .filter {
                when {
                    it.isMissingEnumEntryInK2() -> false
                    it.isShortCircuitedTypeRecordedInK2TypeAliasUnderlyingType() -> false
                    it.isMoreExpandedTypeRecordedInK2TypeAliasUnderlyingType() -> false
                    it.isTypeAliasRecordedInK1BothAsClassAndTypeAlias() -> false
                    it.isValueParameterWithNonPropagatedDeclaresDefaultValueFlagInK1() -> false
                    it.isHasAnnotationsFlagHasDifferentStateInK1AndK2() -> false
                    it.isMissingAnnotationOnNonVisibleDeclaration() -> false
                    /* see KT-65383 */ it.isFalsePositiveIsOperatorFlagOnInvokeFunctionInK1() -> false
                    it.isNullableKotlinAnyUpperBoundInClassTypeParameterMissing() -> false
                    /* see KT-61138 */ it.isIgnoredHasConstantFlag() -> false
                    /* see KT-65664 */ it.isCompileTimeValueForConstantMissingInK1OrK2() -> false
                    /* see KT-65476 */ it.isOpenModalityOfEffectivelyPrivateKotlinxSerializationSynthesizedDeclarationInK1() -> false
                    /* see KT-65476 */ it.isDifferentStateOfIsNotDefaultFlagOnKotlinxSerializationSynthesizedDeclaration() -> false
                    /* see KT-65476 */ it.isMissingInternalSynthesizedConstructorForDeclarationThatIsMarkedAsSerializable() -> false
                    /* see KT-65631 */ it.isDifferentStateOfIsNotDefaultAndIsInlineFlagsOfInlinePropertyInConstructor() -> false
                    /* see KT-65575 */ it.isDifferentStateOfIsNotDefaultAndIsExternalFlagsOfExternalMemberProperty() -> false
                    it.isSpecialKotlinNativeAnnotationRecordedAsTypeAlias() -> false
//                    it.isSerializerMissHasAnnotation() -> false
//                    it.isMissedDeclaresDefaultValue() -> false
//                    it.isMissedParameterName() -> false
//                    it.isMissedDeprecated() -> false
//                    it.isMissedGetterAnnotationJsExportIgnore() -> false
                    else -> true
                }
            }
            .dropPairedMissingFunctionMismatchesDueToMissingNullableKotlinAnyUpperBound()

            /* --- FILTER OUT: MISMATCHES THAT ARE NOT OK --- */
            /* We know about them. But let's skip them all to make sure there is nothing new. */
            .filter {
                when {
                    /* already fixed in KT-64743 */ //it.isTypeAliasRecordedInK2TypeAsClass() -> false
                    /* TODO: KT-65263 (scheduled to 2.0.0-RC) */ it.isAbbreviatedTypeMissingInK1OrK2Type() -> false
                    /* TODO: KT-63871, KT-63852 (both scheduled to 2.1.0) */ it.isNotDefaultPropertyNotMarkedAsNotDefaultInK2() -> false
                    /* TODO: KT-64924 (scheduled to 2.1.0) */ it.isSerializerFunctionWithoutSynthesizedKindInK2() -> false
                    /* TODO: KT-55446 (scheduled to 2.0.0-Beta5) */ it.isPrivateToThisBecamePrivateInK2() -> false
                    /* TODO: KT-65757 (scheduled to 2.0.0-Beta5) */ it.isMissingDeprecatedAnnotationOnKotlinxSerializationSynthesizedDeclarationInK2() -> false
                    else -> true
                }
            }
            /* TODO: KT-65588 (scheduled to 2.0.0-Beta5) */
            .dropPairedFunctionsWithDifferentlyRecordedPrimitiveArraysInVarargParameterPositionInK1AndK2()
    }

    /* --- MISMATCHES THAT ARE OK --- */

    // enum entry classes are not serialized in K2
    private fun Mismatch.isMissingEnumEntryInK2(): Boolean =
        this is Mismatch.MissingEntity
                && kind == EntityKind.Class
                && missingInB
                && (existentValue as KmClass).kind == ClassKind.ENUM_ENTRY


    private fun Mismatch.isSerializerMissHasAnnotation(): Boolean =
        this is Mismatch.DifferentValues
                && kind == EntityKind.FlagKind.GETTER
                && name == "hasAnnotations"
                && valueA == true && valueB == false

    private fun Mismatch.isMissedDeclaresDefaultValue(): Boolean =
        this is Mismatch.DifferentValues
                && kind == EntityKind.FlagKind.REGULAR
                && name == "declaresDefaultValue"
                && valueA == true && valueB == false

    private fun Mismatch.isMissedParameterName(): Boolean =
        this is Mismatch.MissingEntity
                && kind == EntityKind.AnnotationKind.REGULAR
                && name == "kotlin/ParameterName"

    private fun Mismatch.isMissedDeprecated(): Boolean =
        this is Mismatch.MissingEntity
                && kind == EntityKind.AnnotationKind.REGULAR
                && name == "kotlin/Deprecated"
                && missingInA == true

    private fun Mismatch.isMissedGetterAnnotationJsExportIgnore(): Boolean =
        this is Mismatch.MissingEntity
                && kind == EntityKind.AnnotationKind.GETTER
                && name == "kotlin/js/JsExport.Ignore"
                && missingInB == true

    // That's OK. Types in underlying type of TA are written short-circuited. No harm at all.
    private fun Mismatch.isShortCircuitedTypeRecordedInK2TypeAliasUnderlyingType(): Boolean {
        when {
            !isRelatedToTypeAliasUnderlyingType() -> return false
            this is Mismatch.DifferentValues -> if (kind != EntityKind.Classifier) return false
            this is Mismatch.MissingEntity -> if (kind != EntityKind.TypeArgument) return false
            else -> return false
        }

        val lastTypePathElement = path.last() as? PathElement.Type
        val underlyingTypePathElement = path.filterIsInstanceWithChecker<PathElement.Type> { it.kind == TypeKind.UNDERLYING }.singleOrNull()

        for (pathElement in setOfNotNull(lastTypePathElement, underlyingTypePathElement)) {
            val typeK1 = pathElement.typeA
            val typeK2 = pathElement.typeB

            val typeK1TypeAlias = typeK1.classifier as? KmClassifier.TypeAlias
            val typeK1HasAbbreviation = typeK1.abbreviatedType != null

            val typeK2Class = typeK2.classifier as? KmClassifier.Class
            val typeK2Abbreviation = typeK2.abbreviatedType?.classifier as? KmClassifier.TypeAlias

            if (typeK1TypeAlias != null
                && !typeK1HasAbbreviation
                && typeK2Class != null
                && typeK2Abbreviation != null
                && typeK2Abbreviation.name == typeK1TypeAlias.name
            ) {
                return true
            }
        }

        return false
    }

    // That's OK. Types in underlying type of TA are written a bit expanded. No harm at all.
    private fun Mismatch.isMoreExpandedTypeRecordedInK2TypeAliasUnderlyingType(): Boolean {
        if (this is Mismatch.MissingEntity
            && (kind == TypeKind.ABBREVIATED || kind == EntityKind.TypeArgument)
        ) {
            val lastPathElement = path.last()

            if (lastPathElement is PathElement.Type && isRelatedToTypeAliasUnderlyingType()) {
                val typeK1 = lastPathElement.typeA
                val typeK2 = lastPathElement.typeB

                val typeK1TypeAlias = typeK1.classifier as? KmClassifier.TypeAlias
                val typeK1HasAbbreviation = typeK1.abbreviatedType != null

                val typeK2Class = typeK2.classifier as? KmClassifier.Class
                val typeK2Abbreviation = typeK2.abbreviatedType?.classifier as? KmClassifier.TypeAlias

                if (typeK1TypeAlias != null
                    && !typeK1HasAbbreviation
                    && typeK2Class != null
                    && typeK2Abbreviation != null
                    && typeK2Abbreviation.name == typeK1TypeAlias.name
                ) {
                    return true
                }
            }
        }

        return false
    }

    // This is OK since in K2 it works correctly.
    private fun Mismatch.isTypeAliasRecordedInK1BothAsClassAndTypeAlias(): Boolean {
        if (this is Mismatch.MissingEntity && kind == EntityKind.Class && missingInB) {
            val classThatIsMissingInK2 = existentValue as KmClass
            val classFqName = classThatIsMissingInK2.name

            val hasSuchClassInK1 = k1Resolver.hasClass(classFqName)
            val hasSuchClassInK2 = k2Resolver.hasClass(classFqName)

            val hasSuchTypeAliasInK1 = k1Resolver.hasTypeAlias(classFqName)
            val hasSuchTypeAliasInK2 = k2Resolver.hasTypeAlias(classFqName)

            if (hasSuchClassInK1 && !hasSuchClassInK2 && hasSuchTypeAliasInK1 && hasSuchTypeAliasInK2 && classThatIsMissingInK2.isExpect)
                return true
        }

        return false
    }

    // This is OK since in K2 it works correctly.
    private fun Mismatch.isValueParameterWithNonPropagatedDeclaresDefaultValueFlagInK1(): Boolean {
        if (this is Mismatch.DifferentValues
            && kind is FlagKind
            && name == "declaresDefaultValue"
            && path.last() is PathElement.ValueParameter
        ) {
            val declaresDefaultValueInK1 = valueA as Boolean
            val declaresDefaultValueInK2 = valueB as Boolean

            if (!declaresDefaultValueInK1 && declaresDefaultValueInK2)
                return true
        }

        return false
    }

    // This issue itself is not a problem. The real problem is that some annotations are missing,
    // and this is addressed by another check in 'MISMATCHES THAT ARE NOT OK' section.
    private fun Mismatch.isHasAnnotationsFlagHasDifferentStateInK1AndK2(): Boolean {
        if (this is Mismatch.DifferentValues && kind is FlagKind && name == "hasAnnotations") {
            val hasAnnotationsInK1 = valueA as Boolean
            val hasAnnotationsInK2 = valueB as Boolean

            val (nonEmptyAnnotationsInK1, nonEmptyAnnotationsInK2) = when (val lastPathElement = path.last()) {
                is PathElement.Property -> {
                    val annotationsInK1 = lastPathElement.propertyA.annotations
                    val annotationsInK2 = lastPathElement.propertyB.annotations

                    annotationsInK1.isNotEmpty() to annotationsInK2.isNotEmpty()
                }
                is PathElement.Class -> {
                    val annotationsInK1 = lastPathElement.clazzA.annotations
                    val annotationsInK2 = lastPathElement.clazzB.annotations

                    annotationsInK1.isNotEmpty() to annotationsInK2.isNotEmpty()
                }

                is PathElement.Constructor -> {
                    val annotationsInK1 = lastPathElement.constructorA.annotations
                    val annotationsInK2 = lastPathElement.constructorB.annotations

                    annotationsInK1.isNotEmpty() to annotationsInK2.isNotEmpty()
                }
                else -> error("Not yet supported: ${lastPathElement::class.java}")
            }

            if (hasAnnotationsInK1 == nonEmptyAnnotationsInK1 && hasAnnotationsInK2 == nonEmptyAnnotationsInK2)
                return true
        }

        return false
    }

    private fun Mismatch.isMissingAnnotationOnNonVisibleDeclaration(): Boolean {
        val annotationClassFqName = name
        if (annotationClassFqName == "kotlin/Deprecated") return false // This is a very strict exception!

        fun isVisibleClass(classFqName: String): Boolean? {
            return when (k2Resolver.getClass(classFqName)?.visibility) {
                null -> null
                Visibility.PUBLIC, Visibility.PROTECTED, Visibility.INTERNAL -> true
                else -> false
            }
        }

        if (this is Mismatch.MissingEntity && kind is AnnotationKind) {
            // If entity is invisible outside the module or the annotation is invisible outside the module,
            // treat this as a non-error situation.
            if (isDefinitelyUnderNonVisibleDeclarationInK2())
                return true

            // `null` means unknown visibility because the symbol is somewhere in another module.
            val isVisibleAnnotation: Boolean? = isVisibleClass(annotationClassFqName)
                ?: (k2Resolver.getTypeAlias(annotationClassFqName)?.expandedType?.classifier as? KmClassifier.Class)?.name
                    ?.let(::isVisibleClass)

            if (isVisibleAnnotation == false)
                return true
        }

        return false
    }

    private fun Mismatch.isNullableKotlinAnyUpperBoundInClassTypeParameterMissing(): Boolean {
        if (this is Mismatch.MissingEntity && kind == TypeKind.UPPER_BOUND) {
            val missingUpperBound = existentValue as KmType
            if ((missingUpperBound.classifier as? KmClassifier.Class)?.name == "kotlin/Any"
                && missingUpperBound.arguments.isEmpty()
                && missingUpperBound.isNullable
                && !missingUpperBound.isDefinitelyNonNull
                && missingUpperBound.abbreviatedType == null
                && missingUpperBound.flexibleTypeUpperBound == null
                && missingUpperBound.outerType == null
            ) {
                return true
            }
        }

        return false
    }

    private fun Mismatch.isIgnoredHasConstantFlag(): Boolean {
        return this is Mismatch.DifferentValues && kind is FlagKind && name == "hasConstant"
    }

    private fun Mismatch.isCompileTimeValueForConstantMissingInK1OrK2(): Boolean {
        if (this is Mismatch.MissingEntity && kind == EntityKind.CompileTimeValue) {
            if (missingInA) {
                when (existentValue as KmAnnotationArgument) {
                    is KmAnnotationArgument.ArrayValue, is KmAnnotationArgument.EnumValue -> return true
                    else -> Unit
                }
            } else {
                val lastPathElement = path.last() as? PathElement.Property
                if (lastPathElement != null) {
                    val isPropertyInK2Const = lastPathElement.propertyB.isConst

                    when (existentValue as KmAnnotationArgument) {
                        is KmAnnotationArgument.ArrayValue, is KmAnnotationArgument.EnumValue -> Unit
                        else -> if (!isPropertyInK2Const) return true
                    }
                }
            }
        }

        return false
    }

    private fun Mismatch.isOpenModalityOfEffectivelyPrivateKotlinxSerializationSynthesizedDeclarationInK1(): Boolean {
        if (this is Mismatch.DifferentValues
            && kind is FlagKind
            && name == "modality"
            && path.any { it is PathElement.Class && it.clazzB.name.endsWith(".\$serializer") }
        ) {
            val modalityInK1 = valueA as Modality
            val modalityInK2 = valueB as Modality

            if (modalityInK1 == Modality.OPEN && modalityInK2 == Modality.FINAL)
                return true
        }

        return false
    }

    private fun Mismatch.isDifferentStateOfIsNotDefaultFlagOnKotlinxSerializationSynthesizedDeclaration(): Boolean {
        if (this is Mismatch.DifferentValues
            && kind is FlagKind
            && name == "isNotDefault"
            && path.any { it is PathElement.Class && it.clazzB.name.endsWith(".\$serializer") }
        ) {
            val isNotDefaultInK1 = valueA as Boolean
            val isNotDefaultInK2 = valueB as Boolean

            if (isNotDefaultInK1 && !isNotDefaultInK2)
                return true
        }

        return false
    }

    private fun Mismatch.isMissingInternalSynthesizedConstructorForDeclarationThatIsMarkedAsSerializable(): Boolean {
        if (this is Mismatch.MissingEntity && kind == EntityKind.Constructor) {
            val existingConstructor = existentValue as KmConstructor
            val lastPathElement = path.last() as? PathElement.Class

            if (existingConstructor.visibility == Visibility.INTERNAL
                && existingConstructor.valueParameters.firstOrNull()?.name?.startsWith("seen") == true
                && lastPathElement != null
                && lastPathElement.clazzB.annotations.any { it.className == "kotlinx/serialization/Serializable" }
            ) {
                return true
            }
        }

        return false
    }

    private fun Mismatch.isDifferentStateOfIsNotDefaultAndIsInlineFlagsOfInlinePropertyInConstructor(): Boolean {
        if (this is Mismatch.DifferentValues
            && kind is FlagKind
            && (name == "isNotDefault" || name == "isInline")
        ) {
            val propertyInK2 = (path.last() as? PathElement.Property)?.propertyB
            val classInK2 = (path.getOrNull(path.lastIndex - 1) as? PathElement.Class)?.clazzB

            val flagValueInK1 = valueA as Boolean
            val flagValueInK2 = valueB as Boolean

            if (propertyInK2 != null
                && classInK2 != null
                && classInK2.isValue
                && classInK2.inlineClassUnderlyingPropertyName == propertyInK2.name
                && !flagValueInK1 && flagValueInK2
            ) {
                return true
            }
        }

        return false
    }

    private fun Mismatch.isDifferentStateOfIsNotDefaultAndIsExternalFlagsOfExternalMemberProperty(): Boolean {
        if (this is Mismatch.DifferentValues
            && (kind == FlagKind.GETTER || kind == FlagKind.SETTER)
            && (name == "isNotDefault" || name == "isExternal")
        ) {
            val lastPathElement = path.last() as? PathElement.Property
            if (lastPathElement != null) {
                val propertyInK1 = lastPathElement.propertyA
                val propertyInK2 = lastPathElement.propertyB

                val flagValueInK1 = valueA as Boolean
                val flagValueInK2 = valueB as Boolean

                if ((propertyInK1.isExternal || propertyInK2.isExternal) && !flagValueInK1 && flagValueInK2)
                    return true
            }
        }

        return false
    }

    private fun List<Mismatch>.dropPairedMissingFunctionMismatchesDueToMissingNullableKotlinAnyUpperBound(): List<Mismatch> {
        fun groupingKey(mismatch: Mismatch, presentOnlyInK1: Boolean): String? {
            if (mismatch !is Mismatch.MissingEntity || mismatch.kind != EntityKind.Function || presentOnlyInK1 == mismatch.missingInA) return null

            val functionKey = mismatch.toString().substringBefore(" is missing in ", missingDelimiterValue = "")
            if (functionKey.isEmpty()) return null

            return mismatch.path.filter { it !is PathElement.Root }.joinToString(" -> ", postfix = " -> $functionKey")
        }

        val functionsPresentOnlyInK1: Map<String, Mismatch> = mapNotNull { mismatch ->
            val key = groupingKey(mismatch, presentOnlyInK1 = true) ?: return@mapNotNull null
            key to mismatch
        }.toMap()

        val functionsPresentOnlyInK2: Map<String, Mismatch> = mapNotNull { mismatch ->
            val key = groupingKey(mismatch, presentOnlyInK1 = false) ?: return@mapNotNull null
            key to mismatch
        }.toMap()

        val functionsKeysInK2WithoutNullableAnyUppedBound = functionsPresentOnlyInK2.keys.associateWith { key ->
            key.replace(": kotlin/Any?,", ",").replace(": kotlin/Any?>", ">")
        }

        val mismatchesToRemove = hashSetOf<Mismatch>()
        functionsKeysInK2WithoutNullableAnyUppedBound.forEach { (keyForK2, keyForK1) ->
            val functionInK1 = functionsPresentOnlyInK1[keyForK1] ?: return@forEach
            val functionInK2 = functionsPresentOnlyInK2.getValue(keyForK2)

            // self annihilation:
            mismatchesToRemove += functionInK1
            mismatchesToRemove += functionInK2
        }

        return this - mismatchesToRemove
    }

    // 'isOperator' flag is not set for certain functions.
    private fun Mismatch.isFalsePositiveIsOperatorFlagOnInvokeFunctionInK1(): Boolean {
        if (this is Mismatch.DifferentValues && kind is FlagKind && name == "isOperator") {
            val lastPathElement = path.last() as? PathElement.Function
            if (lastPathElement?.functionA?.name == "invoke") {
                val isOperatorInK1 = valueA as Boolean
                val isOperatorInK2 = valueB as Boolean

                if (isOperatorInK1 && !isOperatorInK2)
                    return true
            }
        }

        return false
    }

    private fun Mismatch.isSpecialKotlinNativeAnnotationRecordedAsTypeAlias(): Boolean {
        if (this is Mismatch.MissingEntity && kind is AnnotationKind) {
            val missingAnnotationClassName = name

            val expectedAnnotationClassNameInK1: String
            val expectedAnnotationClassNameInK2: String

            if (missingInB) {
                expectedAnnotationClassNameInK1 = missingAnnotationClassName
                expectedAnnotationClassNameInK2 = missingAnnotationClassName.replace("kotlin/native/concurrent/", "kotlin/native/")
            } else {
                expectedAnnotationClassNameInK1 = missingAnnotationClassName.replace("kotlin/native/", "kotlin/native/concurrent/")
                expectedAnnotationClassNameInK2 = missingAnnotationClassName
            }

            val (annotationsInK1, annotationsInK2) = when (val lastPathElement = path.last()) {
                is PathElement.Property -> lastPathElement.propertyA.annotations to lastPathElement.propertyB.annotations
                is PathElement.Class -> lastPathElement.clazzA.annotations to lastPathElement.clazzB.annotations
                is PathElement.Type -> lastPathElement.typeA.annotations to lastPathElement.typeB.annotations
                is PathElement.Constructor -> lastPathElement.constructorA.annotations to lastPathElement.constructorB.annotations
                else -> error("Not yet supported: ${lastPathElement::class.java}")
            }

            if (annotationsInK1.any { it.className == expectedAnnotationClassNameInK1 }
                && annotationsInK1.none { it.className == expectedAnnotationClassNameInK2 }
                && annotationsInK2.any { it.className == expectedAnnotationClassNameInK2 }
                && annotationsInK2.none { it.className == expectedAnnotationClassNameInK1 }
            ) {
                return true
            }
        }

        return false
    }

    /* --- MISMATCHES THAT ARE NOT OK --- */

    @Suppress("unused")
    private fun Mismatch.isTypeAliasRecordedInK2TypeAsClass(): Boolean {
        if (this is Mismatch.DifferentValues && kind == EntityKind.Classifier) {
            val lastPathElement = path.last()

            if (lastPathElement is PathElement.Type
                && !isRelatedToTypeAliasUnderlyingType()
                && isTypeAliasRecordedInK2TypeAsClass(typeK1 = lastPathElement.typeA, typeK2 = lastPathElement.typeB)
            ) {
                return true
            }
        }

        if (this is Mismatch.MissingEntity && kind == TypeKind.ABBREVIATED) {
            val lastPathElement = path.last()

            if (lastPathElement is PathElement.Type
                && !isRelatedToTypeAliasUnderlyingType()
                && isTypeAliasRecordedInK2TypeAsClass(typeK1 = lastPathElement.typeA, typeK2 = lastPathElement.typeB)
            ) {
                return true
            }
        }

        if (this is Mismatch.MissingEntity && kind == EntityKind.Function) {
            fun KmFunction.allTypes(): List<KmType> = buildList {
                valueParameters.mapTo(this) { it.type }
                addIfNotNull(receiverParameterType)
            }

            if (missingInA) {
                // Some function is missing in K1. Probably, that's because one of the function's
                // value parameters has TA recorded as Class.
                val functionInK2 = existentValue as KmFunction
                functionInK2.allTypes().forEach { type ->
                    if (type.abbreviatedType != null) return@forEach
                    val typeAliasFqName = (type.classifier as? KmClassifier.Class)?.name ?: return@forEach
                    if (k2Resolver.hasTypeAlias(typeAliasFqName))
                        return true
                }
            } else {
                // Some function is missing in K2. Probably, that's because one of the function's
                // value parameters has properly recorded TA in its type.
                val functionInK1 = existentValue as KmFunction
                functionInK1.allTypes().forEach { type ->
                    val classFqName = (type.classifier as? KmClassifier.Class)?.name ?: return@forEach
                    val typeAliasFqName = (type.abbreviatedType?.classifier as? KmClassifier.TypeAlias)?.name ?: return@forEach
                    if (!k1Resolver.isDefinitelyNotResolvedTypeAlias(typeAliasFqName, classFqName))
                        return true
                }
            }
        }

        return false
    }

    // Invalid type: type alias is recorded as KmClassifier.Class in metadata!
    private fun isTypeAliasRecordedInK2TypeAsClass(typeK1: KmType, typeK2: KmType): Boolean {
        val typeK1Class = typeK1.classifier as? KmClassifier.Class
        val typeK1Abbreviation = typeK1.abbreviatedType?.classifier as? KmClassifier.TypeAlias

        val typeK2Class = typeK2.classifier as? KmClassifier.Class
        val typeK2HasAbbreviation = typeK2.abbreviatedType != null

        @Suppress("RedundantIf", "RedundantSuppression")
        if (typeK1Class != null
            && typeK1Abbreviation != null
            && typeK2Class != null
            && !typeK2HasAbbreviation
            && typeK1Abbreviation.name == typeK2Class.name
            && !k1Resolver.isDefinitelyNotResolvedTypeAlias(typeK1Abbreviation.name, typeK1Class.name)
        ) {
            return true
        }

        return false
    }

    // Abbreviated type may be absent at all. No harm at all.
    private fun Mismatch.isAbbreviatedTypeMissingInK1OrK2Type(): Boolean {
        if (this is Mismatch.MissingEntity && kind == TypeKind.ABBREVIATED) {
            val lastPathElement = path.last()

            if (lastPathElement is PathElement.Type && !isRelatedToTypeAliasUnderlyingType()) {
                val typeK1 = lastPathElement.typeA
                val typeK2 = lastPathElement.typeB

                val typeK1Class = typeK1.classifier as? KmClassifier.Class
                val typeK1HasAbbreviation = typeK1.abbreviatedType != null

                val typeK2Class = typeK2.classifier as? KmClassifier.Class
                val typeK2HasAbbreviation = typeK2.abbreviatedType != null

                if (typeK1Class != null
                    && typeK2Class != null
                    && typeK1Class.name == typeK2Class.name
                    && typeK1HasAbbreviation != typeK2HasAbbreviation
                ) {
                    return true
                }
            }
        }

        return false
    }

    // 'isNotDefault' flag is not set for certain properties.
    private fun Mismatch.isNotDefaultPropertyNotMarkedAsNotDefaultInK2(): Boolean {
        if (this is Mismatch.DifferentValues
            && kind is FlagKind
            && name == "isNotDefault"
        ) {
            val lastPathElement = path.last() as? PathElement.Property
            if (lastPathElement != null) {
                val propertyInK2 = lastPathElement.propertyB

                // This is not a 100% correct check for non-defaultness, but a good approximation:
                // If a property has `kind == DELEGATION` then it is a fake override of a property declared in an interface
                // that appeared in the current class through interface delegation. Such property in interface could only have
                // accessor body (never a backing field).
                val isLikelyNotDefault =
                    propertyInK2.kind == MemberKind.DELEGATION // inheritance by delegation
                            || propertyInK2.getterAnnotations.isNotEmpty() // has annotations

                val isNotDefaultInK1 = valueA as Boolean
                val isNotDefaultInK2 = valueB as Boolean

                if (isNotDefaultInK1 != isNotDefaultInK2 && isLikelyNotDefault)
                    return true
            }
        }

        return false
    }

    private fun Mismatch.isSerializerFunctionWithoutSynthesizedKindInK2(): Boolean {
        if (this is Mismatch.DifferentValues && kind == FlagKind.REGULAR && name == "kind") {
            val k1Kind = valueA as MemberKind
            val k2Kind = valueB as MemberKind

            if (k1Kind == MemberKind.SYNTHESIZED && k2Kind == MemberKind.DECLARATION) {
                return true
            }
        }

        return false
    }

    private fun Mismatch.isPrivateToThisBecamePrivateInK2(): Boolean {
        if (this is Mismatch.DifferentValues && kind is FlagKind && name == "visibility") {
            val visibilityInK1 = valueA as Visibility
            val visibilityInK2 = valueB as Visibility
            if (visibilityInK1 == Visibility.PRIVATE_TO_THIS && visibilityInK2 == Visibility.PRIVATE)
                return true
        }

        return false
    }

    private fun Mismatch.isMissingDeprecatedAnnotationOnKotlinxSerializationSynthesizedDeclarationInK2(): Boolean {
        return this is Mismatch.MissingEntity
                && kind is AnnotationKind
                && missingInB
                && name == "kotlin/Deprecated"
                && (path.last() as? PathElement.Class)?.clazzB?.name?.endsWith(".\$serializer") == true
    }

    private fun List<Mismatch>.dropPairedFunctionsWithDifferentlyRecordedPrimitiveArraysInVarargParameterPositionInK1AndK2(): List<Mismatch> {
        data class ProblematicCase(val properArrayType: String, val invalidArrayType: String)
        class Payload(var missingInK1: Mismatch.MissingEntity? = null, var missingInK2: Mismatch.MissingEntity? = null)

        val problematicCases = hashMapOf<ProblematicCase, Payload>()

        for (mismatch in this) {
            if (mismatch is Mismatch.MissingEntity && mismatch.kind == EntityKind.Function) {
                val existingFunction = mismatch.existentValue as KmFunction

                val lastValueParameter = existingFunction.valueParameters.lastOrNull()
                val varargTypeClassName = (lastValueParameter?.varargElementType?.classifier as? KmClassifier.Class)?.name

                if (varargTypeClassName != null) {
                    if (varargTypeClassName.startsWith("kotlin/") && varargTypeClassName.count { it == '/' } == 1) {
                        val case = ProblematicCase(
                            properArrayType = "${varargTypeClassName}Array",
                            invalidArrayType = "kotlin/Array<out $varargTypeClassName>"
                        )

                        val payload = problematicCases.computeIfAbsent(case) { Payload() }
                        if (mismatch.missingInA)
                            payload.missingInK1 = mismatch
                        else
                            payload.missingInK2 = mismatch
                    }
                }
            }
        }

        val mismatchesToRemove = problematicCases.entries.mapNotNull { (_, payload) ->
            val missingInK1 = payload.missingInK1 ?: return@mapNotNull null
            val missingInK2 = payload.missingInK2 ?: return@mapNotNull null
            listOf(missingInK1, missingInK2)
        }.flattenTo(hashSetOf())

        return this - mismatchesToRemove
    }

    /* --- UTILS --- */

    private fun Mismatch.isRelatedToTypeAliasUnderlyingType(): Boolean =
        path.any { it is PathElement.Type && it.kind == TypeKind.UNDERLYING }

    private fun Mismatch.isDefinitelyUnderNonVisibleDeclarationInK2(): Boolean {
        for (pathElement in path) {
            val visibility = when (pathElement) {
                is PathElement.Class -> pathElement.clazzB.visibility
                is PathElement.Constructor -> pathElement.constructorB.visibility
                is PathElement.Function -> pathElement.functionB.visibility
                is PathElement.Property -> pathElement.propertyB.visibility
                else -> continue
            }

            if (visibility != Visibility.PUBLIC && visibility != Visibility.PROTECTED && visibility != Visibility.INTERNAL)
                return true
        }

        return false
    }
}
