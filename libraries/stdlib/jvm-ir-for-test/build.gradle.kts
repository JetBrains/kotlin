description = "Kotlin JVM IR Stdlib for Tests"

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
        exclude("kotlin/jvm/functions/Functions.kt")
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
    classpathJar: Provider<RegularFile>
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
        val jvmFiles = allFiles.filter { it.path.contains("/jvm/") }
        val commonFiles = allFiles.filter { it.path.contains("/common/")}

        val jvmSourceFiles = jvmFiles.map { it.absolutePath }
        val commonSourceFiles = commonFiles.map { it.absolutePath }

        logger.lifecycle("Compiling ${jvmSourceFiles.size} JVM files and ${commonSourceFiles.size} Common files, total ${allFiles.size}")

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
            "-Xoutput-builtins-metadata",
        )
        
        // Add separate Java compilation output and kotlin-reflect to classpath
        val kotlinReflectJar = kotlinReflectFileCollection.singleOrNull()
        val fullClasspath = listOfNotNull(
            classpathJar.get().asFile.absolutePath,
            kotlinReflectJar?.absolutePath
        ).joinToString(File.pathSeparator)

        args("-classpath", fullClasspath)

        args(jvmSourceFiles)
        args(commonSourceFiles)
    }
}

val compileStdlib by tasks.registering(JavaExec::class) {
    configureJklibCompilation(copySources, outputKlib, jarJava.flatMap { it.archiveFile })
}

// Expose the KLIB artifact
val distJKlib by configurations.creating {
    isCanBeConsumed = true
    isCanBeResolved = false
}

artifacts {
    add(distJKlib.name, outputKlib) {
        builtBy(compileStdlib)
    }
    add(distJKlib.name, jarJava)
}
