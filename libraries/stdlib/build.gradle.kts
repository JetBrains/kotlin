import java.nio.file.*
import org.gradle.kotlin.dsl.support.serviceOf
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsTargetDsl
import plugins.configureDefaultPublishing

plugins {
    id("kotlin-multiplatform")
    `maven-publish`
    signing
}


// TODO: all targets
//   - jar manifests

// TODO: JVM
//   - jar module info

// TODO: JS
//   - ensure dist content
//   - ensure npm publishing

// TODO: deprecate these
//jvmTarget = "1.6"
//javaHome = rootProject.extra["JDK_16"] as String

configureJvmToolchain(JdkMajorVersion.JDK_1_6)

val configurationBuiltins = configurations.create("builtins") {
    attributes.attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, project.objects.named(LibraryElements.JAR))
}
dependencies {
    configurationBuiltins(project(":core:builtins"))
}

val builtinsDir = "${rootDir}/core/builtins"
val builtinsSrcDir = "${buildDir}/src/builtin-sources"
val builtinsRuntimeSrcDir = "${buildDir}/src/builtin-sources-for-runtime"

val jsCommonDir = "${projectDir}/../js"
val jsCommonSrcDir = "${jsCommonDir}/src"
val jsV1Dir = "${projectDir}/../js-v1"
val jsSrcDir = "$jsV1Dir/src"
val jsSrcJsDir = "${jsSrcDir}/js"

// for js-ir
val jsIrDir = "${projectDir}/../js-ir"
val jsIrMainSources = "${buildDir}/src/jsMainSources"
lateinit var jsIrTarget: KotlinJsTargetDsl

