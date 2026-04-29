description = "Kotlin JKlib Stdlib for Tests"

plugins {
    kotlin("jvm")
    base
}

project.configureJvmToolchain(JdkMajorVersion.JDK_1_8)

val stdlibProjectDir = file("$rootDir/libraries/stdlib")

val jklibCompilerClasspath by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
}

val substrateStdlibCompilerDependencies by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
}

val klibCompileClasspath by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
        attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.JAR))
        attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.LIBRARY))
    }
}

dependencies {
    // 1. Dependencies to RUN the JKlib Compiler
    jklibCompilerClasspath(project(":compiler:cli-jklib"))
    jklibCompilerClasspath(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) {
        isTransitive = false
    }
    substrateStdlibCompilerDependencies(intellijCore())

    // Transitive dependencies pulled by IntellijCore
    // Used for IR interning and seriliazation and other things
    substrateStdlibCompilerDependencies(libs.intellij.fastutil)
    // Used to read XML metadata files inside META-INF
    substrateStdlibCompilerDependencies(commonDependency("org.codehaus.woodstox:stax2-api"))
    substrateStdlibCompilerDependencies(commonDependency("com.fasterxml:aalto-xml"))

    // 2. Dependencies to COMPILE the minimal stdlib KLIB
    klibCompileClasspath(project(":kotlin-stdlib"))
    klibCompileClasspath(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) {
        isTransitive = false
    }
}

val outputKlib = layout.buildDirectory.file("libs/kotlin-stdlib-jvm-ir.klib")

val copyMinimalSources by tasks.registering(Sync::class) {
    dependsOn(":prepare:build.version:writeStdlibVersion")
    into(layout.buildDirectory.dir("src/genesis-minimal"))

    from("src/stubs") {
        include("kotlin/**")
        into("src/common")
    }

    from(stdlibProjectDir.resolve("jvm-minimal-for-test/common-src")) {
        include(
            "EnumEntries.kt",
            "Serializable.kt",
            "minimalCollections.kt"
        )
        into("src/common")
    }

    from(stdlibProjectDir.resolve("src")) {
        include(
            "kotlin/Annotation.kt",
            "kotlin/Annotations.kt",
            "kotlin/Any.kt",
            "kotlin/Array.kt",
            "kotlin/ArrayIntrinsics.kt",
            "kotlin/Arrays.kt",
            "kotlin/Boolean.kt",
            "kotlin/Char.kt",
            "kotlin/CharSequence.kt",
            "kotlin/Collections.kt",
            "kotlin/Comparable.kt",
            "kotlin/Enum.kt",
            "kotlin/Function.kt",
            "kotlin/Iterator.kt",
            "kotlin/Library.kt",
            "kotlin/Nothing.kt",
            "kotlin/Number.kt",
            "kotlin/String.kt",
            "kotlin/Throwable.kt",
            "kotlin/Primitives.kt",
            "kotlin/Unit.kt",
            "kotlin/annotation/Annotations.kt",
            "kotlin/annotations/Multiplatform.kt",
            "kotlin/annotations/WasExperimental.kt",
            "kotlin/annotations/ReturnValue.kt",
            "kotlin/internal/Annotations.kt",
            "kotlin/internal/AnnotationsBuiltin.kt",
            "kotlin/internal/progressionUtil.kt",
            "kotlin/concurrent/atomics/AtomicArrays.common.kt",
            "kotlin/concurrent/atomics/Atomics.common.kt",
            "kotlin/contextParameters/Context.kt",
            "kotlin/contextParameters/ContextOf.kt",
            "kotlin/contracts/ContractBuilder.kt",
            "kotlin/contracts/Effect.kt",
            "kotlin/util/Standard.kt",
            "kotlin/annotations/Annotations.kt",
            "kotlin/concurrent/atomics/ExperimentalAtomicApi.kt",
            "kotlin/annotations/ExperimentalStdlibApi.kt",
        )
        into("src/common")
    }

    from(stdlibProjectDir.resolve("common/src")) {
        include(
            "kotlin/ExceptionsH.kt",
            "kotlin/JvmAnnotationsH.kt"
        )
        into("src/common")
    }

    from("src/stubs/jvm/builtins") {
        include("**")
        into("src/jvm")
    }

    from(stdlibProjectDir.resolve("jvm")) {
        include(
            "runtime/kotlin/NoWhenBranchMatchedException.kt",
            "runtime/kotlin/UninitializedPropertyAccessException.kt",
            "runtime/kotlin/TypeAliases.kt",
            "runtime/kotlin/text/TypeAliases.kt",
            "src/kotlin/ArrayIntrinsics.kt",
            "src/kotlin/Unit.kt",
            "src/kotlin/collections/TypeAliases.kt",
            "src/kotlin/enums/EnumEntriesJVM.kt",
            "src/kotlin/io/Serializable.kt",
            "builtins/*.kt",
            "src/kotlin/jvm/Annotations.kt",
            "src/kotlin/reflect/KDeclarationContainer.kt",
            "runtime/kotlin/jvm/internal/Lambda.kt",
            "runtime/kotlin/jvm/internal/FunctionBase.kt",
            "runtime/kotlin/jvm/annotations/JvmPlatformAnnotations.kt",
        )
        into("src/jvm")
    }

    from(stdlibProjectDir.resolve("jvm-minimal-for-test/jvm-src")) {
        include(
            "minimalAtomics.kt",
            "minimalCollections.kt",
            "minimalThrowables.kt",
        )
        into("src/jvm")
    }

    duplicatesStrategy = DuplicatesStrategy.FAIL
}

fun JavaExec.configureJklibCompilation(
    sourceTask: TaskProvider<Sync>,
    klibOutput: Provider<RegularFile>,
    klibCompileClasspath: FileCollection,
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
    inputs.files(klibCompileClasspath)
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
            "-Xreturn-value-checker=full",
            "-Xcommon-sources=${(commonSourceFiles).joinToString(",")}",
        )

        val fullClasspath = klibCompileClasspath.asPath
        
        args("-classpath", fullClasspath)
        args(jvmSourceFiles)
        args(commonSourceFiles)
    }
}



val compileStdlib by tasks.registering(JavaExec::class) {
    val javaToolchains = project.extensions.getByType(JavaToolchainService::class.java)
    javaLauncher.set(javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(8))
    })
    configureJklibCompilation(copyMinimalSources, outputKlib, klibCompileClasspath)

    args("-nowarn")
}

val compileMinimalStdlib by tasks.registering {
    dependsOn(compileStdlib)
}

val distJKlib by configurations.creating {
    isCanBeConsumed = true
    isCanBeResolved = false
}
val distMinimalJKlib by configurations.creating {
    isCanBeConsumed = true
    isCanBeResolved = false
    extendsFrom(distJKlib)
}

artifacts {
    add(distJKlib.name, outputKlib) {
        builtBy(compileStdlib)
    }
}
