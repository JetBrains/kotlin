import org.jetbrains.kotlin.gradle.dsl.KotlinCompile

plugins {
    kotlin("multiplatform")
}

kotlin {
    js(IR) {
        nodejs()
    }
}
val commonMainFullSources by task<Sync> {
    dependsOn(":prepare:build.version:writeStdlibVersion")

    val sources = listOf(
        "libraries/stdlib/common/src/",
        "libraries/stdlib/src/kotlin/",
        "libraries/stdlib/unsigned/",
        "core/builtins/src/kotlin/internal/",
    )

    sources.forEach { path ->
        from("$rootDir/$path") {
            into(path.dropLastWhile { it != '/' })
        }
    }

    into("$buildDir/commonMainFullSources")
}

val commonMainSources by task<Sync> {
    dependsOn(commonMainFullSources)
    from {
        exclude(
            listOf(
                "libraries/stdlib/unsigned/src/kotlin/UByteArray.kt",
                "libraries/stdlib/unsigned/src/kotlin/UIntArray.kt",
                "libraries/stdlib/unsigned/src/kotlin/ULongArray.kt",
                "libraries/stdlib/unsigned/src/kotlin/UMath.kt",
                "libraries/stdlib/unsigned/src/kotlin/UNumbers.kt",
                "libraries/stdlib/unsigned/src/kotlin/UShortArray.kt",
                "libraries/stdlib/unsigned/src/kotlin/UStrings.kt",
                "libraries/stdlib/common/src/generated/_Arrays.kt",
                "libraries/stdlib/common/src/generated/_Collections.kt",
                "libraries/stdlib/common/src/generated/_Comparisons.kt",
                "libraries/stdlib/common/src/generated/_Maps.kt",
                "libraries/stdlib/common/src/generated/_OneToManyTitlecaseMappings.kt",
                "libraries/stdlib/common/src/generated/_Sequences.kt",
                "libraries/stdlib/common/src/generated/_Sets.kt",
                "libraries/stdlib/common/src/generated/_Strings.kt",
                "libraries/stdlib/common/src/generated/_UArrays.kt",
                "libraries/stdlib/common/src/generated/_URanges.kt",
                "libraries/stdlib/common/src/generated/_UCollections.kt",
                "libraries/stdlib/common/src/generated/_UComparisons.kt",
                "libraries/stdlib/common/src/generated/_USequences.kt",
                "libraries/stdlib/common/src/kotlin/SequencesH.kt",
                "libraries/stdlib/common/src/kotlin/TextH.kt",
                "libraries/stdlib/common/src/kotlin/UMath.kt",
                "libraries/stdlib/common/src/kotlin/collections/**",
                "libraries/stdlib/common/src/kotlin/ioH.kt",
                "libraries/stdlib/src/kotlin/collections/**",
                "libraries/stdlib/src/kotlin/io/**",
                "libraries/stdlib/src/kotlin/properties/Delegates.kt",
                "libraries/stdlib/src/kotlin/random/URandom.kt",
                "libraries/stdlib/src/kotlin/text/**",
                "libraries/stdlib/src/kotlin/time/**",
                "libraries/stdlib/src/kotlin/util/KotlinVersion.kt",
                "libraries/stdlib/src/kotlin/util/Tuples.kt",
                "libraries/stdlib/src/kotlin/enums/**"
            )
        )
        commonMainFullSources.get().outputs.files.singleFile
    }

    into("$buildDir/commonMainSources")
}

val commonMainCollectionSources by task<Sync> {
    dependsOn(commonMainFullSources)
    from {
        include("libraries/stdlib/src/kotlin/collections/PrimitiveIterators.kt")
        commonMainFullSources.get().outputs.files.singleFile
    }

    into("$buildDir/commonMainCollectionSources")
}

val jsMainSources by task<Sync> {
    dependsOn(":kotlin-stdlib:prepareJsIrMainSources")

    from {
        val fullJsMainSources = tasks.getByPath(":kotlin-stdlib:prepareJsIrMainSources")
        exclude(
            listOf(
                "libraries/stdlib/js/src/org.w3c/**",
                "libraries/stdlib/js/src/kotlin/char.kt",
                "libraries/stdlib/js/src/kotlin/collectionJs.kt",
                "libraries/stdlib/js/src/kotlin/collections/**",
                "libraries/stdlib/js/src/kotlin/time/**",
                "libraries/stdlib/js/src/kotlin/console.kt",
                "libraries/stdlib/js/src/kotlin/coreDeprecated.kt",
                "libraries/stdlib/js/src/kotlin/date.kt",
                "libraries/stdlib/js/src/kotlin/GroupingJs.kt",
                "libraries/stdlib/js/src/kotlin/ItemArrayLike.kt",
                "libraries/stdlib/js/src/kotlin/io/**",
                "libraries/stdlib/js/src/kotlin/json.kt",
                "libraries/stdlib/js/src/kotlin/promise.kt",
                "libraries/stdlib/js/src/kotlin/regexp.kt",
                "libraries/stdlib/js/src/kotlin/sequenceJs.kt",
                "libraries/stdlib/js/src/kotlin/throwableExtensions.kt",
                "libraries/stdlib/js/src/kotlin/text/**",
                "libraries/stdlib/js/src/kotlin/reflect/KTypeHelpers.kt",
                "libraries/stdlib/js/src/kotlin/reflect/KTypeParameterImpl.kt",
                "libraries/stdlib/js/src/kotlin/reflect/KTypeImpl.kt",
                "libraries/stdlib/js/src/kotlin/dom/**",
                "libraries/stdlib/js/src/kotlin/browser/**",
                "libraries/stdlib/js/src/kotlinx/dom/**",
                "libraries/stdlib/js/src/kotlinx/browser/**",
                "libraries/stdlib/js/src/kotlin/enums/**"
            )
        )
        fullJsMainSources.outputs.files.singleFile
    }

    for (jsIrSrcDir in listOf("builtins", "runtime", "src")) {
        from("$rootDir/libraries/stdlib/js-ir/$jsIrSrcDir") {
            exclude(
                listOf(
                    "collectionsHacks.kt",
                    "generated/**",
                    "kotlin/text/**"
                )
            )
            into("libraries/stdlib/js-ir/$jsIrSrcDir")
        }
    }

    from("$rootDir/libraries/stdlib/js-ir-minimal-for-test/src")
    into("$buildDir/jsMainSources")
}

kotlin {
    sourceSets {
        named("commonMain") {
            kotlin.srcDir(files(commonMainSources.map { it.destinationDir }))
            kotlin.srcDir(files(commonMainCollectionSources.map { it.destinationDir }))
        }
        named("jsMain") {
            kotlin.srcDir(files(jsMainSources.map { it.destinationDir }))
        }
    }
}

tasks.withType<KotlinCompile<*>> {
    kotlinOptions.freeCompilerArgs += listOf(
        "-Xallow-kotlin-package",
        "-opt-in=kotlin.ExperimentalMultiplatform",
        "-opt-in=kotlin.contracts.ExperimentalContracts",
        "-opt-in=kotlin.RequiresOptIn",
        "-opt-in=kotlin.ExperimentalUnsignedTypes",
        "-opt-in=kotlin.ExperimentalStdlibApi",
    )
}

tasks {
    compileKotlinMetadata {
        enabled = false
    }

    named("compileKotlinJs", KotlinCompile::class) {
        kotlinOptions.freeCompilerArgs += "-Xir-module-name=kotlin"
    }
}
