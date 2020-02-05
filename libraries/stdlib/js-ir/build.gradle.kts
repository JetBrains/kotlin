import com.moowork.gradle.node.npm.NpmTask
import com.moowork.gradle.node.task.NodeTask

plugins {
    base
    id("com.github.node-gradle.node") version "2.2.0"
}

// A simple CLI for creating JS IR klibs.
// Does not depend on backend lowerings and JS codegen.
val jsIrKlibCli: Configuration by configurations.creating

// Full JS IR compiler CLI
val fullJsIrCli: Configuration by configurations.creating

dependencies {
    jsIrKlibCli(project(":compiler:cli-js-klib"))

    fullJsIrCli(project(":compiler:cli-js"))
    fullJsIrCli(project(":compiler:util"))
    fullJsIrCli(project(":compiler:cli-common"))
    fullJsIrCli(project(":compiler:cli"))
    fullJsIrCli(project(":compiler:frontend"))
    fullJsIrCli(project(":compiler:backend-common"))
    fullJsIrCli(project(":compiler:backend"))
    fullJsIrCli(project(":compiler:ir.backend.common"))
    fullJsIrCli(project(":compiler:ir.serialization.js"))
    fullJsIrCli(project(":compiler:backend.js"))
    fullJsIrCli(project(":js:js.translator"))
    fullJsIrCli(project(":js:js.serializer"))
    fullJsIrCli(project(":js:js.dce"))
    fullJsIrCli(project(":kotlin-reflect"))
    fullJsIrCli(intellijCoreDep()) { includeJars("intellij-core") }
    if (Platform[193].orLower()) {
        fullJsIrCli(intellijDep()) {
            includeJars("picocontainer", rootProject = rootProject)
        }
    }
    fullJsIrCli(intellijDep()) {
        includeJars("trove4j", "guava", "jdom", "asm-all", rootProject = rootProject)
    }
}

val unimplementedNativeBuiltIns =
  (file("$rootDir/core/builtins/native/kotlin/").list().toSortedSet() - file("$rootDir/libraries/stdlib/js-ir/builtins/").list())
    .map { "core/builtins/native/kotlin/$it" }

// Required to compile native builtins with the rest of runtime
val builtInsHeader = """@file:Suppress(
    "NON_ABSTRACT_FUNCTION_WITH_NO_BODY",
    "MUST_BE_INITIALIZED_OR_BE_ABSTRACT",
    "EXTERNAL_TYPE_EXTENDS_NON_EXTERNAL_TYPE",
    "PRIMARY_CONSTRUCTOR_DELEGATION_CALL_EXPECTED",
    "WRONG_MODIFIER_TARGET"
)
"""

