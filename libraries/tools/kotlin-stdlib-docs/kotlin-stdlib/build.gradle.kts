import org.gradle.kotlin.dsl.base
import org.gradle.kotlin.dsl.invoke
import org.gradle.kotlin.dsl.provideDelegate
import org.jetbrains.dokka.gradle.engine.parameters.DokkaPackageOptionsSpec
import org.jetbrains.dokka.gradle.engine.parameters.DokkaSourceSetSpec
import org.jetbrains.dokka.gradle.engine.parameters.KotlinPlatform
import org.jetbrains.dokka.gradle.engine.parameters.VisibilityModifier

plugins {
    base
    `dokka-convention`
}

val kotlin_root = rootProject.file("../../../").absoluteFile.invariantSeparatorsPath
val kotlin_libs: String by project


val outputDir = file(findProperty("docsBuildDir") as String? ?: "${layout.buildDirectory}/doc")
val inputDirPrevious = file(findProperty("docsPreviousVersionsDir") as String? ?: "$outputDir/previous")
val outputDirPartial = outputDir.resolve("partial")
val kotlin_native_root = file("$kotlin_root/kotlin-native").absolutePath
val kotlinTemplatesDir = (findProperty("templatesDir") as String?)?.let { file(it) } ?: rootProject.file("templates")

val isLatest = (findProperty("isLatest") as String?)?.toBoolean() ?: true

