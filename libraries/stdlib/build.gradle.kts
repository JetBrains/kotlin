@file:Suppress("UNUSED_VARIABLE", "NAME_SHADOWING", "DEPRECATION")
import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.mpp.GenerateProjectStructureMetadata
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages
import org.jetbrains.kotlin.gradle.targets.js.d8.D8Plugin
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinTargetWithNodeJsDsl
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinWasmTargetDsl
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrLink
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompile
import org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile
import org.jetbrains.kotlin.gradle.tasks.UsesKotlinJavaToolchain
import org.jetbrains.kotlin.library.KOTLIN_JS_STDLIB_NAME
import org.jetbrains.kotlin.library.KOTLIN_WASM_STDLIB_NAME
import plugins.configureDefaultPublishing
import plugins.configureKotlinPomAttributes
import plugins.publishing.configureMultiModuleMavenPublishing
import plugins.publishing.copyAttributes
import kotlin.io.path.copyTo

plugins {
    kotlin("multiplatform")
    `maven-publish`
    signing
    id("nodejs-cache-redirector-configuration")
    id("d8-configuration")
}

description = "Kotlin Standard Library"

configureJvmToolchain(JdkMajorVersion.JDK_1_8)

fun resolvingConfiguration(name: String, configure: Action<Configuration> = Action {}) =
    configurations.create(name) {
        isCanBeResolved = true
        isCanBeConsumed = false
        configure(this)
    }
fun outgoingConfiguration(name: String, configure: Action<Configuration> = Action {}) =
    configurations.create(name) {
        isCanBeResolved = false
        isCanBeConsumed = true
        configure(this)
    }

fun KotlinCommonCompilerOptions.mainCompilationOptions() {
    languageVersion = KotlinVersion.KOTLIN_2_1
    apiVersion = KotlinVersion.KOTLIN_2_1
    freeCompilerArgs.add("-Xstdlib-compilation")
    freeCompilerArgs.add("-Xdont-warn-on-error-suppression")
    if (!kotlinBuildProperties.disableWerror) allWarningsAsErrors = true
}

val configurationBuiltins = resolvingConfiguration("builtins") {
    attributes.attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, project.objects.named(LibraryElements.JAR))
}
dependencies {
    configurationBuiltins(project(":core:builtins"))
}

val jvmBuiltinsRelativeDir = "libraries/stdlib/jvm/builtins"
val jvmBuiltinsDir = "${rootDir}/${jvmBuiltinsRelativeDir}"

val jsDir = "${projectDir}/js"
val jsBuiltinsSrcDir = "${layout.buildDirectory.get().asFile}/src/js-builtin-sources"

val commonOptIns = listOf(
    "kotlin.ExperimentalMultiplatform",
    "kotlin.contracts.ExperimentalContracts",
)
val commonTestOptIns = listOf(
    "kotlin.ExperimentalUnsignedTypes",
    "kotlin.ExperimentalStdlibApi",
    "kotlin.io.encoding.ExperimentalEncodingApi",
    "kotlin.uuid.ExperimentalUuidApi",
)

