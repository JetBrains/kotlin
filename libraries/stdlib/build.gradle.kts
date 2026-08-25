@file:Suppress("UNUSED_VARIABLE", "NAME_SHADOWING", "DEPRECATION")
import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompilerOptions
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.mpp.GenerateProjectStructureMetadata
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages
import org.jetbrains.kotlin.gradle.targets.js.KotlinWasmTargetType
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsTargetDsl
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinTargetWithNodeJsDsl
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinWasmTargetDsl
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinWasmWasiTargetDsl
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrLink
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest
import org.jetbrains.kotlin.gradle.targets.wasm.wasmtime.WasmtimeEnvSpec
import org.jetbrains.kotlin.gradle.targets.wasm.wasmtime.WasmtimeSetupTask
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompile
import org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile
import org.jetbrains.kotlin.gradle.tasks.UsesKotlinJavaToolchain
import org.jetbrains.kotlin.library.KOTLIN_JS_STDLIB_NAME
import org.jetbrains.kotlin.library.KOTLIN_WASM_STDLIB_NAME
import plugins.configureDefaultPublishing
import plugins.configureKotlinPomAttributes
import plugins.publishing.configureMultiModuleMavenPublishing
import plugins.publishing.copyAttributes
import java.net.URI
import kotlin.io.path.copyTo

plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    kotlin("multiplatform")
    `maven-publish`
    id("signing-convention")
    id("d8-configuration")
    id("binaryen-configuration")
    id("nodejs-configuration")
    id("wasmtime-configuration")
    // HACK (KT-87723): needed to componentize the WASI test binary, see the block at the end of this file
    id("wasm-tools-configuration")
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
    // Use this to override language and API versions for stdlib compared to the version used to build the whole Kotlin
    // languageVersion = KotlinVersion.KOTLIN_...
    // apiVersion = KotlinVersion.KOTLIN_...
    freeCompilerArgs.add("-Xstdlib-compilation")
    freeCompilerArgs.add("-Xdont-warn-on-error-suppression")
    freeCompilerArgs.add("-Xcontext-parameters")
    freeCompilerArgs.add("-Xname-based-destructuring=complete")
    freeCompilerArgs.add("-Xcollection-literals")
    if (!kotlinBuildProperties.disableWerror) allWarningsAsErrors = true

    if (this is KotlinJvmCompilerOptions) {
        suppressRedundantCliArgumentWarning()
    }
}

fun KotlinCommonCompilerOptions.addReturnValueCheckerInfo() {
    freeCompilerArgs.add("-Xreturn-value-checker=full")
}

/**
 * Between making a language feature stable and the next bootstrap, we need to keep providing the compiler argument.
 * But this produces a warning
 * "The argument ... is redundant for the current language version ..."
 * in the bootstrap test and fails because of -Werror.
 * To work around it, we suppress the warning.
 */
fun KotlinCommonCompilerOptions.suppressRedundantCliArgumentWarning() {
    freeCompilerArgs.add("-Xwarning-level=REDUNDANT_CLI_ARG:disabled")
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
    "kotlin.time.ExperimentalTime",
)