dokka {
    val kotlin_stdlib_dir = file("$kotlin_root/libraries/stdlib")

    val stdlibIncludeMd = file("$kotlin_root/libraries/stdlib/src/Module.md")
    val stdlibSamples = file("$kotlin_root/libraries/stdlib/samples/test")

    val suppressedPackages = listOf(
        "kotlin.internal",
        "kotlin.jvm.internal",
        "kotlin.js.internal",
        "kotlin.native.internal",
        "kotlin.jvm.functions",
        "kotlin.coroutines.jvm.internal",
        "kotlin.wasm.internal",
    )

    val kotlinLanguageVersion = version as String

    pluginsConfiguration {
        versioning {
            version.set(kotlinLanguageVersion)
            if (isLatest) {
                olderVersionsDir.set(projectDir.resolve("dokka-docs"))
            }
        }
        register<VersionFilterPluginParameters>("VersionFilterPlugin") {
            targetVersion = kotlinLanguageVersion
        }
    }

    dokkaPublications.html {
        val moduleDirName = "kotlin-stdlib"
        if (isLatest) {
            outputDirectory.set(file("$outputDirPartial/latest").resolve(moduleDirName))
        } else {
            outputDirectory.set(
                file("$outputDirPartial/previous").resolve(moduleDirName).resolve(kotlinLanguageVersion)
            )
        }
    }

    dokkaSourceSets {
        val common = register("common") {
            jdkVersion.set(8)
            analysisPlatform.set(KotlinPlatform.Common)
            enableJdkDocumentationLink.set(false)

            displayName.set("Common")

            sourceRoots.from("$kotlin_stdlib_dir/common/src")
            sourceRoots.from("$kotlin_stdlib_dir/src")
            sourceRoots.from("$kotlin_stdlib_dir/unsigned/src")
        }

        register("jvm") {
            jdkVersion.set(8)
            analysisPlatform.set(KotlinPlatform.JVM)

            displayName.set("JVM")
            dependentSourceSets.add(common.get().sourceSetId.get())

            sourceRoots.from("$kotlin_stdlib_dir/jvm/src")

            sourceRoots.from("$kotlin_stdlib_dir/jvm/builtins")

            sourceRoots.from("$kotlin_stdlib_dir/jvm/runtime/kotlin/jvm/annotations")
            sourceRoots.from("$kotlin_stdlib_dir/jvm/runtime/kotlin/jvm/JvmClassMapping.kt")
            sourceRoots.from("$kotlin_stdlib_dir/jvm/runtime/kotlin/jvm/PurelyImplements.kt")
            sourceRoots.from("$kotlin_stdlib_dir/jvm/runtime/kotlin/Metadata.kt")
            sourceRoots.from("$kotlin_stdlib_dir/jvm/runtime/kotlin/Throws.kt")
            sourceRoots.from("$kotlin_stdlib_dir/jvm/runtime/kotlin/TypeAliases.kt")
            sourceRoots.from("$kotlin_stdlib_dir/jvm/runtime/kotlin/text/TypeAliases.kt")
            sourceRoots.from("$kotlin_stdlib_dir/jdk7/src")
            sourceRoots.from("$kotlin_stdlib_dir/jdk8/src")
        }
        register("js") {
            jdkVersion.set(8)
            analysisPlatform.set(KotlinPlatform.JS)

            enableJdkDocumentationLink.set(false)

            displayName.set("JS")
            dependentSourceSets.add(common.get().sourceSetId.get())

            sourceRoots.from("$kotlin_stdlib_dir/js/src/generated")
            sourceRoots.from("$kotlin_stdlib_dir/js/src/kotlin")

            sourceRoots.from("$kotlin_stdlib_dir/js/builtins")
            // We don't generate docs for the intermediate webMain source set, so to make
            // regular declarations from it visible, they are explicitly included in js and wasm-js source sets.
            sourceRoots.from("$kotlin_stdlib_dir/common-js-wasmjs/src/kotlin/JsInterop.kt")
            sourceRoots.from("$kotlin_stdlib_dir/common-js-wasmjs/src/kotlin/js/ExperimentalWasmJsInterop.kt")
            // We don't generate docs for the intermediate commonNonJvm source set, add them to the platform docs
            sourceRoots.from("$kotlin_stdlib_dir/common-non-jvm/src/kotlin/coroutines/cancellation/CancellationException.kt")
            sourceRoots.from("$kotlin_stdlib_dir/common-non-jvm/src/kotlin/reflect/AssociatedObjects.kt")

            // builtin sources that are copied from common builtins during JS stdlib build
            listOf(
                "Annotation.kt",
                "Any.kt",
                "CharSequence.kt",
                "Comparable.kt",
                "Iterator.kt",
                "Nothing.kt",
                "Number.kt",
            ).forEach { sourceRoots.from("$kotlin_stdlib_dir/jvm/builtins/$it") }

            perPackageOption("kotlin.browser") {
                suppress.set(true)
            }
            perPackageOption("kotlin.dom") {
                suppress.set(true)
            }
        }
        register("native") {
            jdkVersion.set(8)
            analysisPlatform.set(KotlinPlatform.Native)
            enableJdkDocumentationLink.set(false)

            displayName.set("Native")
            dependentSourceSets.add(common.get().sourceSetId.get())

            sourceRoots.from("$kotlin_native_root/Interop/Runtime/src/main/kotlin")
            sourceRoots.from("$kotlin_native_root/Interop/Runtime/src/native/kotlin")
            sourceRoots.from("$kotlin_native_root/runtime/src/main/kotlin")
            sourceRoots.from("$kotlin_stdlib_dir/native-wasm/src")
            sourceRoots.from("$kotlin_stdlib_dir/native-wasm/wasi")
            // We don't generate docs for the intermediate commonNonJvm source set, add them to the platform docs
            sourceRoots.from("$kotlin_stdlib_dir/common-non-jvm/src/kotlin/coroutines/cancellation/CancellationException.kt")
            sourceRoots.from("$kotlin_stdlib_dir/common-non-jvm/src/kotlin/reflect/AssociatedObjects.kt")
            perPackageOption("kotlin.test") {
                suppress.set(true)
            }
        }
        register("wasm-js") {
            analysisPlatform.set(KotlinPlatform.Wasm)
            enableJdkDocumentationLink.set(false)

            displayName.set("Wasm-JS")
            dependentSourceSets.add(common.get().sourceSetId.get())
            sourceRoots.from("$kotlin_stdlib_dir/native-wasm/src")
            sourceRoots.from("$kotlin_stdlib_dir/wasm/src")
            sourceRoots.from("$kotlin_stdlib_dir/wasm/builtins")
            sourceRoots.from("$kotlin_stdlib_dir/wasm/internal")
            sourceRoots.from("$kotlin_stdlib_dir/wasm/stubs")
            sourceRoots.from("$kotlin_stdlib_dir/wasm/js/builtins")
            sourceRoots.from("$kotlin_stdlib_dir/wasm/js/internal")
            sourceRoots.from("$kotlin_stdlib_dir/wasm/js/src")
            // We don't generate docs for the intermediate webMain source set, so to make
            // regular declarations from it visible, they are explicitly included in js and wasm-js source sets.
            sourceRoots.from("$kotlin_stdlib_dir/common-js-wasmjs/src/kotlin/JsInterop.kt")
            sourceRoots.from("$kotlin_stdlib_dir/common-js-wasmjs/src/kotlin/js/ExperimentalWasmJsInterop.kt")
            // We don't generate docs for the intermediate commonNonJvm source set, add them to the platform docs
            sourceRoots.from("$kotlin_stdlib_dir/common-non-jvm/src/kotlin/coroutines/cancellation/CancellationException.kt")
            sourceRoots.from("$kotlin_stdlib_dir/common-non-jvm/src/kotlin/reflect/AssociatedObjects.kt")

            // builtin sources that are copied from common builtins during Wasm stdlib build
            listOf(
                "Annotation.kt",
                "CharSequence.kt",
                "Comparable.kt",
                "Number.kt",
            ).forEach { sourceRoots.from("$kotlin_stdlib_dir/jvm/builtins/$it") }
        }
        register("wasm-wasi") {
            analysisPlatform.set(KotlinPlatform.Wasm)
            enableJdkDocumentationLink.set(false)

            displayName.set("Wasm-WASI")
            dependentSourceSets.add(common.get().sourceSetId.get())
            sourceRoots.from("$kotlin_stdlib_dir/native-wasm/src")
            sourceRoots.from("$kotlin_stdlib_dir/native-wasm/wasi")
            sourceRoots.from("$kotlin_stdlib_dir/wasm/src")
            sourceRoots.from("$kotlin_stdlib_dir/wasm/builtins")
            sourceRoots.from("$kotlin_stdlib_dir/wasm/internal")
            sourceRoots.from("$kotlin_stdlib_dir/wasm/stubs")
            sourceRoots.from("$kotlin_stdlib_dir/wasm/wasi/builtins")
            sourceRoots.from("$kotlin_stdlib_dir/wasm/wasi/src")
            // We don't generate docs for the intermediate commonNonJvm source set, add them to the platform docs
            sourceRoots.from("$kotlin_stdlib_dir/common-non-jvm/src/kotlin/coroutines/cancellation/CancellationException.kt")
            sourceRoots.from("$kotlin_stdlib_dir/common-non-jvm/src/kotlin/reflect/AssociatedObjects.kt")

            // builtin sources that are copied from common builtins during Wasm stdlib build
            listOf(
                "Annotation.kt",
                "CharSequence.kt",
                "Comparable.kt",
                "Number.kt",
            ).forEach { sourceRoots.from("$kotlin_stdlib_dir/jvm/builtins/$it") }
        }
        configureEach {
            documentedVisibilities.set(
                setOf(
                    VisibilityModifier.Public,
                    VisibilityModifier.Protected
                )
            )
            skipDeprecated.set(false)
            includes.from(stdlibIncludeMd)
            enableKotlinStdLibDocumentationLink.set(false)
            languageVersion.set(kotlinLanguageVersion)
            samples.from(stdlibSamples)
            suppressedPackages.forEach { packageName ->
                perPackageOption(packageName) {
                    suppress.set(true)
                }
            }
            sourceLinksFromRoot(this)
        }

    }
    fixIntersectedSourceRootsAndSamples(dokkaSourceSets, "stdlib")
}

fun DokkaSourceSetSpec.perPackageOption(packageNamePrefix: String, action: Action<in DokkaPackageOptionsSpec>) =
    perPackageOption {
        matchingRegex.set(Regex.escape(packageNamePrefix) + "(\$|\\..*)")
        action(this)
    }