kotlin {
    val renderDiagnosticNames by extra(project.kotlinBuildProperties.renderDiagnosticNames)
    val diagnosticNamesArg = if (renderDiagnosticNames) "-Xrender-internal-diagnostic-names" else null

    explicitApi()

    metadata {
        compilations {
            all {
                compileTaskProvider.configure {
                    compilerOptions {
                        freeCompilerArgs.set(
                            listOfNotNull(
                                "-Xallow-kotlin-package",
                                "-module-name", "kotlin-stdlib-common",
                                "-Xexpect-actual-classes",
                                "-Xexplicit-api=strict",
                                diagnosticNamesArg,
                            )
                        )
                        mainCompilationOptions()
                    }
                }
            }
        }
    }
    jvm {
        withJava()
        compilations {
            val compileOnlyDeclarations by creating {
                compileTaskProvider.configure {
                    compilerOptions {
                        freeCompilerArgs.set(
                            listOfNotNull(
                                "-Xallow-kotlin-package",
                                "-Xsuppress-missing-builtins-error",
                                diagnosticNamesArg
                            )
                        )
                    }
                }
            }

            val main by getting {
                compileTaskProvider.configure {
                    this as UsesKotlinJavaToolchain
                    kotlinJavaToolchain.toolchain.use(getToolchainLauncherFor(JdkMajorVersion.JDK_11_0))
                    compilerOptions {
                        moduleName = "kotlin-stdlib"
                        jvmTarget = JvmTarget.JVM_1_8
                        // providing exhaustive list of args here
                        freeCompilerArgs.set(
                            listOfNotNull(
                                "-Xjdk-release=6",
                                "-Xallow-kotlin-package",
                                "-Xexpect-actual-classes",
                                "-Xmultifile-parts-inherit",
                                "-Xuse-14-inline-classes-mangling-scheme",
                                "-Xno-new-java-annotation-targets",
                                diagnosticNamesArg,
                            )
                        )
                        mainCompilationOptions()
                    }
                }
                defaultSourceSet {
                    dependencies {
                        compileOnly(compileOnlyDeclarations.output.allOutputs)
                    }
                }
            }
            val mainJdk7 by creating {
                associateWith(main)
                compileTaskProvider.configure {
                    this as UsesKotlinJavaToolchain
                    kotlinJavaToolchain.toolchain.use(getToolchainLauncherFor(JdkMajorVersion.JDK_11_0))
                    compilerOptions {
                        moduleName = "kotlin-stdlib-jdk7"
                        jvmTarget = JvmTarget.JVM_1_8
                        freeCompilerArgs.set(
                            listOfNotNull(
                                "-Xjdk-release=7",
                                "-Xallow-kotlin-package",
                                "-Xexpect-actual-classes",
                                "-Xmultifile-parts-inherit",
                                "-Xno-new-java-annotation-targets",
                                "-Xexplicit-api=strict",
                                diagnosticNamesArg,
                            )
                        )
                        mainCompilationOptions()
                    }
                }
            }
            val mainJdk8 by creating {
                associateWith(main)
                associateWith(mainJdk7)
                compileTaskProvider.configure {
                    compilerOptions {
                        moduleName = "kotlin-stdlib-jdk8"
                        freeCompilerArgs.set(
                            listOfNotNull(
                                "-Xallow-kotlin-package",
                                "-Xmultifile-parts-inherit",
                                "-Xno-new-java-annotation-targets",
                                "-Xexplicit-api=strict",
                                diagnosticNamesArg,
                            )
                        )
                        mainCompilationOptions()
                    }
                }
            }
            project.sourceSets.create("java9") {
                java.srcDir("jvm/java9")
            }
            configureJava9Compilation("kotlin.stdlib", listOf(
                main.output.allOutputs,
                mainJdk7.output.allOutputs,
                mainJdk8.output.allOutputs,
            ), main.configurations.compileDependencyConfiguration)
            val test by getting {
                associateWith(mainJdk7)
                associateWith(mainJdk8)
                compileTaskProvider.configure {
                    compilerOptions {
                        freeCompilerArgs.addAll(
                            listOf(
                                "-Xallow-kotlin-package", // TODO: maybe rename test packages
                                "-Xexpect-actual-classes",
                            )
                        )
                    }
                }
            }
            val longRunningTest by creating {
                associateWith(main)
                associateWith(mainJdk7)
                associateWith(mainJdk8)
            }
        }
    }
    js(IR) {
        if (!kotlinBuildProperties.isTeamcityBuild) {
            browser {}
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
                @Suppress("DEPRECATION")
                kotlinOptions {
                    freeCompilerArgs += listOf(
                        "-Xallow-kotlin-package",
                        "-Xexpect-actual-classes",
                    )
                }
            }
            val main by getting {
                @Suppress("DEPRECATION")
                kotlinOptions {
                    freeCompilerArgs += listOfNotNull(
                        "-Xir-module-name=$KOTLIN_JS_STDLIB_NAME",
                        diagnosticNamesArg,
                    )
                }
                compileTaskProvider.configure {
                    compilerOptions.mainCompilationOptions()
                }
            }
        }
    }

    fun KotlinWasmTargetDsl.commonWasmTargetConfiguration() {
        (this as KotlinTargetWithNodeJsDsl).nodejs()
        compilations {
            all {
                @Suppress("DEPRECATION")
                kotlinOptions.freeCompilerArgs += listOfNotNull(
                    "-Xallow-kotlin-package",
                    "-Xexpect-actual-classes",
                    diagnosticNamesArg
                )
            }
            @Suppress("DEPRECATION")
            val main by getting {
                kotlinOptions.freeCompilerArgs += "-Xir-module-name=$KOTLIN_WASM_STDLIB_NAME"
                compileTaskProvider.configure {
                    compilerOptions.mainCompilationOptions()
                }
            }
        }
    }

    // Please remove this check after bootstrap and replacing @ExperimentalWasmDsl
    val newExperimentalWasmDslAvailable = runCatching {
        Class.forName("org.jetbrains.kotlin.gradle.ExperimentalWasmDsl")
    }.isSuccess

    if (newExperimentalWasmDslAvailable) {
        logger.warn(
            """
            Apparently kotlin bootstrap just happened. And @ExperimentalWasmDsl annotation was moved to a new FQN.
            Please replace 'org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalWasmDsl' 
            with 'org.jetbrains.kotlin.gradle.ExperimentalWasmDsl'
            and remove this check.
            
            Please note that the same check exists in kotlin-test module. Fix it there too.
            """.trimIndent()
        )
    }

    @Suppress("OPT_IN_USAGE")
    // Remove line above and uncomment line below after bootstrap
    // @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs {
        commonWasmTargetConfiguration()
    }

    @Suppress("OPT_IN_USAGE")
    // Remove line above and uncomment line below after bootstrap
    // @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmWasi {
        commonWasmTargetConfiguration()
    }

    if (kotlinBuildProperties.isInIdeaSync) {
        val hostOs = System.getProperty("os.name")
        val isMingwX64 = hostOs.startsWith("Windows")
        val nativeTarget = when {
            hostOs == "Mac OS X" -> macosX64("native")
            hostOs == "Linux" -> linuxX64("native")
            isMingwX64 -> mingwX64("native")
            else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
        }
        nativeTarget.apply {
            compilations.all {
                @Suppress("DEPRECATION")
                kotlinOptions {
                    freeCompilerArgs += listOf(
                        "-Xallow-kotlin-package",
                        "-Xexpect-actual-classes",
                        "-nostdlib",
                    )
                }
            }
        }
    }

    sourceSets {
        fun <TP : TaskProvider<*>> TP.requiredForImport(): TP {
            tasks.findByName("prepareKotlinIdeaImport")?.dependsOn(this)
            return this
        }
        all {
            kotlin.setSrcDirs(emptyList<File>())
        }
        commonMain {
            val prepareCommonSources by tasks.registering {
                dependsOn(":prepare:build.version:writeStdlibVersion")
            }
            kotlin {
                srcDir("common/src")
                srcDir(files("src").builtBy(prepareCommonSources))
                srcDir("unsigned/src")
            }
        }
        commonTest {
            dependencies {
                api(kotlinTest())
            }
            kotlin {
                srcDir("common/test")
                srcDir("test")
            }
        }
        val jvmCompileOnlyDeclarations by getting {
            kotlin.srcDir("jvm/compileOnly")
        }
        val jvmMain by getting {
            project.configurations.getByName("jvmMainCompileOnly").extendsFrom(configurationBuiltins)
            dependencies {
                api("org.jetbrains:annotations:13.0")
            }
            val jvmSrcDirs = listOfNotNull(
                "jvm/src",
                "jvm/runtime",
            )
            project.sourceSets["main"].java.srcDirs(*jvmSrcDirs.toTypedArray())
            kotlin.setSrcDirs(jvmSrcDirs)
            kotlin.exclude("kotlin/internal/InternalAnnotations.kt")
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
                api(kotlinTest("junit"))
            }
            kotlin.srcDir("jvm/test")
            kotlin.srcDir("jdk7/test")
            kotlin.srcDir("jdk8/test")
        }

        val jvmLongRunningTest by getting {
            dependencies {
                api(kotlinTest("junit"))
            }
            kotlin.srcDir("jvm/testLongRunning")
        }

        val jsMain by getting {
            val prepareJsIrMainSources by tasks.registering(Sync::class)
            kotlin {
                srcDir(prepareJsIrMainSources.requiredForImport())
                srcDir("$jsDir/builtins")
                srcDir("$jsDir/runtime")
                srcDir("$jsDir/src").apply {
                    exclude("kotlin/browser")
                    exclude("kotlin/dom")
                    exclude("kotlinx")
                    exclude("org.w3c")
                }
            }

            prepareJsIrMainSources.configure {
                val unimplementedNativeBuiltIns =
                    (file(jvmBuiltinsDir).list()!!.toSortedSet() - file("$jsDir/builtins/").list()!!)
                        .map { "$jvmBuiltinsRelativeDir/$it" }
                        .filterNot { it == "$jvmBuiltinsRelativeDir/Atomics.jvm.kt" || it == "$jvmBuiltinsRelativeDir/AtomicArrays.jvm.kt" }

                val sources = unimplementedNativeBuiltIns

                sources.forEach { path ->
                    from("$rootDir/$path") {
                        into(path.dropLastWhile { it != '/' })
                    }
                }

                into(jsBuiltinsSrcDir)

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
                        val file = File("$destinationDir/$path")
                        val sourceCode = builtInsHeader + file.readText()
                        file.writeText(sourceCode)
                    }
                }
            }
        }
        val jsTest by getting {
            kotlin.srcDir("${jsDir}/test")
        }

        val nativeWasmMain by creating {
            dependsOn(commonMain.get())
            kotlin.srcDir("native-wasm/src")
        }

        val nativeWasmTest by creating {
            dependsOn(commonTest.get())
            kotlin.srcDir("native-wasm/test")
        }

        val wasmCommonMain by creating {
            dependsOn(nativeWasmMain)
            val prepareWasmBuiltinSources by tasks.registering(Sync::class)
            kotlin {
                srcDir(prepareWasmBuiltinSources.requiredForImport())
                srcDir("wasm/builtins")
                srcDir("wasm/internal")
                srcDir("wasm/runtime")
                srcDir("wasm/src")
                srcDir("wasm/stubs")
            }
            prepareWasmBuiltinSources.configure {
                val unimplementedNativeBuiltIns =
                    (file(jvmBuiltinsDir).list().toSortedSet() - file("wasm/builtins/kotlin/").list())
                        .map { "$jvmBuiltinsRelativeDir/$it" }

                val sources = unimplementedNativeBuiltIns

                val excluded = listOf(
                    "Atomics.jvm.kt", "AtomicArrays.jvm.kt",
                    // Included with K/N collections
                    "Collections.kt", "Iterator.kt"
                )

                sources.forEach { path ->
                    from("$rootDir/$path") {
                        into(path.dropLastWhile { it != '/' })
                        excluded.forEach {
                            exclude(it)
                        }
                    }
                }

                into(layout.buildDirectory.dir("src/wasm-builtin-sources"))
            }

        }
        val wasmCommonTest by creating {
            dependsOn(nativeWasmTest)
            kotlin {
                srcDir("wasm/test")
            }
        }

        val wasmJsMain by getting {
            dependsOn(wasmCommonMain)
            kotlin {
                srcDir("wasm/js/builtins")
                srcDir("wasm/js/internal")
                srcDir("wasm/js/src")
            }
        }
        val wasmJsTest by getting {
            dependsOn(wasmCommonTest)
            kotlin {
                srcDir("wasm/js/test")
            }
        }
        val wasmWasiMain by getting {
            dependsOn(wasmCommonMain)
            kotlin {
                srcDir("wasm/wasi/builtins")
                srcDir("wasm/wasi/src")
            }
            languageSettings {
                optIn("kotlin.wasm.unsafe.UnsafeWasmMemoryApi")
            }
        }
        val wasmWasiTest by getting {
            dependsOn(wasmCommonTest)
            kotlin {
                srcDir("wasm/wasi/test")
            }
        }

        if (kotlinBuildProperties.isInIdeaSync) {
            val nativeKotlinTestCommon by creating {
                dependsOn(commonMain.get())
                val prepareKotlinTestCommonNativeSources by tasks.registering(Sync::class) {
                    from("../kotlin.test/common/src/main/kotlin")
                    from("../kotlin.test/annotations-common/src/main/kotlin")
                    into(layout.buildDirectory.dir("src/native-kotlin-test-common-sources"))
                }

                kotlin {
                    srcDir(prepareKotlinTestCommonNativeSources.requiredForImport())
                }
            }
            val nativeMain by getting {
                dependsOn(nativeWasmMain)
                dependsOn(nativeKotlinTestCommon)
                kotlin {
                    srcDir("$rootDir/kotlin-native/runtime/src/main/kotlin")
                    srcDir("$rootDir/kotlin-native/Interop/Runtime/src/main/kotlin")
                    srcDir("$rootDir/kotlin-native/Interop/Runtime/src/native/kotlin")
                }
                languageSettings {
                    optIn("kotlin.native.internal.InternalForKotlinNative")
                }
            }
            val nativeTest by getting {
                dependsOn(nativeWasmTest)
                kotlin {
                    srcDir("$rootDir/kotlin-native/runtime/test")
                }
                languageSettings {
                    optIn("kotlin.experimental.ExperimentalNativeApi")
                    optIn("kotlin.native.ObsoleteNativeApi")
                    optIn("kotlin.native.runtime.NativeRuntimeApi")
                    optIn("kotlin.native.internal.InternalForKotlinNative")
                    optIn("kotlinx.cinterop.ExperimentalForeignApi")
                    optIn("kotlin.native.concurrent.ObsoleteWorkersApi")
                }
            }
        }

        all sourceSet@ {
            languageSettings {
                // TODO: progressiveMode = use build property 'test.progressive.mode'
                if (this@sourceSet == jvmCompileOnlyDeclarations) {
                    return@languageSettings
                }
                commonOptIns.forEach { optIn(it) }
                if (this@sourceSet.name.endsWith("Test")) {
                    commonTestOptIns.forEach { optIn(it) }
                }
            }
        }
    }
}