kotlin {
    metadata {
        compilations {
            all {
                kotlinOptions.freeCompilerArgs += listOf(
                    "-Xallow-kotlin-package",
                )
            }
        }

//        mavenPublication {
//            artifactId = "kotlin-stdlib-mpp-common" // ?
//        }
    }
    jvm {
        withJava()
        compilations {
            val main by getting {
                kotlinOptions {
                    moduleName = "kotlin-stdlib"
                    // providing exhaustive list of args here
                    freeCompilerArgs = listOf(
                        "-Xallow-kotlin-package",
                        "-Xmultifile-parts-inherit",
                        "-Xnormalize-constructor-calls=enable",
                        "-Xuse-14-inline-classes-mangling-scheme",
                        "-Xsuppress-deprecated-jvm-target-warning",
                    )
                }
            }
        }
    }
    js(LEGACY) {
        browser {
        }
        nodejs {
        }
        compilations {
            all {
                kotlinOptions {
                    main = "noCall"
                    moduleKind = "commonjs"
                    freeCompilerArgs += listOf(
                        "-Xallow-kotlin-package"
                    )
                }
            }
            val main by getting
            main.apply {
                kotlinOptions {
                    outputFile = "${buildDir}/classes/js-v1/main/kotlin.js"
                    sourceMap = true
                }
            }
            val runtime by creating {
                kotlinOptions {
                    metaInfo = false
                    outputFile = "${buildDir}/classes/js-v1/runtime/kotlin.js"
                    sourceMap = true
                }
            }
        }
    }
    jsIrTarget = js("jsIr", IR) {
        nodejs {
            testTask {
                useMocha {
                    timeout = "10s"
                }
            }
        }
        compilations {
            all {
                kotlinOptions {
                    freeCompilerArgs += "-Xallow-kotlin-package"
                }
            }
            val main by getting
            main.apply {
                kotlinOptions {
                    freeCompilerArgs += "-Xir-module-name=kotlin"

                    if (!kotlinBuildProperties.disableWerror) {
                        allWarningsAsErrors = true
                    }
                }
            }
        }
    }

    sourceSets {
        commonMain {
            kotlin.apply {
                srcDir("../common/src")
                srcDir("../src")
                srcDir("../unsigned/src")
            }
        }
        val jvmMain by getting {
            project.configurations.getByName("jvmMainCompileOnly").extendsFrom(configurationBuiltins)
            dependencies {
                api("org.jetbrains:annotations:13.0")
            }
            val jvmSrcDirs = arrayOf(
                "../jvm/src",
                "../jvm/runtime",
                "$builtinsDir/src"
            )
//            kotlin.srcDirs(*jvmSrcDirs)
            project.sourceSets["main"].java.srcDirs(*jvmSrcDirs)
        }

        val jsRuntime by getting {
            kotlin.apply {
                srcDir(builtinsRuntimeSrcDir)
                srcDir("$jsCommonDir/runtime")
                srcDir("$jsV1Dir/runtime")
            }
            val prepareComparableSources by tasks.registering(Copy::class) {
                val fs = serviceOf<FileSystemOperations>()
                doFirst {
                    fs.delete { delete(builtinsRuntimeSrcDir) }
                }
                from("${builtinsDir}/native/kotlin") {
                    include("Comparable.kt")
                }
                into(builtinsRuntimeSrcDir)
            }
            js().compilations["runtime"].compileKotlinTaskProvider.configure {
                dependsOn(prepareComparableSources)
            }
        }

        val jsMain by getting {
            kotlin.apply {
                srcDir(builtinsSrcDir)
                srcDir(jsCommonSrcDir)
                srcDir(jsSrcDir)
            }

            val prepareBuiltinsSources by tasks.registering(Copy::class) {
                val fs = serviceOf<FileSystemOperations>()
                doFirst {
                    fs.delete { delete(builtinsSrcDir) }
                }
                from("${builtinsDir}/native/kotlin") {
                    include("Iterator.kt")
                    include("Collections.kt")
                    include("CharSequence.kt")
                    include("Annotation.kt")
                }
                from("${builtinsDir}/src/kotlin/") {
                    include("annotation/Annotations.kt")
                    include("Function.kt")
                    include("Iterators.kt")
                    include("Range.kt")
                    include("Progressions.kt")
                    include("ProgressionIterators.kt")
                    include("Ranges.kt")
                    include("internal/InternalAnnotations.kt")
                    include("internal/progressionUtil.kt")
                    include("Unit.kt")
                }
                into(builtinsSrcDir)
            }
            js().compilations["main"].compileKotlinTaskProvider.configure {
                dependsOn(prepareBuiltinsSources)
            }
        }

        val jsIrMain by getting {
            kotlin.apply {
                srcDir(jsIrMainSources)
                srcDir("$jsIrDir/builtins")
                srcDir("$jsIrDir/runtime")
                srcDir("$jsIrDir/src")
            }

            val prepareJsIrMainSources by tasks.registering(Sync::class) {
                val unimplementedNativeBuiltIns =
                    (file("$builtinsDir/native/kotlin/").list()!!.toSortedSet() - file("$jsIrDir/builtins/").list()!!)
                        .map { "core/builtins/native/kotlin/$it" }

                // TODO: try to reuse absolute paths defined in the beginning
                val sources = listOf(
                    "core/builtins/src/kotlin/",
                    "libraries/stdlib/js/src/",
                    "libraries/stdlib/js/runtime/"
                ) + unimplementedNativeBuiltIns

                val excluded = listOf(
                    // stdlib/js/src/generated is used exclusively for current `js-v1` backend.
                    "libraries/stdlib/js/src/generated/**",

                    // JS-specific optimized version of emptyArray() already defined
                    "core/builtins/src/kotlin/ArrayIntrinsics.kt"
                )

                sources.forEach { path ->
                    from("$rootDir/$path") {
                        into(path.dropLastWhile { it != '/' })
                        excluded.filter { it.startsWith(path) }.forEach {
                            exclude(it.substring(path.length))
                        }
                    }
                }

                into(jsIrMainSources)

// Required to compile native builtins with the rest of runtime
                val builtInsHeader = """@file:Suppress(
    "NON_ABSTRACT_FUNCTION_WITH_NO_BODY",
    "MUST_BE_INITIALIZED_OR_BE_ABSTRACT",
    "EXTERNAL_TYPE_EXTENDS_NON_EXTERNAL_TYPE",
    "PRIMARY_CONSTRUCTOR_DELEGATION_CALL_EXPECTED",
    "WRONG_MODIFIER_TARGET",
    "UNUSED_PARAMETER"
)
"""
                doLast {
                    unimplementedNativeBuiltIns.forEach { path ->
                        val file = File("$jsIrMainSources/$path")
                        val sourceCode = builtInsHeader + file.readText()
                        file.writeText(sourceCode)
                    }
                }
            }
            jsIrTarget.compilations["main"].compileKotlinTaskProvider.configure {
                dependsOn(prepareJsIrMainSources)
            }
        }

        all {
            languageSettings {
                //            progressiveMode = true
                if (this@all != jsRuntime) {
                    optIn("kotlin.RequiresOptIn")
                    optIn("kotlin.ExperimentalMultiplatform")
                    optIn("kotlin.contracts.ExperimentalContracts")
                }
            }
        }
    }
}

