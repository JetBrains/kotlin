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

dependencies {
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
}

val outputKlib = layout.buildDirectory.file("libs/kotlin-stdlib-jvm-ir.klib")

val copyMinimalSources by tasks.registering(Sync::class) {
    dependsOn(":prepare:build.version:writeStdlibVersion")
    into(layout.buildDirectory.dir("src/genesis-minimal"))

    from("src/stubs") {
        include("kotlin/**")
        include("kotlin/util/**")
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
            "kotlin/CharSequence.kt",
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
            "kotlin/concurrent/atomics/AtomicArrays.common.kt",
            "kotlin/concurrent/atomics/Atomics.common.kt",
            "kotlin/contextParameters/Context.kt",
            "kotlin/contextParameters/ContextOf.kt",
            "kotlin/contracts/ContractBuilder.kt",
            "kotlin/contracts/Effect.kt",
        )
        into("src/common")
    }

    from(stdlibProjectDir.resolve("common/src")) {
        include(
            "kotlin/ExceptionsH.kt",
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
            "builtins/*.kt"
        )
        exclude(
            "builtins/Char.kt",
            "builtins/Collections.kt"
        )
        into("src/jvm")
    }

    from(stdlibProjectDir.resolve("jvm-minimal-for-test/jvm-src")) {
        include(
            "minimalAtomics.kt",
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
        val commonFiles = allFiles.filter { it.path.contains(commonPathSegment) }
        val jvmFiles = allFiles.filter { !it.path.contains(commonPathSegment) }

        val jvmSourceFiles = jvmFiles.map { it.absolutePath }
        val commonSourceFiles = commonFiles.map { it.absolutePath }

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
            "-Xcompile-builtins-as-part-of-stdlib",
            "-Xreturn-value-checker=full",
            "-Xcommon-sources=${(commonSourceFiles).joinToString(",")}",
        )


        val fullClasspath = klibCompileClasspath.files
            .filter { it.extension == "jar" && !it.name.contains("sources") }
            .map { it.absolutePath }
            .joinToString(File.pathSeparator)

        args("-classpath", fullClasspath)
        args(jvmSourceFiles)
        args(commonSourceFiles)
    }
}

val fullStdlibJar by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
        attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.JAR))
        attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.LIBRARY))
    }
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
    fullStdlibJar(project(":kotlin-stdlib"))

    klibCompileClasspath(project(":kotlin-stdlib"))
    klibCompileClasspath(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) {
        isTransitive = false
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
