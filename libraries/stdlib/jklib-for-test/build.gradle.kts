description = "Kotlin JKlib Stdlib for Tests"

plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    base
}

jvmToolchains {
    targetBytecodeVersion = JdkMajorVersion.JDK_1_8
}

val stdlibProjectDir = file("$rootDir/libraries/stdlib")

val jklibCompilerClasspath = configurations.create("jklibCompilerClasspath") {
    isCanBeConsumed = false
    isCanBeResolved = true
}

val substrateStdlibCompilerDependencies = configurations.create("substrateStdlibCompilerDependencies") {
    isCanBeConsumed = false
    isCanBeResolved = true
}

dependencies {
    // 1. Dependencies to RUN the JKlib Compiler
    jklibCompilerClasspath(project(":compiler:cli-jklib"))
    jklibCompilerClasspath(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) {
        isTransitive = false
    }
    substrateStdlibCompilerDependencies(intellijCore())

    // Transitive dependencies pulled by IntellijCore
    // Required by 'NoopTelemetryManager', queried through 'CoreProgressManager'
    substrateStdlibCompilerDependencies(libs.opentelemetry.api)
    // Used for IR interning and seriliazation and other things
    substrateStdlibCompilerDependencies(libs.intellij.fastutil)
    // Used to read XML metadata files inside META-INF
    substrateStdlibCompilerDependencies(commonDependency("org.codehaus.woodstox:stax2-api"))
    substrateStdlibCompilerDependencies(commonDependency("com.fasterxml:aalto-xml"))
}

val outputKlib = layout.buildDirectory.file("libs/kotlin-stdlib-jklib-for-test.klib")

// The stdlib is compiled from the same sources as the main compilation of the JVM `kotlin-stdlib`: all the common sources,
// and the JVM builtins and actuals. JKlib only compiles Kotlin, so the Java part of the JVM runtime (`jvm/runtime/**/*.java`)
// is left out and the few declarations of it that Kotlin sources reference are stubbed in `src/runtime-stubs`. The jdk7 and
// jdk8 compilations are left out too.
val copyStdlibSources = tasks.register("copyStdlibSources", Sync::class) {
    dependsOn(":prepare:build.version:writeStdlibVersion")
    into(layout.buildDirectory.dir("src/stdlib"))

    from(stdlibProjectDir.resolve("src")) {
        include("**/*.kt")
        into("src/common/src")
    }
    from(stdlibProjectDir.resolve("common/src")) {
        include("**/*.kt")
        into("src/common/common-src")
    }
    from(stdlibProjectDir.resolve("unsigned/src")) {
        include("**/*.kt")
        into("src/common/unsigned-src")
    }
    from(stdlibProjectDir.resolve("jvm/builtins")) {
        include("**/*.kt")
        into("src/jvm/builtins")
    }
    from(stdlibProjectDir.resolve("jvm/src")) {
        include("**/*.kt")
        into("src/jvm/src")
    }
    from(stdlibProjectDir.resolve("jvm/runtime")) {
        include("**/*.kt")
        // Only declares subclasses of the Java `PropertyReference0` and `MutablePropertyReference0`.
        exclude("kotlin/jvm/internal/localVariableReferences.kt")
        // Replaced by stubs in `src/runtime-stubs`: its hidden `KClass<T>.java` overload has the same KLIB signature as
        // `KClass<T & Any>.java`, since they only differ by their `@JvmName` and a definitely non-null type argument.
        exclude("kotlin/jvm/JvmClassMapping.kt")
        into("src/jvm/runtime")
    }
    from("src/runtime-stubs") {
        include("**/*.kt")
        into("src/jvm/runtime-stubs")
    }

    duplicatesStrategy = DuplicatesStrategy.FAIL
}

fun JavaExec.configureJklibCompilation(
    sourceTask: TaskProvider<Sync>,
    klibOutput: Provider<RegularFile>,
) {
    dependsOn(sourceTask)

    val jklibCompilerClasspath = project.configurations.getByName("jklibCompilerClasspath")
    val substrateStdlibCompilerDependencies = project.configurations.getByName("substrateStdlibCompilerDependencies")
    classpath = jklibCompilerClasspath + substrateStdlibCompilerDependencies
    mainClass.set("org.jetbrains.kotlin.cli.jklib.K2JKlibCompiler")

    val sourceTree = fileTree(sourceTask.map { it.destinationDir }) {
        include("**/*.kt")
    }
    inputs.files(sourceTree)
    inputs.files(jklibCompilerClasspath)
        .withNormalizer(ClasspathNormalizer::class)
        .withPropertyName("jklibCompilerClasspath")
    outputs.file(klibOutput)

    doFirst {
        val allFiles = sourceTree.files

        val commonPathSegment = "${File.separator}common${File.separator}"
        val (commonSourceFiles, jvmSourceFiles) = allFiles
            .map { it.absolutePath }
            .partition { it.contains(commonPathSegment) }

        logger.lifecycle("Compiling ${jvmSourceFiles.size} JVM files and ${commonSourceFiles.size} Common files, total ${allFiles.size}")
        logger.lifecycle("Running K2JKlibCompiler with Java version: ${System.getProperty("java.version")}")

        val outputPath = outputs.files.singleFile.absolutePath

        args(
            "-no-stdlib",
            "-Xallow-kotlin-package",
            "-Xexpect-actual-classes",
            "-module-name", "kotlin-stdlib",
            "-Xstdlib-compilation",
            "-d", outputPath,
            "-Xmulti-platform",
            "-opt-in=kotlin.contracts.ExperimentalContracts",
            "-opt-in=kotlin.ExperimentalMultiplatform",
            "-opt-in=kotlin.contracts.ExperimentalExtendedContracts",
            "-opt-in=kotlin.ExperimentalStdlibApi",
            "-Xmultifile-parts-inherit",
            *dogfoodedExperimentalFeatures.toTypedArray(),
            "-Xreturn-value-checker=full",
            "-Xcompanion-blocks",
            "-Xcommon-sources=${(commonSourceFiles).joinToString(",")}",
        )

        args(jvmSourceFiles)
        args(commonSourceFiles)
    }
}

val compileStdlib = tasks.register("compileStdlib", JavaExec::class) {
    val javaToolchains = project.extensions.getByType(JavaToolchainService::class.java)
    javaLauncher.set(javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(17))
    })
    configureJklibCompilation(copyStdlibSources, outputKlib)

    args("-nowarn")
}

val distJKlib = configurations.create("distJKlib") {
    isCanBeConsumed = true
    isCanBeResolved = false
}

artifacts {
    add(distJKlib.name, outputKlib) {
        builtBy(compileStdlib)
    }
}