tasks {
    val jvmJar by existing(Jar::class) {
        dependsOn(configurationBuiltins)
        from { zipTree(configurationBuiltins.singleFile) }
    }

    val jvmSourcesJar by existing(org.gradle.jvm.tasks.Jar::class) {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }


    val jsOutputFileName = "${buildDir}/classes/js-v1/kotlin.js"
    val jsOutputMapFileName = "${jsOutputFileName}.map"
    val jsOutputMetaFileName = "${buildDir}/classes/js-v1/kotlin.meta.js"

    val mergeJs by registering(NoDebugJavaExec::class) {
        val compileKotlinJs by getting(org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile::class)
        val compileRuntimeKotlinJs by getting(org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile::class)
        dependsOn(compileRuntimeKotlinJs, compileKotlinJs)
        val compileRuntimeFiles = compileRuntimeKotlinJs.outputs.files
        val compileMainFiles = compileKotlinJs.outputs.files
        inputs.files(compileRuntimeFiles).withPathSensitivity(PathSensitivity.RELATIVE)
        inputs.files(compileMainFiles).withPathSensitivity(PathSensitivity.RELATIVE)
        inputs.dir(jsSrcDir).withPathSensitivity(PathSensitivity.RELATIVE)
        inputs.dir(jsCommonSrcDir).withPathSensitivity(PathSensitivity.RELATIVE)

        outputs.file(jsOutputFileName)
        outputs.file(jsOutputMapFileName)
        outputs.file(jsOutputMetaFileName)
        outputs.cacheIf { true }

        val inputFiles = fileTree(jsSrcJsDir) {
            include("**/*.js")
        }

        mainClass.set("org.jetbrains.kotlin.cli.js.internal.JSStdlibLinker")

        // local variables for configuration cache work
        val rootDir = rootDir

        doFirst {
            args = listOf(jsOutputFileName, "$rootDir", "$jsSrcDir/wrapper.js") + inputFiles.map { it.path }.sorted() +
                    (compileRuntimeFiles.map { it.path }.sorted() +
                            compileMainFiles.map { it.path }.sorted()).filter {
                        it.endsWith(".js") && !it.endsWith(".meta.js")
                    }
        }
        classpath = configurations["kotlinCompilerClasspath"]

        val sourceMapFile = file(jsOutputMapFileName)
        val jsOutputMetaFile = file(jsOutputMetaFileName)
        val compileMetaFile = file(compileKotlinJs.outputFileProperty.get().path.replace(Regex("\\.js$"), ".meta.js"))
        val mainJsOutputDir = compileKotlinJs.destinationDirectory
        doLast {
            fun AntBuilder.replaceRegexp(file: String, match: String, replace: String) {
                withGroovyBuilder {
                    "replaceregexp"(
                        "file" to file,
                        "match" to match,
                        "replace" to replace,
                        "byline" to "true",
                        "encoding" to "UTF-8"
                    )
                }
            }
            ant.replaceRegexp(jsOutputFileName,  "module.exports,\\s*require\\([^)]+\\)", "")
            ant.replaceRegexp(jsOutputFileName, "function\\s*\\(_,\\s*Kotlin\\)", "function()")
            ant.replaceRegexp(jsOutputFileName, "return\\s+_;", "")

            val sourceMap = groovy.json.JsonSlurper().parse(sourceMapFile, "UTF-8")

            val sourceMapSourcesBaseDirs = listOf(mainJsOutputDir.get(), "${jsCommonDir}/runtime", jsV1Dir, rootDir).map { File(it.toString()) }

            sourceMap.withGroovyBuilder {
                @Suppress("UNCHECKED_CAST")
                val sourcePaths = getProperty("sources") as List<String>

                setProperty("sourcesContent", sourcePaths.map { sourcePath ->
                    sourceMapSourcesBaseDirs
                        .map { it.resolve(sourcePath) }
                        .firstOrNull { it.exists() }
                        ?.readText().also {
                            if (it == null) logger.warn("Sources missing for file $sourcePath")
                        }
                })

                val sourceMapBasePaths = listOf(
                    "../../../../",
                    "../../../",
                    "../../",
                    "./",
                    "libraries/stdlib/js-v1/src/"
                )
                val shortPaths = sourcePaths.map { sourcePath ->
                    val prefixToRemove = sourceMapBasePaths.find { basePath -> sourcePath.startsWith(basePath) }
                    if (prefixToRemove != null) sourcePath.substring(prefixToRemove.length) else sourcePath
                }
                if (shortPaths.size != shortPaths.distinct().size) {
                    logger.warn("Duplicate source file names found:\n${sourcePaths.sorted().joinToString("\n")}")
                }
                setProperty("sources", shortPaths)
            }

            sourceMapFile.writeText(groovy.json.JsonOutput.toJson(sourceMap))
            compileMetaFile.copyTo(jsOutputMetaFile, overwrite = true)
        }
    }

    val jsMainClasses by existing {
        dependsOn(mergeJs)
    }

    val jsResultingJar by registering(Jar::class) {
        archiveClassifier.set("js-v1")
        archiveVersion.set("")
        destinationDirectory.set(file("$buildDir/lib"))

        includeEmptyDirs = false
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        from(jsOutputFileName)
        from(jsOutputMetaFileName)
        from(jsOutputMapFileName)
        from(kotlin.js().compilations["main"].output.allOutputs)
        from(jsIrTarget.compilations["main"].output.allOutputs)
        filesMatching("*.*") { mode = 0b110100100 } // KTI-401
    }

    val jsJar by existing(Jar::class) {
        val jsResultingJarFile = jsResultingJar.get().archiveFile
        inputs.file(jsResultingJarFile)
        doLast {
            Files.copy(jsResultingJarFile.get().asFile.toPath(), archiveFile.get().asFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }
}

configureDefaultPublishing()