kotlin {
    val renderDiagnosticNames = project.kotlinBuildProperties.renderDiagnosticNames.get()
    extra["renderDiagnosticNames"] = renderDiagnosticNames
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
                        addReturnValueCheckerInfo()
                        suppressRedundantCliArgumentWarning()
                    }
                }
            }
        }
    }
    jvm {
        compilations {
            val compileOnlyDeclarations = create("compileOnlyDeclarations") {
                compileTaskProvider.configure {
                    compilerOptions {
                        freeCompilerArgs.set(
                            listOfNotNull(
                                "-Xallow-kotlin-package",
                                "-Xsuppress-missing-builtins-error",
                                diagnosticNamesArg
                            )
                        )
                        suppressRedundantCliArgumentWarning()
                    }
                }
            }

            val main = getByName("main") {
                compileTaskProvider.configure {
                    // use os.arch as an input property of the compilation task
                    // to avoid resuing compilation results from the build cache
                    // produced on the other CPU architecture due to KT-53258
                    inputs.property("os.arch", providers.systemProperty("os.arch"))

                    this as UsesKotlinJavaToolchain
                    kotlinJavaToolchain.toolchain.use(getToolchainLauncherFor(JdkMajorVersion.JDK_11_0))
                    compilerOptions {
                        moduleName = "kotlin-stdlib"
                        jvmTarget = JvmTarget.JVM_1_8
                        // providing exhaustive list of args here
                        freeCompilerArgs.set(
                            listOfNotNull(
                                "-Xjdk-release=6",
                                "-jvm-default=disable",
                                "-Xallow-kotlin-package",
                                "-Xexpect-actual-classes",
                                "-Xmultifile-parts-inherit",
                                "-Xuse-14-inline-classes-mangling-scheme",
                                "-Xno-new-java-annotation-targets",
                                "-Xoutput-builtins-metadata",
                                diagnosticNamesArg
                            )
                        )
                        mainCompilationOptions()
                        addReturnValueCheckerInfo()
                    }
                }
                defaultSourceSet {
                    dependencies {
                        compileOnly(compileOnlyDeclarations.output.allOutputs)
                    }
                }
            }
            val mainJdk7 = create("mainJdk7") {
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
                                "-jvm-default=disable",
                                "-Xallow-kotlin-package",
                                "-Xexpect-actual-classes",
                                "-Xmultifile-parts-inherit",
                                "-Xno-new-java-annotation-targets",
                                "-Xexplicit-api=strict",
                                diagnosticNamesArg,
                            )
                        )
                        mainCompilationOptions()
                        addReturnValueCheckerInfo()
                    }
                }
            }
            val mainJdk8 = create("mainJdk8") {
                associateWith(main)
                associateWith(mainJdk7)
                compileTaskProvider.configure {
                    compilerOptions {
                        moduleName = "kotlin-stdlib-jdk8"
                        freeCompilerArgs.set(
                            listOfNotNull(
                                "-Xallow-kotlin-package",
                                "-jvm-default=disable",
                                "-Xmultifile-parts-inherit",
                                "-Xno-new-java-annotation-targets",
                                "-Xexplicit-api=strict",
                                diagnosticNamesArg,
                            )
                        )
                        mainCompilationOptions()
                        addReturnValueCheckerInfo()
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
            val test = getByName("test") {
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
            val longRunningTest = create("longRunningTest") {
                associateWith(main)
                associateWith(mainJdk7)
                associateWith(mainJdk8)
            }
            val recursiveDeletionTest = create("recursiveDeletionTest") {
                associateWith(main)
                associateWith(mainJdk7)
                associateWith(mainJdk8)
            }
        }
    }
    js {
        if (!kotlinBuildProperties.isTeamcityBuild.get()) {
            browser {}
        }
        nodejs {
            testTask {
                useMocha {
                    timeout = "10s"
                }
            }
        }

        compilerOptions {
            freeCompilerArgs.addAll(
                listOf(
                    "-Xallow-kotlin-package",
                    "-Xexpect-actual-classes",
                )
            )
        }

        compilations {
            val main = getByName("main") {
                compileTaskProvider.configure {
                    compilerOptions.mainCompilationOptions()
                    compilerOptions.freeCompilerArgs.addAll(
                        listOfNotNull(
                            "-Xir-module-name=$KOTLIN_JS_STDLIB_NAME",
                            diagnosticNamesArg,
                        )
                    )
                    compilerOptions.addReturnValueCheckerInfo()
                }
            }
        }
    }

    fun <T> T.commonWasmTargetConfiguration()
            where T : KotlinTargetWithNodeJsDsl,
                  T : KotlinWasmTargetDsl {
        // this is necessary because KotlinWasmTargetDsl does not extend HasConfigurableKotlinCompilerOptions<KotlinJsCompilerOptions>
        // upgrade after bootstrap
        // KT-85971
        this as KotlinJsTargetDsl
        if (this.wasmTargetType == KotlinWasmTargetType.JS) {
            nodejs()
        } else {
            this as KotlinWasmWasiTargetDsl
            @OptIn(ExperimentalWasmDsl::class)
            wasmtime()
        }
        compilerOptions {
            sourceMap = false
            sourceMapEmbedSources.unsetConvention()
            freeCompilerArgs.addAll(
                listOfNotNull(
                    "-Xallow-kotlin-package",
                    "-Xexpect-actual-classes",
                    diagnosticNamesArg
                )
            )
        }
        compilations {
            val main = getByName("main") {
                compileTaskProvider.configure {
                    compilerOptions.mainCompilationOptions()
                    compilerOptions.addReturnValueCheckerInfo()
                    compilerOptions.freeCompilerArgs.add("-Xir-module-name=$KOTLIN_WASM_STDLIB_NAME")
                }
            }
        }
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        commonWasmTargetConfiguration()
    }
    @OptIn(ExperimentalWasmDsl::class)
    wasmWasi {
        commonWasmTargetConfiguration()
    }

    // FIXME: KT-85818 Avoid using isInIdeaSync in stdlib/build.gradle.kts in kotlin.git
    if (kotlinBuildProperties.isInIdeaSync.get()) {
        val hostOs = System.getProperty("os.name")
        val isMingwX64 = hostOs.startsWith("Windows")
        val nativeTarget = when {
            hostOs == "Mac OS X" -> macosX64("native")
            hostOs == "Linux" -> linuxX64("native")
            isMingwX64 -> mingwX64("native")
            else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
        }
        nativeTarget.compilerOptions {
            freeCompilerArgs.addAll(
                listOf(
                    "-Xallow-kotlin-package",
                    "-Xexpect-actual-classes",
                    "-nostdlib",
                )
            )
        }
        nativeTarget.compilations["main"].compileTaskProvider.configure {
            compilerOptions.addReturnValueCheckerInfo()
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
            val prepareCommonSources = tasks.register("prepareCommonSources") {
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
                implementation(kotlinTest())
            }
            kotlin {
                srcDir("common/test")
                srcDir("test")
            }
        }
        val jvmCompileOnlyDeclarations = getByName("jvmCompileOnlyDeclarations") {
            kotlin.srcDir("jvm/compileOnly")
        }
        val jvmMain = getByName("jvmMain") {
            project.configurations.getByName("jvmMainCompileOnly")
            dependencies {
                api("org.jetbrains:annotations:13.0")
            }
            val jvmSrcDirs = listOfNotNull(
                "jvm/src",
                "jvm/runtime",
                "jvm/builtins",
            )
            project.sourceSets["jvmMain"].java.srcDirs(*jvmSrcDirs.toTypedArray())
            kotlin.setSrcDirs(jvmSrcDirs)
            kotlin.exclude("kotlin/internal/InternalAnnotations.kt")
        }

        val jvmMainJdk7 = getByName("jvmMainJdk7") {
            kotlin.srcDir("jdk7/src")
        }
        val jvmMainJdk8 = getByName("jvmMainJdk8") {
            kotlin.srcDir("jdk8/src")
        }

        val jvmTest = getByName("jvmTest") {
            languageSettings {
                optIn("kotlin.io.path.ExperimentalPathApi")
            }
            dependencies {
                implementation(kotlinTest("junit5"))
            }
            kotlin.srcDir("jvm/test")
            kotlin.srcDir("jdk7/test")
            kotlin.srcDir("jdk8/test")
        }

        val jvmLongRunningTest = getByName("jvmLongRunningTest") {
            dependencies {
                implementation(kotlinTest("junit5"))
            }
            kotlin.srcDir("jvm/testLongRunning")
        }

        val jvmRecursiveDeletionTest = getByName("jvmRecursiveDeletionTest") {
            dependencies {
                implementation(kotlinTest("junit5"))
            }
            kotlin.srcDir("jdk7/recursiveDeletionTest")
        }

        val commonNonJvmMain = create("commonNonJvmMain") {
            dependsOn(commonMain.get())
            kotlin.srcDir("common-non-jvm/src")
        }

        val webMain = create("webMain") {
            dependsOn(commonMain.get())
            kotlin {
                srcDir("common-js-wasmjs/src")
            }
        }

        val jsMain = getByName("jsMain") {
            dependsOn(webMain)
            dependsOn(commonNonJvmMain)
            val prepareJsIrMainSources = tasks.register("prepareJsIrMainSources", Sync::class)
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
                val ignoredFileNames = setOf("Atomics.kt", "AtomicArrays.kt")
                val jsBuiltins: FileCollection = layout.projectDirectory.dir("js/builtins").asFileTree
                val jvmBuiltins: FileCollection = layout.projectDirectory.dir("jvm/builtins").asFileTree
                val jsBuiltinsSrcDirFile = layout.buildDirectory.dir("src/js-builtin-sources")

                into(jsBuiltinsSrcDirFile)
                from(jvmBuiltins) {
                    into("kotlin")
                    ignoredFileNames.forEach {
                        exclude(it)
                    }
                    jsBuiltins.files.forEach {
                        exclude(it.name)
                    }
                }
            }
        }
        val jsTest = getByName("jsTest") {
            kotlin.srcDir("${jsDir}/test")
        }

        val nativeWasmMain = create("nativeWasmMain") {
            dependsOn(commonNonJvmMain)
            kotlin.srcDir("native-wasm/src")
        }

        val nativeWasmWasiMain = create("nativeWasmWasiMain") {
            dependsOn(nativeWasmMain)
            kotlin.srcDir("native-wasm/wasi")
        }

        val nativeWasmTest = create("nativeWasmTest") {
            dependsOn(commonTest.get())
            kotlin.srcDir("native-wasm/test")
        }

        val wasmCommonMain = create("wasmCommonMain") {
            dependsOn(nativeWasmMain)
            val prepareWasmBuiltinSources = tasks.register("prepareWasmBuiltinSources", Sync::class)
            kotlin {
                srcDir(prepareWasmBuiltinSources.requiredForImport())
                srcDir("wasm/builtins")
                srcDir("wasm/internal")
                srcDir("wasm/runtime")
                srcDir("wasm/src")
                srcDir("wasm/stubs")
            }
            prepareWasmBuiltinSources.configure {
                val wasmBuiltins: FileCollection = layout.projectDirectory.dir("wasm/builtins/kotlin/").asFileTree
                val jvmBuiltins: FileCollection = layout.projectDirectory.dir("jvm/builtins").asFileTree
                val wasmBuiltinsSrcDirFile = layout.buildDirectory.dir("src/wasm-builtin-sources")
                val excluded = listOf(
                    "Atomics.kt", "AtomicArrays.kt",
                    // Included with K/N collections
                    "Collections.kt", "Iterator.kt"
                )

                into(wasmBuiltinsSrcDirFile)
                from(jvmBuiltins) {
                    into("kotlin")
                    excluded.forEach {
                        exclude(it)
                    }
                    wasmBuiltins.files.forEach {
                        exclude(it.name)
                    }

                }
            }

        }
        val wasmCommonTest = create("wasmCommonTest") {
            dependsOn(nativeWasmTest)
            kotlin {
                srcDir("wasm/test")
            }
        }

        val wasmJsMain = getByName("wasmJsMain") {
            dependsOn(webMain)
            dependsOn(wasmCommonMain)
            kotlin {
                srcDir("wasm/js/builtins")
                srcDir("wasm/js/internal")
                srcDir("wasm/js/src")
            }
        }
        val wasmJsTest = getByName("wasmJsTest") {
            dependsOn(wasmCommonTest)
            kotlin {
                srcDir("wasm/js/test")
            }
        }
        val wasmWasiMain = getByName("wasmWasiMain") {
            dependsOn(wasmCommonMain)
            dependsOn(nativeWasmWasiMain)
            kotlin {
                srcDir("wasm/wasi/builtins")
                srcDir("wasm/wasi/internal")
                srcDir("wasm/wasi/src")
            }
            languageSettings {
                optIn("kotlin.wasm.unsafe.UnsafeWasmMemoryApi")
            }
        }
        val wasmWasiTest = getByName("wasmWasiTest") {
            dependsOn(wasmCommonTest)
            kotlin {
                srcDir("wasm/wasi/test")
            }
        }

        if (kotlinBuildProperties.isInIdeaSync.get()) {
            val nativeKotlinTestCommon = create("nativeKotlinTestCommon") {
                dependsOn(commonMain.get())
                val prepareKotlinTestCommonNativeSources = tasks.register("prepareKotlinTestCommonNativeSources", Sync::class) {
                    from("../kotlin.test/common/src/main/kotlin")
                    from("../kotlin.test/annotations-common/src/main/kotlin")
                    into(layout.buildDirectory.dir("src/native-kotlin-test-common-sources"))
                }

                kotlin {
                    srcDir(prepareKotlinTestCommonNativeSources.requiredForImport())
                }
            }
            val nativeMain = getByName("nativeMain") {
                dependsOn(nativeWasmMain)
                dependsOn(nativeWasmWasiMain)
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
            val nativeTest = getByName("nativeTest") {
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
            compilerOptions.freeCompilerArgs.add("-Xname-based-destructuring=complete")
            compilerOptions.freeCompilerArgs.add("-Xcollection-literals")
        }
    }
}

dependencies {
    val jvmMainApi = configurations.getByName("jvmMainApi")
    val metadataCompilationApi = configurations.getByName("metadataCompilationApi")

    // native target is declared only when "ideaSync" is on,
    // FIXME: KT-85818 Avoid using isInIdeaSync in stdlib/build.gradle.kts in kotlin.git
    val nativeMainApi = configurations.findByName("nativeMainApi") ?: configurations.dependencyScope("nativeMainApi").get()
    val nativeApiElements = configurations.findByName("nativeApiElements") ?: configurations.consumable("nativeApiElements").get()
    nativeApiElements.extendsFrom(nativeMainApi)

    constraints {
        // there is no dependency anymore from kotlin-stdlib to kotlin-stdlib-common,
        // but use this constraint to align it if another library brings it transitively
        jvmMainApi(project(":kotlin-stdlib-common"))
        metadataCompilationApi(project(":kotlin-stdlib-common"))
        nativeMainApi(project(":kotlin-stdlib-common"))
        // to avoid split package and duplicate classes on classpath after moving them from these artifacts in 1.8.0
        jvmMainApi("org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.8.0")
        jvmMainApi("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.8.0")
    }
}

tasks {
    val allMetadataJar by existing(Jar::class) {
        archiveClassifier = "all"
    }
    val commonMetadataJar by registering(Jar::class) {
        archiveAppendix.set("metadata")
        archiveExtension.set("klib")
    }
    kotlin.metadata().compilations.named { it == "commonMain" }.configureEach {
        commonMetadataJar.configure { from(output.allOutputs) }
    }

    val webMetadataJar by registering(Jar::class) {
        archiveAppendix.set("metadata-web")
        archiveExtension.set("klib")
    }
    kotlin.metadata().compilations.named { it == "webMain" }.configureEach {
        webMetadataJar.configure { from(output.allOutputs) }
    }

    val sourcesJar by existing(Jar::class) {
        archiveAppendix.set("metadata")
    }
    val jvmJar by existing(Jar::class) {
        duplicatesStrategy = DuplicatesStrategy.FAIL
        archiveAppendix.set(null as String?)
        manifestAttributes(manifest, "Main", multiRelease = true)
        manifest.attributes(mapOf("Implementation-Title" to "kotlin-stdlib"))
        from(kotlin.jvm().compilations["mainJdk7"].output.allOutputs)
        from(kotlin.jvm().compilations["mainJdk8"].output.allOutputs)
        from(project.sourceSets["java9"].output)
    }

    val jvmRearrangedSourcesJar by registering(Jar::class) {
        archiveClassifier.set("jvm-sources")
        archiveVersion.set("")
        destinationDirectory.set(layout.buildDirectory.dir("lib"))

        includeEmptyDirs = false
        duplicatesStrategy = DuplicatesStrategy.FAIL

        into("commonMain") {
            from(kotlin.sourceSets.commonMain.get().kotlin)
        }
        into("jvmMain") {
            from(kotlin.sourceSets["jvmMain"].kotlin) {
                // relocate builtins sources that get placed in the root of the sources file tree
                eachFile {
                    val sourcePathSegments = relativeSourcePath.segments
                    if (sourcePathSegments.size == 1) {
                        relativePath = RelativePath(true, "jvmMain", "kotlin", *sourcePathSegments)
                    }
                }
            }
            from(kotlin.sourceSets["jvmMainJdk7"].kotlin) {
                into("jdk7")
            }
            from(kotlin.sourceSets["jvmMainJdk8"].kotlin) {
                into("jdk8")
            }
        }
    }

    val jvmSourcesJar by existing(Jar::class) {
        duplicatesStrategy = DuplicatesStrategy.FAIL
        archiveAppendix.set(null as String?)

        val jvmSourcesJarFile = jvmRearrangedSourcesJar.get().archiveFile
        inputs.file(jvmSourcesJarFile)
        doLast {
            jvmSourcesJarFile.get().asFile.toPath().copyTo(archiveFile.get().asFile.toPath(), overwrite = true)
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
        into(rootProject.isolated.projectDirectory.dir("build/js-ir-runtime"))
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
        val distWasmJsKlib = configurations.create("distWasmJsKlib")
        val distWasmWasiKlib = configurations.create("distWasmWasiKlib")
        val commonMainMetadataElements = configurations.create("commonMainMetadataElements")
        val webMainMetadataElements = configurations.create("webMainMetadataElements")

        add(distJsSourcesJar.name, jsSourcesJar)
        add(distJsKlib.name, jsJar)
        add(distWasmJsKlib.name, wasmJsJar)
        add(distWasmWasiKlib.name, wasmWasiJar)
        add(webMainMetadataElements.name, webMetadataJar)
        add(commonMainMetadataElements.name, commonMetadataJar)
    }


    val jvmTest by existing(Test::class)

    listOf(JdkMajorVersion.JDK_11_0, JdkMajorVersion.JDK_17_0, JdkMajorVersion.JDK_25_0).forEach { jvmVersion ->
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
            compilerOptions.freeCompilerArgs.add("-Xwasm-enable-array-range-checks")
        }
        named("compileTestProductionExecutableKotlinWasm$wasmTarget", KotlinJsIrLink::class) {
            enabled = false  // Causes out-of-memory in CI: KTI-2150
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
        if (!kotlinBuildProperties.isInIdeaSync.get()) {
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

    val jvmRecursiveDeletionTestTmpDir = layout.buildDirectory.asFile.map {
        it.toPath().resolve("recursiveDeletionTestsWorkDir")
    }

    val jvmRecursiveDeletionTestCleanup by registering(Delete::class) {
        setDelete(jvmRecursiveDeletionTestTmpDir)
    }

    // A dedicated task for tests on files and directories deletion from the current working directory.
    // To prevent (to some extent) accidental removal of surrounding files and directories when tested functions
    // are malfunctioning, this task gets its own working directory where removal will take place.
    val jvmRecursiveDeletionTest by registering(Test::class) {
        group = "verification"
        val compilation = kotlin.jvm().compilations["recursiveDeletionTest"]

        testClassesDirs = compilation.output.classesDirs
        classpath = compilation.compileDependencyFiles + compilation.runtimeDependencyFiles + compilation.output.allOutputs

        doFirst {
            workingDir = jvmRecursiveDeletionTestTmpDir.get().toFile()
            workingDir.deleteRecursively()
            workingDir.mkdirs()
        }
        finalizedBy(jvmRecursiveDeletionTestCleanup)
    }
    check.configure { dependsOn(jvmRecursiveDeletionTest) }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}


// region ==== Publishing ====

configureDefaultPublishing()


val emptyJavadocJar = tasks.create("emptyJavadocJar", org.gradle.api.tasks.bundling.Jar::class) {
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

// =====================================================================================================================
// HACK (KT-87723): run the stdlib WASI tests as a WASI 0.2 *component* instead of a core module.
//
// Quick-and-dirty stand-in for the future KGP integration, only wired into `wasmWasiWasmtimeTest`:
//   1. the compiler-generated unit test entry point export `startUnitTests` is renamed to the kebab-case WIT name
//      `run-unit-tests` (both are exactly 14 bytes, so this is a plain byte patch of the linked .wasm). WIT and the
//      component model only allow kebab-case names, and `wasmtime --invoke` parses WAVE, which rejects camelCase.
//   2. `wasm-tools component embed` embeds a *test-only* world: same imports as `wasip2`, but exporting
//      `run-unit-tests` instead of `wasi:cli/run`, because test binaries have no `main`.
//   3. `wasm-tools component new` builds the component; the `wasi_snapshot_preview1` `args_get`/`args_sizes_get`
//      imports that kotlin-test still uses for the test CLI arguments are satisfied by the preview1 adapter. The
//      stock reactor adapter is used by default (the freelist allocators no longer hand out memory that the adapter
//      still holds on to); pass -Pkotlin.wasm.wasiAdapterPath=... to use a customized one.
//   4. the resulting component overwrites the linked .wasm in place (so no KGP change is needed for the test input),
//      and the wasmtime executable is replaced by a wrapper script that rewrites `--invoke startUnitTests` into
//      `--invoke run-unit-tests()`.
//
// Needs `wasm-tools` on PATH (or -Pkotlin.wasm.wasmToolsPath=/path/to/wasm-tools), and network access on the first run
// (to fetch the preview1 adapter). Unix only. Disable with -Pkotlin.stdlib.wasi.componentHack=false.
// Delete this whole block once KGP produces components properly.
// =====================================================================================================================
@OptIn(ExperimentalWasmDsl::class)
run {
    val hackEnabled = providers.gradleProperty("kotlin.stdlib.wasi.componentHack").orNull != "false"
    if (!hackEnabled || org.gradle.internal.os.OperatingSystem.current().isWindows) return@run

    val entryPointCoreName = "startUnitTests"
    val entryPointWitName = "run-unit-tests" // same length as above on purpose: allows a plain byte patch
    val testWorld = "kotlin-stdlib:wasip2/wasip2-test"

    val wasmtimeSpec = the<WasmtimeEnvSpec>()
    // resolved before `command` is overridden below, so this is the real wasmtime binary
    val realWasmtime = File(wasmtimeSpec.executable.get())

    val wasmtimeVersion = wasmtimeSpec.version.get()
    val customAdapter = providers.gradleProperty("kotlin.wasm.wasiAdapterPath").map { File(it) }.orNull
    val stdlibWitDir = layout.projectDirectory.dir("wasm/wasi/internal/wit").asFile
    val hackDir = layout.buildDirectory.dir("wasip2-component-hack").get().asFile

    // The wrapper lives in the build directory (and not next to the real wasmtime, which is a task output directory
    // that gets wiped on re-extraction) and is (re)written by a task, so that it survives configuration cache hits.
    val wrapper = hackDir.resolve("wasmtime-component-hack.sh")
    val wrapperText =
        """
        #!/usr/bin/env bash
        # Generated by libraries/stdlib/build.gradle.kts (KT-87723 hack) - safe to delete.
        set -eu
        real="${realWasmtime.absolutePath}"
        if [ ! -x "${'$'}real" ]; then
          echo "component hack wrapper: ${'$'}real is missing (wasmtime version bumped?);" \
               "re-run with --no-configuration-cache" >&2
          exit 1
        fi
        args=()
        for arg in "${'$'}@"; do
          if [ "${'$'}arg" = "$entryPointCoreName" ]; then
            args+=("$entryPointWitName()")
          else
            args+=("${'$'}arg")
          fi
        done
        exec "${'$'}real" "${'$'}{args[@]}"
        """.trimIndent()

    val writeWasmtimeWrapper = tasks.register("writeWasmtimeComponentHackWrapper") {
        val wrapperFile = wrapper
        val text = wrapperText
        outputs.upToDateWhen { false }
        doLast {
            wrapperFile.parentFile.mkdirs()
            wrapperFile.writeText(text)
            wrapperFile.setExecutable(true)
        }
    }

    // absolute path, so the install directory of wasmtime is left alone entirely
    wasmtimeSpec.command.set(wrapper.absolutePath)

    // WasmtimeSetupTask chmod's `command` after extracting the distribution, so the wrapper has to exist by then
    tasks.withType<WasmtimeSetupTask>().configureEach {
        dependsOn(writeWasmtimeWrapper)
    }

    tasks.named<KotlinJsTest>("wasmWasiWasmtimeTest") {
        dependsOn(writeWasmtimeWrapper)
        val wasmToolsExecutablePath = with(wasmToolsKotlinBuild) { useWasmTools() }
        val testModule = inputFileProperty
        val taskLogger = logger

        doFirst {
            val wasmToolsExecutable = wasmToolsExecutablePath.get()

            fun runCommand(vararg command: String) {
                val process = ProcessBuilder(*command).redirectErrorStream(true).start()
                val output = process.inputStream.bufferedReader().readText()
                val exitCode = process.waitFor()
                check(exitCode == 0) { "Command failed with exit code $exitCode:\n${command.joinToString(" ")}\n$output" }
                if (output.isNotBlank()) taskLogger.info(output)
            }

            fun ByteArray.containsAscii(value: String): Boolean {
                val valueBytes = value.toByteArray(Charsets.US_ASCII)
                var i = 0
                outer@ while (i <= size - valueBytes.size) {
                    for (j in valueBytes.indices) {
                        if (this[i + j] != valueBytes[j]) {
                            i++
                            continue@outer
                        }
                    }
                    return true
                }
                return false
            }

            fun ByteArray.patchAscii(from: String, to: String): Int {
                require(from.length == to.length)
                val fromBytes = from.toByteArray(Charsets.US_ASCII)
                val toBytes = to.toByteArray(Charsets.US_ASCII)
                var patched = 0
                var i = 0
                outer@ while (i <= size - fromBytes.size) {
                    for (j in fromBytes.indices) {
                        if (this[i + j] != fromBytes[j]) {
                            i++
                            continue@outer
                        }
                    }
                    toBytes.copyInto(this, i)
                    patched++
                    i += fromBytes.size
                }
                return patched
            }

            val linkedFile = testModule.get().asFile
            hackDir.mkdirs()

            // 0. the linked file is overwritten with the component below, so keep the pristine core module around:
            //    that way changes to the adapter or to the WIT are picked up without having to relink
            val coreModule = hackDir.resolve("test-module-core.wasm")
            val linkedBytes = linkedFile.readBytes()
            // core module header: 00 61 73 6d 01 00 00 00, component header: 00 61 73 6d 0d 00 01 00
            val linkedIsCore = linkedBytes.size >= 8 && linkedBytes[4] == 0x01.toByte() && linkedBytes[6] == 0x00.toByte()
            if (linkedIsCore) linkedFile.copyTo(coreModule, overwrite = true)
            check(coreModule.exists()) {
                "${linkedFile.absolutePath} is already a component and ${coreModule.absolutePath} is missing; " +
                        "delete the former and re-run to get a freshly linked core module"
            }
            val bytes = if (linkedIsCore) linkedBytes else coreModule.readBytes()

            // 1. rename the unit test entry point export to a name that is valid in WIT
            val patchedCount = bytes.patchAscii(entryPointCoreName, entryPointWitName)
            check(patchedCount > 0) { "Export '$entryPointCoreName' not found in ${linkedFile.absolutePath}" }
            val renamedModule = hackDir.resolve("test-module-renamed.wasm")
            renamedModule.writeBytes(bytes)

            // 2. the stdlib WIT plus a test-only world exporting the unit test entry point instead of wasi:cli/run
            val witDir = hackDir.resolve("wit")
            witDir.deleteRecursively()
            stdlibWitDir.copyRecursively(witDir)
            witDir.resolve("zz-test-world-hack.wit").writeText(
                """
                package kotlin-stdlib:wasip2@2.5.0;

                // HACK (KT-87723): `wasip2`, but exporting the unit test entry point instead of `wasi:cli/run`,
                // since stdlib test binaries have no `main`.
                world wasip2-test {
                    include wasi:io/imports@0.2.12;
                    include wasi:random/imports@0.2.12;
                    include wasi:clocks/imports@0.2.12;
                    include wasi:cli/imports@0.2.12;

                    export $entryPointWitName: func();
                }
                """.trimIndent()
            )

            // 3. a preview1 adapter is only needed while something still imports `wasi_snapshot_preview1`;
            //    a WASI 0.2 native module is componentized as is
            val needsAdapter = bytes.containsAscii("wasi_snapshot_preview1")
            val adapter = if (!needsAdapter) null else {
                (customAdapter ?: hackDir.resolve("wasi_snapshot_preview1.reactor.wasm").also { downloaded ->
                    if (!downloaded.exists()) {
                        val url = "https://github.com/bytecodealliance/wasmtime/releases/download/" +
                                "v$wasmtimeVersion/wasi_snapshot_preview1.reactor.wasm"
                        taskLogger.lifecycle("Downloading $url")
                        URI(url).toURL().openStream().use { input ->
                            downloaded.outputStream().buffered().use { output -> input.copyTo(output) }
                        }
                    }
                }).also { check(it.isFile) { "preview1 adapter ${it.absolutePath} does not exist" } }
            }
            taskLogger.lifecycle(adapter?.let { "Using preview1 adapter ${it.absolutePath}" } ?: "No preview1 adapter needed")

            val embeddedModule = hackDir.resolve("test-module-embedded.wasm")
            val component = hackDir.resolve("test-component.wasm")
            runCommand(
                wasmToolsExecutable, "component", "embed", witDir.absolutePath, renamedModule.absolutePath,
                "--world", testWorld, "-o", embeddedModule.absolutePath,
            )
            runCommand(
                wasmToolsExecutable, "component", "new", embeddedModule.absolutePath,
                *adapter?.let { arrayOf("--adapt", "wasi_snapshot_preview1=${it.absolutePath}") } ?: emptyArray(),
                "-o", component.absolutePath,
            )

            // 4. run the tests on the component instead of on the core module
            component.copyTo(linkedFile, overwrite = true)
            taskLogger.lifecycle("Componentized ${linkedFile.absolutePath} (world $testWorld)")
        }
    }
}