dependencies {
    val jvmMainApi by configurations.getting
    val metadataApiElements by configurations.getting
    val nativeApiElements = configurations.maybeCreate("nativeApiElements")
    constraints {
        // there is no dependency anymore from kotlin-stdlib to kotlin-stdlib-common,
        // but use this constraint to align it if another library brings it transitively
        jvmMainApi(project(":kotlin-stdlib-common"))
        metadataApiElements(project(":kotlin-stdlib-common"))
        nativeApiElements(project(":kotlin-stdlib-common"))
        // to avoid split package and duplicate classes on classpath after moving them from these artifacts in 1.8.0
        jvmMainApi("org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.8.0")
        jvmMainApi("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.8.0")
    }
}

tasks {
    val allMetadataJar by existing(Jar::class) {
        archiveClassifier = "all"
    }
    val metadataJar by registering(Jar::class) {
        archiveAppendix.set("metadata")
        archiveExtension.set("klib")
    }
    kotlin.metadata().compilations.named { it == "commonMain" }.configureEach {
        metadataJar.configure { from(output.allOutputs) }
    }
    val sourcesJar by existing(Jar::class) {
        archiveAppendix.set("metadata")
    }
    val jvmJar by existing(Jar::class) {
        dependsOn(configurationBuiltins)
        duplicatesStrategy = DuplicatesStrategy.FAIL
        archiveAppendix.set(null as String?)
        manifestAttributes(manifest, "Main", multiRelease = true)
        manifest.attributes(mapOf("Implementation-Title" to "kotlin-stdlib"))
        from { zipTree(configurationBuiltins.singleFile) }
        from(kotlin.jvm().compilations["mainJdk7"].output.allOutputs)
        from(kotlin.jvm().compilations["mainJdk8"].output.allOutputs)
        from(project.sourceSets["java9"].output)
    }

    val jvmSourcesJar by existing(Jar::class) {
        duplicatesStrategy = DuplicatesStrategy.FAIL
        archiveAppendix.set(null as String?)
        into("jvmMain") {
            from(jvmBuiltinsDir) {
                into("kotlin")
            }
            from(kotlin.sourceSets["jvmMainJdk7"].kotlin) {
                into("jdk7")
            }
            from(kotlin.sourceSets["jvmMainJdk8"].kotlin) {
                into("jdk8")
            }
        }
    }

    dexMethodCount {
        from(jvmJar)
        ownPackages.set(listOf("kotlin"))
    }

    val jsJar by existing(Jar::class) {
        manifestAttributes(manifest, "Main")
        manifest.attributes(mapOf("Implementation-Title" to "kotlin-stdlib-js"))
    }

    val jsJarForTests by registering(Copy::class) {
        from(jsJar)
        rename { _ -> "full-runtime.klib" }
        // some tests expect stdlib-js klib in this location
        into(rootProject.layout.buildDirectory.dir("js-ir-runtime"))
    }

    val jsRearrangedSourcesJar by registering(Jar::class) {
        archiveClassifier.set("js-sources")
        archiveVersion.set("")
        destinationDirectory.set(layout.buildDirectory.dir("lib"))

        includeEmptyDirs = false
        duplicatesStrategy = DuplicatesStrategy.FAIL

        into("commonMain") {
            from(kotlin.sourceSets.commonMain.get().kotlin)
        }
        into("jsMain") {
            from(kotlin.sourceSets["jsMain"].kotlin) {
                // just to depend on source-generating tasks
                exclude("**")
            }
            from(jvmBuiltinsDir) {
                into("kotlin")
                include("Comparable.kt")
                include("Enum.kt")
            }
            from("$jsBuiltinsSrcDir/libraries/stdlib/jvm") {
                exclude("builtins/Comparable.kt")
            }
            from("$jsBuiltinsSrcDir/libraries/stdlib/js/src")
            from("$jsDir/builtins") {
                into("kotlin")
                exclude("Enum.kt")
            }
            from("$jsDir/runtime") {
                into("runtime")
            }
            from("$jsDir/src") {
                include("**/*.kt")
            }
        }
    }

    val jsSourcesJar by existing(Jar::class) {
        val jsSourcesJarFile = jsRearrangedSourcesJar.get().archiveFile
        inputs.file(jsSourcesJarFile)
        doLast {
            jsSourcesJarFile.get().asFile.toPath().copyTo(archiveFile.get().asFile.toPath(), overwrite = true)
        }
    }

    val wasmJsJar by existing(Jar::class) {
        manifestAttributes(manifest, "Main")
        manifest.attributes(mapOf("Implementation-Title" to "kotlin-stdlib-wasm-js"))
    }
    val wasmWasiJar by existing(Jar::class) {
        manifestAttributes(manifest, "Main")
        manifest.attributes(mapOf("Implementation-Title" to "kotlin-stdlib-wasm-wasi"))
    }

    artifacts {
        val distJsJar = configurations.create("distJsJar")
        val distJsSourcesJar = configurations.create("distJsSourcesJar")
        val distJsKlib = configurations.create("distJsKlib")
        val commonMainMetadataElements by configurations.creating

        add(distJsSourcesJar.name, jsSourcesJar)
        add(distJsKlib.name, jsJar)
        add(commonMainMetadataElements.name, metadataJar)
    }


    val jvmTest by existing(Test::class)

    listOf(JdkMajorVersion.JDK_9_0, JdkMajorVersion.JDK_11_0).forEach { jvmVersion ->
        val jvmVersionTest = register("jvm${jvmVersion.majorVersion}Test", Test::class) {
            group = "verification"
            javaLauncher.set(getToolchainLauncherFor(jvmVersion))
            // additional test tasks are not configured automatically same as the main test task
            // after KMP plugin stopped applying java plugin
            classpath = jvmTest.get().classpath
            testClassesDirs = jvmTest.get().testClassesDirs

        }
        check.configure { dependsOn(jvmVersionTest) }
    }

    val jvmLongRunningTest by registering(Test::class) {
        group = "verification"
        val compilation = kotlin.jvm().compilations["longRunningTest"]
        classpath = compilation.compileDependencyFiles + compilation.runtimeDependencyFiles + compilation.output.allOutputs
        testClassesDirs = compilation.output.classesDirs
    }

    if (project.hasProperty("kotlin.stdlib.test.long.running")) {
        check.configure { dependsOn(jvmLongRunningTest) }
    }

    listOf("Js", "Wasi").forEach { wasmTarget ->
        named("compileTestKotlinWasm$wasmTarget", AbstractKotlinCompile::class) {
            // TODO: fix all warnings, enable -Werror
            compilerOptions.suppressWarnings = true
            // exclusions due to KT-51647
            exclude("generated/minmax/*")
            exclude("collections/MapTest.kt")
        }
        named("compileTestDevelopmentExecutableKotlinWasm$wasmTarget", KotlinJsIrLink::class) {
            @Suppress("DEPRECATION")
            kotlinOptions.freeCompilerArgs += listOf("-Xwasm-enable-array-range-checks")
        }
    }
    val wasmWasiNodeTest by existing {
        if (!kotlinBuildProperties.getBoolean("kotlin.stdlib.wasi.tests")) {
            enabled = false
        }
    }

    /*
    We are using a custom 'kotlin-project-structure-metadata' to ensure 'nativeApiElements' lists 'commonMain' as source set
    */
    val generateProjectStructureMetadata by existing(GenerateProjectStructureMetadata::class) {
        val outputTestFile = file("kotlin-project-structure-metadata.beforePatch.json")
        val patchedFile = file("kotlin-project-structure-metadata.json")

        inputs.file(patchedFile)
        inputs.file(outputTestFile)
        inputs.property("isInIdeaSync", kotlinBuildProperties.isInIdeaSync)

        // overwrite kotlin-project-structure-metadata when building the artifact,
        // but use automatically generated one when importing the project
        // because of the different source set structure
        if (!kotlinBuildProperties.isInIdeaSync) {
            doLast {
                /*
                    Check that the generated 'outputFile' by default matches our expectations stored in the .beforePatch file
                    This will fail if the kotlin-project-structure-metadata.json file would change unnoticed (w/o updating our patched file)
                     */
                run {
                    val outputFileText = resultFile.readText().trim()
                    val expectedFileContent = outputTestFile.readText().trim()
                    if (outputFileText != expectedFileContent)
                        error(
                            "${resultFile.path} file content does not match expected content\n\n" +
                                    "expected:\n\n$expectedFileContent\n\nactual:\n\n$outputFileText"
                        )
                }

                patchedFile.copyTo(resultFile, overwrite = true)
            }
        }
    }

}


