import org.gradle.api.internal.component.SoftwareComponentInternal
import org.gradle.api.internal.component.UsageContext
import java.nio.file.*
import org.gradle.kotlin.dsl.support.serviceOf
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsTargetDsl
import org.jetbrains.kotlin.gradle.tasks.UsesKotlinJavaToolchain
import plugins.configureDefaultPublishing
import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import plugins.configureKotlinPomAttributes

plugins {
    id("kotlin-multiplatform")
    `maven-publish`
    signing
}


// TODO: JS
//   - ensure dist content
//   - ensure npm publishing

configureJvmToolchain(JdkMajorVersion.JDK_1_8)

val configurationBuiltins = configurations.create("builtins") {
    attributes.attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, project.objects.named(LibraryElements.JAR))
}
dependencies {
    configurationBuiltins(project(":core:builtins"))
}

val builtinsDir = "${rootDir}/core/builtins"
val builtinsSrcDir = "${buildDir}/src/builtin-sources"
val builtinsRuntimeSrcDir = "${buildDir}/src/builtin-sources-for-runtime"

val jsCommonDir = "${projectDir}/js"
val jsCommonSrcDir = "${jsCommonDir}/src"
val jsCommonTestSrcDir = "${jsCommonDir}/test"
val jsV1Dir = "${projectDir}/js-v1"
val jsSrcDir = "$jsV1Dir/src"
val jsSrcJsDir = "${jsSrcDir}/js"
val prepareJsV1ComparableSources by tasks.registering(Sync::class)
val prepareJsV1BuiltinsSources by tasks.registering(Sync::class)

// for js-ir
val jsIrDir = "${projectDir}/js-ir"
val jsIrMainSources = "${buildDir}/src/jsMainSources"
val jsIrTestSources = "${buildDir}/src/jsTestSources"
val prepareJsIrMainSources by tasks.registering(Sync::class)
lateinit var jsIrTarget: KotlinJsTargetDsl
lateinit var jsV1Target: KotlinJsTargetDsl

val prepareCommonSources by tasks.registering {
    dependsOn(":prepare:build.version:writeStdlibVersion")
}
val prepareAllSources by tasks.registering {
    dependsOn(prepareCommonSources)
    dependsOn(prepareJsIrMainSources, prepareJsV1ComparableSources, prepareJsV1BuiltinsSources)
}

