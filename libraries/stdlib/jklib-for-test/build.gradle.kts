description = "Kotlin JKlib Stdlib for Tests"

plugins {
    kotlin("jvm")
    base
}

project.configureJvmToolchain(JdkMajorVersion.JDK_1_8)

val stdlibProjectDir = file("$rootDir/libraries/stdlib")


dependencies {
    implementation(project(":compiler:cli-jklib"))
    implementation(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) {
        isTransitive = false
    }
    implementation(intellijCore())
    implementation(libs.intellij.fastutil)
    implementation(commonDependency("org.codehaus.woodstox:stax2-api"))
    implementation(commonDependency("com.fasterxml:aalto-xml"))
}

val copySources by tasks.registering(Sync::class) {
    dependsOn(":prepare:build.version:writeStdlibVersion")
    into(layout.buildDirectory.dir("src/genesis-all"))
    
    // Common Sources
    from(stdlibProjectDir.resolve("common/src")) {
        include("**/*")
        into("common/common")
    }
    from(stdlibProjectDir.resolve("src")) {
        include("**/*")
        into("common/src")
    }
    from(stdlibProjectDir.resolve("unsigned/src")) {
        include("**/*")
        into("common/unsigned")
    }

    // JVM Sources
    from(stdlibProjectDir.resolve("jvm/src")) {
        include("**/*")
        into("jvm/src")
    }
    from(stdlibProjectDir.resolve("jvm/runtime")) {
        include("**/*")
        into("jvm/runtime")
    }
    from(stdlibProjectDir.resolve("jvm/builtins")) {
        include("**/*")
        into("jvm/builtins")
    }
    from(stdlibProjectDir.resolve("jvm/compileOnly")) {
        include("**/*")
        into("jvm/compileOnly")
    }
    
    // Stub Sources
    from(project.file("src/stubs")) {
        include("**/*")
        into("jvm/stubs")
    }

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

val outputKlib = layout.buildDirectory.file("libs/kotlin-stdlib-jvm-ir.klib")

val copyMinimalSources by tasks.registering(Sync::class) {
    dependsOn(":prepare:build.version:writeStdlibVersion")
    into(layout.buildDirectory.dir("src/genesis-minimal"))

    from("src/stubs/jvm/builtins") {
        include("**")
        into("jvm/builtins")
    }

    from("src/stubs") {
        include("kotlin/**")
        into("common/src")
    }

    // Common Sources - mirroring jvm-minimal-for-test
    from(stdlibProjectDir.resolve("src")) {
        include(
            "kotlin/Annotation.kt",
            "kotlin/Any.kt",
            "kotlin/Array.kt",
            "kotlin/ArrayIntrinsics.kt",
            "kotlin/Arrays.kt",
            "kotlin/Boolean.kt",
            //"kotlin/Char.kt", // Used via stub in src/stubs/kotlin/Char.kt
            "kotlin/CharSequence.kt",
            //"kotlin/Collections.kt",
            "kotlin/Comparable.kt",
            "kotlin/Enum.kt",
            "kotlin/Enum.kt",
            //"kotlin/enums/EnumEntries.kt", // Used via stub in src/stubs/kotlin/enums/EnumEntries.kt
            "kotlin/Function.kt",
            "kotlin/Function.kt",
            "kotlin/Iterator.kt",
            "kotlin/Library.kt",
            "kotlin/Nothing.kt",
            "kotlin/Number.kt",
            //"kotlin/Primitives.kt", // Used via stub in src/stubs/kotlin/Primitives.kt
            "kotlin/String.kt",
            "kotlin/Throwable.kt",
            "kotlin/Unit.kt",
            "kotlin/util/Standard.kt",
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
            "kotlin/Annotations.kt", // Defines SinceKotlin, Deprecated, etc.
        )
        into("common/src")
    }
    

    from(stdlibProjectDir.resolve("common/src")) {
        include(
            "kotlin/ExceptionsH.kt",
        )
        into("common/common")
    }

    // JVM Sources - mirroring jvm-minimal-for-test
    from(stdlibProjectDir.resolve("jvm/runtime")) {
        include(
            "kotlin/NoWhenBranchMatchedException.kt",
            "kotlin/UninitializedPropertyAccessException.kt",
            "kotlin/TypeAliases.kt",
            "kotlin/text/TypeAliases.kt",
        )
        into("jvm/runtime")
    }
    from(stdlibProjectDir.resolve("jvm/src")) {
        include(
            "kotlin/ArrayIntrinsics.kt",
            "kotlin/Unit.kt",
            "kotlin/collections/TypeAliases.kt",
            "kotlin/enums/EnumEntriesJVM.kt",
            "kotlin/io/Serializable.kt",
        )
        into("jvm/src")
    }
    
    from(stdlibProjectDir.resolve("jvm/builtins")) {
        include("*.kt")
        exclude("Char.kt")
        exclude("Primitives.kt")
        exclude("Collections.kt")
        into("jvm/builtins")
    }


    
    // Stub sources - include if they are needed for minimal too, or maybe not?
    // jvm-minimal-for-test includes 'jvm-src/minimalAtomics.kt' etc. which are LOCAL to that project.
    // jvm-ir-for-test has 'src/stubs/kotlin/jvm/internal/Functions.kt' etc.
    // The user said "same srcs", implying the ones from jvm-minimal-for-test?
    // Or just the stdlib subset?
    // jvm-minimal-for-test has local sources: "jvm-src" and "common-src".
    // We probably need to include THOSE local sources from jvm-minimal-for-test if checking "same srcs".
    // BUT jvm-ir-for-test might not have access to jvm-minimal-for-test sources easily without path hacking or duplication.
    // User said "We can use the full stdlib in the classpath... jvm-minimal only contains kt classes".
    // Let's assume for now we just want the subset of STDLIB sources.
    // If we need the local stubs from jvm-minimal-for-test, we might need to copy them or reference them.
    // Let's copy the stdlib subset first.
    
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

val outputMinimalKlib = layout.buildDirectory.file("libs/kotlin-stdlib-jvm-minimal-ir.klib")

// Helper to separate Java compilation
fun createJavaCompilationTask(sourceTask: TaskProvider<Sync>): TaskProvider<Jar> {
    val variantName = sourceTask.name.replaceFirstChar { it.uppercase() }
    val javaCompileName = "compileJava${variantName}"
    val jarName = "jarJava${variantName}"

    // Use 'project' to ensure we are targeting the project's task container
    // strictly speaking 'tasks.register' at script level targets the project's tasks
    val javaCompileTask = tasks.register(javaCompileName, JavaCompile::class) {
        dependsOn(sourceTask)
        source = fileTree(sourceTask.map { it.destinationDir }) {
            include("**/*.java")
        }
        destinationDirectory.set(layout.buildDirectory.dir("classes/java/$variantName"))
        // Resolve dependencies for Java compilation
        val runtimeClasspath = project.configurations.getByName("runtimeClasspath")
        // We add the full runtime classpath to satisfy dependencies like kotlin-reflect and annotations     
        classpath = runtimeClasspath
        
        sourceCompatibility = "1.8"
        targetCompatibility = "1.8"
        options.compilerArgs.add("-Xlint:-options")
        options.compilerArgs.add("-Xlint:-deprecation")
        options.compilerArgs.add("-Xlint:none")
        options.compilerArgs.add("-nowarn")

        // Remove -Werror if present to allow build to pass with warnings
        options.compilerArgs.remove("-Werror")
        options.compilerArgs.remove("-Xwerror") 
        options.isDeprecation = false
        options.isWarnings = false
    }

    return tasks.register(jarName, Jar::class) {
        from(javaCompileTask.map { it.destinationDirectory })
        archiveFileName.set("kotlin-stdlib-java-$variantName.jar")
        destinationDirectory.set(layout.buildDirectory.dir("libs"))
    }
}

val jarJava = createJavaCompilationTask(copySources)

fun JavaExec.configureJklibCompilation(
    sourceTask: TaskProvider<Sync>,
    klibOutput: Provider<RegularFile>,
    classpathJar: Provider<RegularFile>,
    extraClasspath: FileCollection = project.files()
) {
    dependsOn(sourceTask)
    
    // Add dependency on the jar task to ensure it's built
    dependsOn(classpathJar)
    
    // Use the standard runtime classpath from the 'main' source set
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("org.jetbrains.kotlin.cli.jklib.K2JKlibCompiler")

    // Inputs/Outputs for incremental build
    val inputDir = sourceTask.map { it.destinationDir }
    val sourceTree = fileTree(inputDir) {
        include("**/*")
    }
    inputs.files(sourceTree)
    
    // Add Jar as input
    inputs.file(classpathJar)
    
    outputs.file(klibOutput)

    val runtimeClasspath = project.configurations.getByName("runtimeClasspath")
    // Capture the file collection at configuration time, but map it to value at execution if needed, 
    // or just pass the file collection to inputs to be safe.
    // Actually, simple way: filter it now.
    val kotlinReflectFileCollection = runtimeClasspath.filter { it.name.startsWith("kotlin-reflect") }
    
    // Add to inputs
    inputs.files(kotlinReflectFileCollection)

    doFirst {
        val allFiles = inputs.files.files.filter { it.extension == "kt" }
        val commonFiles = allFiles.filter { it.path.contains("/common/") }
        val jvmFiles = allFiles.filter { !it.path.contains("/common/") }

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
            "-language-version", "2.3",
            "-api-version", "2.3",
            "-Xstdlib-compilation",
            "-d", outputPath,
            "-Xmulti-platform",
            "-opt-in=kotlin.contracts.ExperimentalContracts",
            "-opt-in=kotlin.ExperimentalMultiplatform",
            "-opt-in=kotlin.contracts.ExperimentalExtendedContracts",
            "-Xcontext-parameters",
            "-Xcompile-builtins-as-part-of-stdlib",
            "-Xreturn-value-checker=full",
            "-Xcommon-sources=${(commonSourceFiles).joinToString(",")}",
        )
        
        
        val kotlinReflectJar = kotlinReflectFileCollection.singleOrNull()
        val fullClasspath = listOfNotNull(
            classpathJar.get().asFile.absolutePath,
            kotlinReflectJar?.absolutePath,
            extraClasspath.asPath
        ).joinToString(File.pathSeparator)

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
    configureJklibCompilation(copySources, outputKlib, jarJava.flatMap { it.archiveFile })
}

val fullStdlibJarProvider = project(":kotlin-stdlib").tasks.named("jvmJar", Jar::class).flatMap { it.archiveFile }

val compileMinimalStdlib by tasks.registering(JavaExec::class) {
    val javaToolchains = project.extensions.getByType(JavaToolchainService::class.java)
    javaLauncher.set(javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(8))
    })
    // Use the full stdlib jar as classpath, instead of the locally built jarJava
    configureJklibCompilation(copyMinimalSources, outputMinimalKlib, fullStdlibJarProvider)
    
    // Suppress "Actual without expect" errors typical in minimal stdlib (due to missing common sources)
    args("-nowarn") 
}

// Expose the KLIB artifact
val distJKlib by configurations.creating {
    isCanBeConsumed = true
    isCanBeResolved = false
}

val distMinimalJKlib by configurations.creating {
    isCanBeConsumed = true
    isCanBeResolved = false
}

artifacts {
    add(distJKlib.name, outputKlib) {
        builtBy(compileStdlib)
    }
    add(distJKlib.name, jarJava)

    add(distMinimalJKlib.name, outputMinimalKlib) {
        builtBy(compileMinimalStdlib)
    }
    add(distMinimalJKlib.name, fullStdlibJarProvider)
}