// region ==== Publishing ====

configureDefaultPublishing()


val emptyJavadocJar by tasks.creating(org.gradle.api.tasks.bundling.Jar::class) {
    archiveClassifier.set("javadoc")
}

publishing {
    val artifactBaseName = base.archivesName.get()
    configureMultiModuleMavenPublishing {
        val rootModule = module("rootModule") {
            mavenPublication {
                artifactId = artifactBaseName
                configureKotlinPomAttributes(project, "Kotlin Standard Library")
                artifact(emptyJavadocJar)
            }

            // creates a variant from existing configuration or creates new one
            variant("jvmApiElements")
            variant("jvmRuntimeElements")
            variant("jvmSourcesElements")

            variant("metadataApiElements")
            variant("metadataSourcesElementsFromJvm") {
                name = "metadataSourcesElements"
                configuration {
                    // to avoid clash in Gradle 8+ with metadataSourcesElements configuration with the same attributes
                    isCanBeConsumed = false
                }
                attributes {
                    copyAttributes(from = project.configurations["metadataSourcesElements"].attributes, to = this)
                }
                artifact(tasks["sourcesJar"]) {
                    classifier = "common-sources"
                }
            }
            variant("nativeApiElements") {
                attributes {
                    attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.LIBRARY))
                    attribute(TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE, objects.named("non-jvm"))
                    attribute(Usage.USAGE_ATTRIBUTE, objects.named(KotlinUsages.KOTLIN_API))
                    attribute(KotlinPlatformType.attribute, KotlinPlatformType.native)
                }
            }
        }

        val js = module("jsModule") {
            mavenPublication {
                artifactId = "$artifactBaseName-js"
                configureKotlinPomAttributes(project, "Kotlin Standard Library for JS", packaging = "klib")
            }
            variant("jsApiElements")
            variant("jsRuntimeElements")
            variant("jsSourcesElements")
        }

        val wasmJs = module("wasmJsModule") {
            mavenPublication {
                artifactId = "$artifactBaseName-wasm-js"
                configureKotlinPomAttributes(project, "Kotlin Standard Library for experimental WebAssembly JS platform", packaging = "klib")
            }
            variant("wasmJsApiElements")
            variant("wasmJsRuntimeElements")
            variant("wasmJsSourcesElements")
        }
        val wasmWasi = module("wasmWasiModule") {
            mavenPublication {
                artifactId = "$artifactBaseName-wasm-wasi"
                configureKotlinPomAttributes(project, "Kotlin Standard Library for experimental WebAssembly WASI platform", packaging = "klib")
            }
            variant("wasmWasiApiElements")
            variant("wasmWasiRuntimeElements")
            variant("wasmWasiSourcesElements")
        }

        // Makes all variants from accompanying artifacts visible through `available-at`
        rootModule.include(js, wasmJs, wasmWasi)
    }

    publications {
        val rootModule by existing(MavenPublication::class)
        val jsModule by existing(MavenPublication::class)
        configureSbom("Main", "kotlin-stdlib", setOf("jvmRuntimeClasspath"), rootModule)
        configureSbom("Js", "kotlin-stdlib-js", setOf("jsRuntimeClasspath"), jsModule)

        val wasmJsModule by existing(MavenPublication::class)
        val wasmWasiModule by existing(MavenPublication::class)
        configureSbom("Wasm-Js", "kotlin-stdlib-wasm-js", setOf("wasmJsRuntimeClasspath"), wasmJsModule)
        configureSbom("Wasm-Wasi", "kotlin-stdlib-wasm-wasi", setOf("wasmWasiRuntimeClasspath"), wasmWasiModule)
    }
}


// endregion

// for legacy intra-project dependencies
for (name in listOf("sources", "distSources")) {
    val sourcesConfiguration = configurations.getOrCreate(name).apply {
        isCanBeResolved = false
        isCanBeConsumed = true
    }
    artifacts.add(sourcesConfiguration.name, tasks["jvmSourcesJar"])
}

// Disabling IC for JS tasks as they may produce false-positive compilation failure
tasks.withType<Kotlin2JsCompile>().configureEach {
    incremental = false
}