kotlin {
    metadata {
        compilations {
            all {
                compileTaskProvider.configure {
                    kotlinOptions {
                        freeCompilerArgs = listOf(
                            "-Xallow-kotlin-package",
                            "-module-name", "kotlin-stdlib-common"
                        )
                    }
                }
            }
        }

//        mavenPublication {
//            artifactId = "kotlin-stdlib-mpp-common" // ?
//        }
    }
    jvm {
        withJava()
        compilations {
            val compileOnlyDeclarations by creating {
                compileTaskProvider.configure {
                    kotlinOptions {
                        freeCompilerArgs = listOf("-Xallow-kotlin-package")
                    }
                }
            }

            val main by getting {
                compileTaskProvider.configure {
                    this as UsesKotlinJavaToolchain
                    kotlinJavaToolchain.toolchain.use(getToolchainLauncherFor(JdkMajorVersion.JDK_1_6))
                    kotlinOptions {
                        moduleName = "kotlin-stdlib"
                        jvmTarget = "1.8"
                        // providing exhaustive list of args here
                        freeCompilerArgs = listOf(
                            "-Xallow-kotlin-package",
                            "-Xmultifile-parts-inherit",
                            "-Xuse-14-inline-classes-mangling-scheme",
                            "-Xbuiltins-from-sources",
                            "-Xno-new-java-annotation-targets",
                            "-Xuse-old-innerclasses-logic",
                        )
                    }
                }
                defaultSourceSet {
                    dependencies {
                        compileOnly(compileOnlyDeclarations.output.allOutputs)
                    }
                }
            }
            val mainJdk7 by creating {
                compileTaskProvider.configure {
                    this as UsesKotlinJavaToolchain
                    kotlinJavaToolchain.toolchain.use(getToolchainLauncherFor(JdkMajorVersion.JDK_1_7))
                    kotlinOptions {
                        moduleName = "kotlin-stdlib-jdk7"
                        jvmTarget = "1.8"
                        freeCompilerArgs = listOf(
                            "-Xallow-kotlin-package",
                            "-Xmultifile-parts-inherit",
                            "-Xno-new-java-annotation-targets"
                        )
                    }
                }
                defaultSourceSet {
                    dependencies {
                        // TODO: is not imported correctly
                        implementation(main.output.classesDirs)
                    }
                    configurations.compileOnlyConfiguration.extendsFrom(main.configurations.compileDependencyConfiguration)
                }
            }
            val mainJdk8 by creating {
                compileTaskProvider.configure {
                    kotlinOptions {
                        moduleName = "kotlin-stdlib-jdk8"
                        freeCompilerArgs = listOf(
                            "-Xallow-kotlin-package",
                            "-Xmultifile-parts-inherit",
                            "-Xno-new-java-annotation-targets"
                        )
                    }
                }
                defaultSourceSet {
                    dependencies {
                        implementation(main.output.allOutputs)
                        implementation(mainJdk7.output.allOutputs)
                    }
                    configurations.compileOnlyConfiguration.extendsFrom(main.configurations.compileDependencyConfiguration)
                }
            }
            project.sourceSets.create("java9") {
                java.srcDir("jvm/java9")
            }
            configureJava9Compilation("kotlin.stdlib", listOf(
                main.output.allOutputs,
                mainJdk7.output.allOutputs,
                mainJdk8.output.allOutputs,
            ))
            val test by getting {
                compileTaskProvider.configure {
                    kotlinOptions {
                        freeCompilerArgs += listOf(
                            "-Xallow-kotlin-package", // TODO: maybe rename test packages
                        )
                        if (kotlinBuildProperties.useFir) {
                            freeCompilerArgs += "-Xuse-k2"
                        }
                        // This is needed for JavaTypeTest; typeOf for non-reified type parameters doesn't work otherwise, for implementation reasons.
                        freeCompilerArgs -= "-Xno-optimized-callable-references"
                    }
                }
                defaultSourceSet {
                    dependencies {
                        compileOnly(mainJdk7.output.classesDirs)
                        compileOnly(mainJdk8.output.classesDirs)
                    }
                }
            }
            project.sourceSets.create("longRunningTest")
            val longRunningTest by getting {
                defaultSourceSet {
                    dependencies {
                        implementation(main.output.allOutputs)
                    }
                }
            }
        }
    }
    jsV1Target = js("jsV1", LEGACY) {
        browser {
        }
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
                    main = "noCall"
                    moduleKind = "commonjs"
                    freeCompilerArgs += listOf(
                        "-Xallow-kotlin-package"
                    )
                }
                compileTaskProvider.configure {
                    // proper caching
                    // source map paths are sensitive to relative source location
                    inputs.property("relativeSrcPath", file(jsV1Dir).relativeTo(projectDir).invariantSeparatorsPath)
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
            val test by getting; test.apply {
                kotlinOptions {
                    moduleKind = "umd"
                }
            }
        }
    }
    jsIrTarget = js(IR) {
        browser {}
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

    targets.all {
        compilations.all {
            if (name == "main" || name == "commonMain") {
                compileTaskProvider.configure {
                    dependsOn(prepareCommonSources)
                }
            }
        }
    }

    sourceSets {
        commonMain {
            kotlin {
                srcDir("common/src")
                srcDir("src")
                srcDir("unsigned/src")
                if (!kotlinBuildProperties.isInIdeaSync) {
                    srcDir("$rootDir/core/builtins/src/kotlin/internal")
                }
            }
        }
        commonTest {
            dependencies {
                // TODO: use project dependency when kotlin-test is migrated
                compileOnly("org.jetbrains.kotlin:kotlin-test-common:$bootstrapKotlinVersion")
                compileOnly("org.jetbrains.kotlin:kotlin-test-annotations-common:$bootstrapKotlinVersion")
//                compileOnly(project(":kotlin-test:kotlin-test-common"))
//                compileOnly(project(":kotlin-test:kotlin-test-annotations-common"))
            }
            kotlin {
                srcDir("common/test")
                srcDir("test")
            }
        }
        val jvmCompileOnlyDeclarations by getting {
            project.configurations.getByName("jvmMainCompileOnly").extendsFrom(configurationBuiltins)
            project.sourceSets["compileOnlyDeclarations"].java.srcDirs(
                "jvm/compileOnly"
            )
        }
        val jvmMain by getting {
            dependencies {
                api("org.jetbrains:annotations:13.0")
            }
            val jvmSrcDirs = arrayOf(
                "jvm/src",
                "jvm/runtime",
                "$builtinsDir/src"
            )
            project.sourceSets["main"].java.srcDirs(*jvmSrcDirs)
        }

        val jvmMainJdk7 by getting {
            kotlin.srcDir("jdk7/src")
        }
        val jvmMainJdk8 by getting {
            kotlin.srcDir("jdk8/src")
        }

        val jvmTest by getting {
            languageSettings {
                optIn("kotlin.io.path.ExperimentalPathApi")
            }
            dependencies {
                api(project(":kotlin-test:kotlin-test-junit"))
            }
            kotlin.srcDir("jvm/test")
            kotlin.srcDir("jdk7/test")
            kotlin.srcDir("jdk8/test")
        }

        val jvmLongRunningTest by getting {
            dependencies {
                api(project(":kotlin-test:kotlin-test-junit"))
            }
            kotlin.srcDir("jvm/testLongRunning")
        }

        val jsV1Runtime by getting {
            kotlin {
                srcDir(builtinsRuntimeSrcDir)
                srcDir("$jsCommonDir/runtime")
                srcDir("$jsV1Dir/runtime")
            }
            prepareJsV1ComparableSources.configure {
                from("${builtinsDir}/native/kotlin") {
                    include("Comparable.kt")
                }
                into(builtinsRuntimeSrcDir)
            }
            jsV1Target.compilations["runtime"].compileTaskProvider.configure {
                dependsOn(prepareJsV1ComparableSources)
            }
        }

        val jsV1Main by getting {
            kotlin {
                srcDir(builtinsSrcDir)
                srcDir(jsCommonSrcDir)
                srcDir(jsSrcDir)
            }

            prepareJsV1BuiltinsSources.configure {
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
                    include("Unit.kt")
                }
                into(builtinsSrcDir)
            }
            jsV1Target.compilations["main"].compileTaskProvider.configure {
                dependsOn(prepareJsV1BuiltinsSources)
            }
        }
        val jsV1Test by getting {
            dependencies {
                api(project(":kotlin-test:kotlin-test-js-v1"))
            }
            kotlin {
                srcDir(jsCommonTestSrcDir)
                srcDir("$jsV1Dir/test")
            }
        }

        val jsMain by getting {
            kotlin {
                srcDir(jsIrMainSources)
                srcDir("$jsIrDir/builtins")
                srcDir("$jsIrDir/runtime")
                srcDir("$jsIrDir/src")
            }

            prepareJsIrMainSources.configure {
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
                    "libraries/stdlib/js/src/kotlin/browser",
                    "libraries/stdlib/js/src/kotlin/dom",
                    "libraries/stdlib/js/src/org.w3c",
                    "libraries/stdlib/js/src/kotlinx",

                    // JS-specific optimized version of emptyArray() already defined
                    "core/builtins/src/kotlin/ArrayIntrinsics.kt",
                    // included in common
                    "core/builtins/src/kotlin/internal/**",
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
                val jsIrMainSources = jsIrMainSources // for f-ing conf-cache
                doLast {
                    unimplementedNativeBuiltIns.forEach { path ->
                        val file = File("$jsIrMainSources/$path")
                        val sourceCode = builtInsHeader + file.readText()
                        file.writeText(sourceCode)
                    }
                }
            }
            jsIrTarget.compilations["main"].compileTaskProvider.configure {
                dependsOn(prepareJsIrMainSources)
            }
        }
        val jsTest by getting {
            kotlin.srcDir(jsIrTestSources)
            dependencies {
                api(project(":kotlin-test:kotlin-test-js-ir"))
            }
            val prepareJsIrTestSources by tasks.registering(Sync::class) {
                from(jsCommonTestSrcDir)
                into(jsIrTestSources)
            }
            jsIrTarget.compilations["test"].compileTaskProvider.configure {
                dependsOn(prepareJsIrTestSources)
            }
        }

        all sourceSet@ {
            languageSettings {
                //            progressiveMode = true
                if (this@sourceSet == jvmCompileOnlyDeclarations) {
                    return@languageSettings
                }
                if (this@sourceSet != jsV1Runtime) {
                    optIn("kotlin.ExperimentalMultiplatform")
                    optIn("kotlin.contracts.ExperimentalContracts")
                }
                if (this@sourceSet.name.endsWith("Test")) {
                    optIn("kotlin.ExperimentalUnsignedTypes")
                    optIn("kotlin.ExperimentalStdlibApi")
                    optIn("kotlin.io.encoding.ExperimentalEncodingApi")
                }
            }
        }
    }
}