val fullRuntimeSources by task<Sync> {

    val sources = listOf(
        "core/builtins/src/kotlin/",
        "libraries/stdlib/common/src/",
        "libraries/stdlib/src/kotlin/",
        "libraries/stdlib/unsigned/",
        "libraries/stdlib/js/src/",
        "libraries/stdlib/js/runtime/",
        "libraries/stdlib/js-ir/builtins/",
        "libraries/stdlib/js-ir/src/",
        "libraries/stdlib/js-ir/runtime/",

        // TODO get rid - move to test module
        "js/js.translator/testData/_commonFiles/"
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

    into("$buildDir/fullRuntime/src")

    doLast {
        unimplementedNativeBuiltIns.forEach { path ->
            val file = File("$buildDir/fullRuntime/src/$path")
            val sourceCode = builtInsHeader + file.readText()
            file.writeText(sourceCode)
        }
    }
}

val reducedRuntimeSources by task<Sync> {
    dependsOn(fullRuntimeSources)

    from(fullRuntimeSources.get().outputs.files.singleFile) {
        exclude(
            listOf(
                "libraries/stdlib/unsigned/**",
                "libraries/stdlib/common/src/generated/_Arrays.kt",
                "libraries/stdlib/common/src/generated/_Collections.kt",
                "libraries/stdlib/common/src/generated/_Comparisons.kt",
                "libraries/stdlib/common/src/generated/_Maps.kt",
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
                "libraries/stdlib/js-ir/runtime/collectionsHacks.kt",
                "libraries/stdlib/js-ir/src/generated/**",
                "libraries/stdlib/js-ir/src/kotlin/text/**",
                "libraries/stdlib/js/src/jquery/**",
                "libraries/stdlib/js/src/org.w3c/**",
                "libraries/stdlib/js/src/kotlin/char.kt",
                "libraries/stdlib/js/src/kotlin/collections.kt",
                "libraries/stdlib/js/src/kotlin/collections/**",
                "libraries/stdlib/js/src/kotlin/time/**",
                "libraries/stdlib/js/src/kotlin/console.kt",
                "libraries/stdlib/js/src/kotlin/coreDeprecated.kt",
                "libraries/stdlib/js/src/kotlin/date.kt",
                "libraries/stdlib/js/src/kotlin/debug.kt",
                "libraries/stdlib/js/src/kotlin/grouping.kt",
                "libraries/stdlib/js/src/kotlin/json.kt",
                "libraries/stdlib/js/src/kotlin/promise.kt",
                "libraries/stdlib/js/src/kotlin/regexp.kt",
                "libraries/stdlib/js/src/kotlin/sequence.kt",
                "libraries/stdlib/js/src/kotlin/text/**",
                "libraries/stdlib/js/src/kotlin/reflect/KTypeHelpers.kt",
                "libraries/stdlib/js/src/kotlin/reflect/KTypeParameterImpl.kt",
                "libraries/stdlib/js/src/kotlin/reflect/KTypeImpl.kt",
                "libraries/stdlib/src/kotlin/collections/**",
                "libraries/stdlib/src/kotlin/experimental/bitwiseOperations.kt",
                "libraries/stdlib/src/kotlin/properties/Delegates.kt",
                "libraries/stdlib/src/kotlin/random/URandom.kt",
                "libraries/stdlib/src/kotlin/text/**",
                "libraries/stdlib/src/kotlin/time/**",
                "libraries/stdlib/src/kotlin/util/KotlinVersion.kt",
                "libraries/stdlib/src/kotlin/util/Tuples.kt",
                "libraries/stdlib/js/src/kotlin/dom/**",
                "libraries/stdlib/js/src/kotlin/browser/**"
            )
        )
    }

    from("$rootDir/libraries/stdlib/js-ir/smallRuntime") {
        into("libraries/stdlib/js-ir/runtime/")
    }

    into("$buildDir/reducedRuntime/src")
}

fun JavaExec.buildKLib(
    moduleName: String,
    sources: List<File>,
    dependencies: List<File>,
    outDir: File,
    commonSources: List<File>
) {
    inputs.files(sources)
        .withPathSensitivity(PathSensitivity.RELATIVE)

    outputs.dir(file(outDir))
    outputs.cacheIf { true }

    classpath = jsIrKlibCli
    main = "org.jetbrains.kotlin.ir.backend.js.GenerateJsIrKlibKt"
    workingDir = rootDir
    args = sources.map(::pathRelativeToWorkingDir) +
            listOf("-n", moduleName, "-o", pathRelativeToWorkingDir(outDir)) +
            dependencies.flatMap { listOf("-d", pathRelativeToWorkingDir(it)) } +
            commonSources.flatMap { listOf("-c", pathRelativeToWorkingDir(it)) }

    dependsOn(":compiler:cli-js-klib:jar")
    passClasspathInJar()
}

val fullRuntimeDir = buildDir.resolve("fullRuntime/klib")

val generateFullRuntimeKLib by eagerTask<NoDebugJavaExec> {
    dependsOn(fullRuntimeSources)

    buildKLib(moduleName = "kotlin",
              sources = listOf(fullRuntimeSources.get().outputs.files.singleFile),
              dependencies = emptyList(),
              outDir = fullRuntimeDir,
              commonSources = listOf("common", "src", "unsigned").map { file("$buildDir/fullRuntime/src/libraries/stdlib/$it") }
    )
}

val packFullRuntimeKLib by tasks.registering(Jar::class) {
    dependsOn(generateFullRuntimeKLib)
    from(fullRuntimeDir)
    destinationDirectory.set(rootProject.buildDir.resolve("js-ir-runtime"))
    archiveFileName.set("full-runtime.klib")
}

val generateReducedRuntimeKLib by eagerTask<NoDebugJavaExec> {
    dependsOn(reducedRuntimeSources)

    buildKLib(moduleName = "kotlin",
              sources = listOf(reducedRuntimeSources.get().outputs.files.singleFile),
              dependencies = emptyList(),
              outDir = buildDir.resolve("reducedRuntime/klib"),
              commonSources = listOf("common", "src", "unsigned").map { file("$buildDir/reducedRuntime/src/libraries/stdlib/$it") }
    )
}

val generateWasmRuntimeKLib by eagerTask<NoDebugJavaExec> {
    buildKLib(moduleName = "kotlin",
              sources = listOf(file("$rootDir/libraries/stdlib/wasm")),
              dependencies = emptyList(),
              outDir = file("$buildDir/wasmRuntime/klib"),
              commonSources = emptyList()
    )
}

val kotlinTestCommonSources = listOf(
    "$rootDir/libraries/kotlin.test/annotations-common/src/main",
    "$rootDir/libraries/kotlin.test/common/src/main"
)

val generateKotlinTestKLib by eagerTask<NoDebugJavaExec> {
    dependsOn(generateFullRuntimeKLib)

    buildKLib(
        moduleName = "kotlin-test",
        sources = (listOf("$rootDir/libraries/kotlin.test/js/src/main") + kotlinTestCommonSources).map(::file),
        dependencies = listOf(generateFullRuntimeKLib.outputs.files.singleFile),
        outDir = file("$buildDir/kotlin.test/klib"),
        commonSources = kotlinTestCommonSources.map(::file)
    )
}

val jsTestDir = "${buildDir}/testSrc"

val prepareStdlibTestSources by task<Sync> {
    from("$rootDir/libraries/stdlib/test") {
        exclude("src/generated/**")
        into("test")
    }
    from("$rootDir/libraries/stdlib/common/test") {
        exclude("src/generated/**")
        into("common")
    }
    from("$rootDir/libraries/stdlib/js/test") {
        into("js")
    }
    into(jsTestDir)
}

fun JavaExec.buildJs(sources: List<String>, dependencies: List<String>, outPath: String, commonSources: List<String>) {
    inputs.files(sources)
    outputs.dir(file(outPath).parent)
    classpath = fullJsIrCli
    main = "org.jetbrains.kotlin.cli.js.K2JsIrCompiler"
    workingDir = rootDir

    val libraryString: String = dependencies.joinToString(File.pathSeparator)
    val libraryArgs: List<String> = if (libraryString.isEmpty()) emptyList() else listOf<String>("-libraries", libraryString, "-Xfriend-modules=$libraryString")
    val allArgs =
     sources.toList() + listOf("-output", outPath) + libraryArgs + listOf(
         "-Xir-produce-js",
         "-Xmulti-platform",
         "-Xuse-experimental=kotlin.Experimental",
         "-Xuse-experimental=kotlin.contracts.ExperimentalContracts",
         "-Xuse-experimental=kotlin.ExperimentalMultiplatform",
         "-Xuse-experimental=kotlin.ExperimentalStdlibApi",
         "-Xuse-experimental=kotlin.ExperimentalUnsignedTypes"
     )
    args = allArgs

    dependsOn(":compiler:cli-js:jar")
    passClasspathInJar()
}

val testOutputFile = "$buildDir/kotlin-stdlib-js-ir_test.js"

val tryRunFullCli by eagerTask<NoDebugJavaExec> {
    dependsOn(prepareStdlibTestSources)
    dependsOn(generateFullRuntimeKLib)
    dependsOn(generateKotlinTestKLib)

    buildJs(
        sources = listOf(jsTestDir),
        dependencies = listOf(
            "${generateFullRuntimeKLib.outputs.files.singleFile.path}/klib",
            "${generateKotlinTestKLib.outputs.files.singleFile.path}/klib"
        ),
        outPath = testOutputFile,
        commonSources = emptyList()
    )
}

node {
    download = true
    version = "10.16.2"
    nodeModulesDir = buildDir
}

val installMocha by task<NpmTask> {
    setArgs(listOf("install", "mocha"))
}

val installTeamcityReporter by task<NpmTask> {
    setArgs(listOf("install", "mocha-teamcity-reporter"))
}

// TODO: TEST OUTPUT FILE
// val kotlinTestTestOutputFile = "${project(':kotlin-test:kotlin-test-js').buildDir}/classes/kotlin/test/kotlin-test-js-ir_test.js"

val runMocha by task<NodeTask> {
    dependsOn(installMocha)
    dependsOn(tryRunFullCli)

    script = file("${buildDir}/node_modules/mocha/bin/mocha")

    if (project.hasProperty("teamcity")) {
        dependsOn(installTeamcityReporter)
        setArgs(
            listOf(
                "--reporter",
                "mocha-teamcity-reporter",
                "--reporter-options",
                "topLevelSuite=stdlib-js-ir"
            )
        )
    }
    else {
        setArgs(listOf("--reporter", "min"))
    }

    val allArgs = getArgs().toList() + listOf(testOutputFile/*, kotlinTestTestOutputFile*/)
    setArgs(allArgs)

    setIgnoreExitValue(rootProject.getBooleanProperty("ignoreTestFailures") ?: false)
    setWorkingDir(buildDir)
}

tasks {
    val test by registering { dependsOn(runMocha) }
    val check by existing { dependsOn(test) }

    // dummy task to make coreLibsInstall aggregate task not fail
    val install by registering
}