tasks {
    val jvmJar by existing(Jar::class) {
        dependsOn(configurationBuiltins)
        duplicatesStrategy = DuplicatesStrategy.FAIL
        callGroovy("manifestAttributes", manifest, project, "Main", true)
        manifest.attributes(mapOf("Implementation-Title" to "kotlin-stdlib"))
        from { zipTree(configurationBuiltins.singleFile) }
        from(kotlin.jvm().compilations["mainJdk7"].output.allOutputs)
        from(kotlin.jvm().compilations["mainJdk8"].output.allOutputs)
        from(project.sourceSets["java9"].output)
    }

    val jvmSourcesJar by existing(org.gradle.jvm.tasks.Jar::class) {
        dependsOn(prepareCommonSources)
        duplicatesStrategy = DuplicatesStrategy.FAIL
        into("jvmMain") {
            from("${rootDir}/core/builtins/native")
            from(kotlin.sourceSets["jvmMainJdk7"].kotlin) {
                into("jdk7")
            }
            from(kotlin.sourceSets["jvmMainJdk8"].kotlin) {
                into("jdk8")
            }
        }
    }

    val jsOutputFileName = "${buildDir}/classes/js-v1/kotlin.js"
    val jsOutputMapFileName = "${jsOutputFileName}.map"
    val jsOutputMetaFileName = "${buildDir}/classes/js-v1/kotlin.meta.js"

    val mergeJsV1 by registering(NoDebugJavaExec::class) {
        val compileKotlinJsV1 by getting(org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile::class)
        val compileRuntimeKotlinJsV1 by getting(org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile::class)
        dependsOn(compileRuntimeKotlinJsV1, compileKotlinJsV1)
        val compileRuntimeFiles = compileRuntimeKotlinJsV1.destinationDirectory.asFileTree
        val compileMainFiles = compileKotlinJsV1.destinationDirectory.asFileTree
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
        val jsSrcDir = jsSrcDir
        val jsOutputFileName = jsOutputFileName

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
        val compileMetaFile = file(compileKotlinJsV1.outputFileProperty.get().path.replace(Regex("\\.js$"), ".meta.js"))
        val mainJsOutputDir = compileKotlinJsV1.destinationDirectory
        val sourceMapSourcesBaseDirs = listOf(mainJsOutputDir.get(), "${jsCommonDir}/runtime", jsV1Dir, rootDir).map { File(it.toString()) }
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

    val jsV1MainClasses by existing {
        dependsOn(mergeJsV1)
    }

    jsV1Target.compilations["test"].compileTaskProvider.configure {
        val fs = serviceOf<FileSystemOperations>()
        val jsOutputFileName = jsOutputFileName
        doLast {
            // copy freshly-built legacy kotlin.js into node_modules subdir of kotlin-stdlib-js-v1-test module
            fs.copy {
                from(jsOutputFileName)
                into(destinationDirectory.dir("node_modules"))
            }
        }
    }

    val jsResultingJar by registering(Jar::class) {
        archiveClassifier.set("js")
        archiveVersion.set("")
        destinationDirectory.set(file("$buildDir/lib"))

        includeEmptyDirs = false
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        callGroovy("manifestAttributes", manifest, project, "Main")
        manifest.attributes(mapOf("Implementation-Title" to "kotlin-stdlib-js"))
        from(jsOutputFileName)
        from(jsOutputMetaFileName)
        from(jsOutputMapFileName)
        from(jsV1Target.compilations["main"].output.allOutputs)
        from(jsIrTarget.compilations["main"].output.allOutputs)
        filesMatching("*.*") { mode = 0b110100100 } // KTI-401
    }

    val jsJar by existing(Jar::class) {
        callGroovy("manifestAttributes", manifest, project, "Main")
        manifest.attributes(mapOf("Implementation-Title" to "kotlin-stdlib-js"))
    }

    val jsV1Jar by existing(Jar::class) {
        val jsResultingJarFile = jsResultingJar.get().archiveFile
        inputs.file(jsResultingJarFile)
        doLast {
            Files.copy(jsResultingJarFile.get().asFile.toPath(), archiveFile.get().asFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }

    val jsRearrangedSourcesJar by registering(Jar::class) {
        archiveClassifier.set("js-sources")
        archiveVersion.set("")
        destinationDirectory.set(file("$buildDir/lib"))

        includeEmptyDirs = false
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        dependsOn(prepareAllSources)
        val jsMainSourcesDir = prepareJsIrMainSources.get().destinationDir

        into("commonMain") {
            from(kotlin.sourceSets.commonMain.get().kotlin)
        }
        into("jsMain") {
            from("${rootDir}/core/builtins/native/kotlin") {
                into("kotlin")
                include("Comparable.kt")
                include("Enum.kt")
            }
            from("$jsMainSourcesDir/core/builtins/native") {
                exclude("kotlin/Comparable.kt")
            }
            from("$jsMainSourcesDir/core/builtins/src")
            from("$jsMainSourcesDir/libraries/stdlib/js/src")
            from("$jsIrDir/builtins") {
                into("kotlin")
                exclude("Enum.kt")
            }
            from("$jsIrDir/runtime") {
                into("runtime")
            }
            from("$jsIrDir/src") {
                include("**/*.kt")
            }
        }
    }

    val jsSourcesJar by existing(Jar::class) {
        val jsSourcesJarFile = jsRearrangedSourcesJar.get().archiveFile
        inputs.file(jsSourcesJarFile)
        doLast {
            Files.copy(jsSourcesJarFile.get().asFile.toPath(), archiveFile.get().asFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }


    listOf(JdkMajorVersion.JDK_9_0, JdkMajorVersion.JDK_10_0, JdkMajorVersion.JDK_11_0).forEach { jvmVersion ->
        val jvmVersionTest = register("jvm${jvmVersion.majorVersion}Test", Test::class) {
            group = "verification"
            javaLauncher.set(getToolchainLauncherFor(jvmVersion))
        }
        check.configure { dependsOn(jvmVersionTest) }
    }

    val jvmLongRunningTest by registering(Test::class) {
        val compilation = kotlin.jvm().compilations["longRunningTest"]
        classpath = compilation.compileDependencyFiles + compilation.runtimeDependencyFiles + compilation.output.allOutputs
        testClassesDirs = compilation.output.classesDirs
    }

    if (project.hasProperty("kotlin.stdlib.test.long.running")) {
        check.configure { dependsOn(jvmLongRunningTest) }
    }
}


// region ==== Publishing ====

configureDefaultPublishing()

val defaultKotlinPublications = listOf("kotlinMultiplatform", "jvm", "js", "jsV1")
afterEvaluate {
    tasks.withType<AbstractPublishToMaven>().all {
        if (publication.name in defaultKotlinPublications) {
            enabled = false
        }
//        println("$name - ${publication.name} - enabled: $enabled")
    }
}

open class ComponentsFactoryAccess
@javax.inject.Inject
constructor(val factory: SoftwareComponentFactory)

val componentFactory = objects.newInstance<ComponentsFactoryAccess>().factory

val emptyJavadocJar by tasks.creating(org.gradle.api.tasks.bundling.Jar::class) {
    archiveClassifier.set("javadoc")
}

publishing {
    configureMultiModuleMavenPublishing {
        val rootModule = module("rootModule") {
            mavenPublication {
                artifactId = "kotlin-stdlib-mpp"
                configureKotlinPomAttributes(project, "Kotlin Standard Library")
            }

            // creates a variant from existing configuration or creates new one
            variant("jvmApiElements")
            variant("jvmRuntimeElements")
            variant("jvmSourcesElements")

            variant("metadataApiElements")
            variant("metadataSourcesElementsFromJvm") {
                name = "metadataSourcesElements"
                attributes {
                    copyAttributes(from = project.configurations["metadataSourcesElements"].attributes, to = this)
                }
                artifact(tasks["jvmSourcesJar"])
            }
            variant("nativeApiElements") {
                attributes {
                    attribute(Usage.USAGE_ATTRIBUTE, objects.named("kotlin-api"))
                    attribute(KotlinPlatformType.attribute, KotlinPlatformType.native)
                }
            }
        }

        // TODO: decide what should be published in kotlin-stdlib-common
//        val common = module("commonModule") {
//            mavenPublication {
//                groupId = "org.example"
//                artifactId = "sample-lib-common"
//            }
//            variant("commonMainMetadataElements") {
//                // Multiplatform KGP already added klib artifact to metadataApiElements
//                // attributes { kotlinLegacyMetadataAttributes() }
//            }
//        }
        val js = module("jsModule") {
            mavenPublication {
                artifactId = "kotlin-stdlib-mpp-js"
                configureKotlinPomAttributes(project, "Kotlin Standard Library for JS")
            }
            variant("jsApiElements")
            variant("jsRuntimeElements")
            variant("jsV1ApiElements")
            variant("jsV1RuntimeElements")
            variant("jsSourcesElements") {
                // TODO: Remove org.jetbrains.kotlin.js.compiler attribute?
            }
        }

        // Makes all variants from common and js be visible through `available-at`
        rootModule.include(js)
    }

    publications {
        withType<MavenPublication> {
            artifact(emptyJavadocJar)
        }
    }
}

fun copyAttributes(from: AttributeContainer, to: AttributeContainer,) {
    // capture type argument T
    fun <T : Any> copyOneAttribute(from: AttributeContainer, to: AttributeContainer, key: Attribute<T>) {
        val value = checkNotNull(from.getAttribute(key))
        to.attribute(key, value)
    }
    for (key in from.keySet()) {
        copyOneAttribute(from, to, key)
    }
}

class MultiModuleMavenPublishingConfiguration() {
    val modules = mutableMapOf<String, Module>()

    class Module(val name: String) {
        val variants = mutableMapOf<String, Variant>()
        val includes = mutableSetOf<Module>()

        class Variant(
            val configurationName: String
        ) {
            var name: String = configurationName
            val attributesConfigurations = mutableListOf<AttributeContainer.() -> Unit>()
            fun attributes(code: AttributeContainer.() -> Unit) {
                attributesConfigurations += code
            }

            val artifactsWithConfigurations = mutableListOf<Pair<Any, ConfigurablePublishArtifact.() -> Unit>>()
            fun artifact(file: Any, code: ConfigurablePublishArtifact.() -> Unit = {}) {
                artifactsWithConfigurations += file to code
            }

            val configurationConfigurations = mutableListOf<Configuration.() -> Unit>()
            fun configuration(code: Configuration.() -> Unit) {
                configurationConfigurations += code
            }

            val variantDetailsConfigurations = mutableListOf<ConfigurationVariantDetails.() -> Unit>()
            fun configureVariantDetails(code: ConfigurationVariantDetails.() -> Unit) {
                variantDetailsConfigurations += code
            }
        }

        val mavenPublicationConfigurations = mutableListOf<MavenPublication.() -> Unit>()
        fun mavenPublication(code: MavenPublication.() -> Unit) {
            mavenPublicationConfigurations += code
        }

        fun variant(fromConfigurationName: String, code: Variant.() -> Unit = {}): Variant {
            val variant = variants.getOrPut(fromConfigurationName) { Variant(fromConfigurationName) }
            variant.code()
            return variant
        }

        fun include(vararg modules: Module) {
            includes.addAll(modules)
        }
    }

    fun module(name: String, code: Module.() -> Unit): Module {
        val module = modules.getOrPut(name) { Module(name) }
        module.code()
        return module
    }
}

fun configureMultiModuleMavenPublishing(code: MultiModuleMavenPublishingConfiguration.() -> Unit) {
    val publishingConfiguration = MultiModuleMavenPublishingConfiguration()
    publishingConfiguration.code()

    val components = publishingConfiguration
        .modules
        .mapValues { (_, module) -> project.createModulePublication(module) }

    val componentsWithExternals = publishingConfiguration
        .modules
        .filter { (_, module) -> module.includes.isNotEmpty() }
        .mapValues { (moduleName, module) ->
            val mainComponent = components[moduleName] ?: error("Component with name $moduleName wasn't created")
            val externalComponents = module.includes
                .map { components[it.name] ?: error("Component with name ${it.name} wasn't created") }
                .toSet()
            ComponentWithExternalVariants(mainComponent, externalComponents)
        }

    // override some components wih items from componentsWithExternals
    val mergedComponents = components + componentsWithExternals

    val publicationsContainer = publishing.publications
    for ((componentName, component) in mergedComponents) {
        publicationsContainer.create<MavenPublication>(componentName) {
            from(component)
            val module = publishingConfiguration.modules[componentName]!!
            module.mavenPublicationConfigurations.forEach { configure -> configure() }
        }
    }
}


fun Project.createModulePublication(module: MultiModuleMavenPublishingConfiguration.Module): SoftwareComponent {
    val component = componentFactory.adhoc(module.name)
    module.variants.values.forEach { addVariant(component, it) }

    val newNames = module.variants.map { it.key to it.value.name }.filter { it.first != it.second }.toMap()
    return if (newNames.isNotEmpty()) {
        ComponentWithRenamedVariants(newNames, component as SoftwareComponentInternal)
    } else {
        component
    }
}

fun Project.addVariant(component: AdhocComponentWithVariants, variant: MultiModuleMavenPublishingConfiguration.Module.Variant) {
    val configuration = configurations.getOrCreate(variant.configurationName)
    configuration.apply {
        isCanBeResolved = false
        isCanBeConsumed = true

        variant.attributesConfigurations.forEach { configure -> attributes.configure() }
    }

    for ((artifactNotation, configure) in variant.artifactsWithConfigurations) {
        artifacts.add(configuration.name, artifactNotation) {
            configure()
        }
    }

    for (configure in variant.configurationConfigurations) {
        configuration.apply(configure)
    }

    component.addVariantsFromConfiguration(configuration) {
        variant.variantDetailsConfigurations.forEach { configure -> configure() }
    }
}

private class RenamedVariant(val newName: String, context: UsageContext) : UsageContext by context {
    override fun getName(): String = newName
}

private class ComponentWithRenamedVariants(
    val newNames: Map<String, String>,
    private val base: SoftwareComponentInternal
): SoftwareComponentInternal by base {

    override fun getName(): String = base.name
    override fun getUsages(): Set<UsageContext> {
        return base.usages.map {
            val newName = newNames[it.name]
            if (newName != null) {
                RenamedVariant(newName, it)
            } else {
                it
            }
        }.toSet()
    }
}

private class ComponentWithExternalVariants(
    private val mainComponent: SoftwareComponent,
    private val externalComponents: Set<SoftwareComponent>
) : ComponentWithVariants, SoftwareComponentInternal {
    override fun getName(): String = mainComponent.name

    override fun getUsages(): Set<UsageContext> = (mainComponent as SoftwareComponentInternal).usages

    override fun getVariants(): Set<SoftwareComponent> = externalComponents
}

// endregion