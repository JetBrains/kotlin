
plugins {
    kotlin("jvm")
}

val compile by configurations
val fatJarContents by configurations.creating
val fatJarContentsStripMetadata by configurations.creating
val fatJarContentsStripServices by configurations.creating

val compilerModules: Array<String> by rootProject.extra
val compilerManifestClassPath = "kotlin-stdlib.jar kotlin-reflect.jar kotlin-script-runtime.jar"

dependencies {
    compilerModules.forEach { module ->
        compile(project(module)) { isTransitive = false }
    }

    fatJarContents(project(":core:builtins", configuration = "builtins"))
    fatJarContents(commonDep("javax.inject"))
    fatJarContents(commonDep("org.jline", "jline"))
    fatJarContents(commonDep("org.fusesource.jansi", "jansi"))
    fatJarContents(protobufFull())
    fatJarContents(commonDep("com.google.code.findbugs", "jsr305"))
    fatJarContents(commonDep("io.javaslang", "javaslang"))
    fatJarContents(commonDep("org.jetbrains.kotlinx", "kotlinx-coroutines-core")) { isTransitive = false }

    fatJarContents(intellijCoreDep()) { includeJars("intellij-core") }
    fatJarContents(intellijDep()) { includeIntellijCoreJarDependencies(project, { !(it.startsWith("jdom") || it.startsWith("log4j")) }) }
    fatJarContents(intellijDep()) { includeJars("jna-platform") }
    fatJarContentsStripServices(intellijDep("jps-standalone")) { includeJars("jps-model") }
    fatJarContentsStripMetadata(intellijDep()) { includeJars("oro", "jdom", "log4j", rootProject = rootProject) }
}

val jar: Jar by tasks
jar.apply {
    dependsOn(fatJarContents)
    from(compile.filter { it.extension == "jar" }.map { zipTree(it) })
    from(fatJarContents.map { zipTree(it) })
    from(fatJarContentsStripServices.map { zipTree(it) }) { exclude("META-INF/services/**") }
    from(fatJarContentsStripMetadata.map { zipTree(it) }) { exclude("META-INF/jb/** META-INF/LICENSE") }

    manifest.attributes["Class-Path"] = compilerManifestClassPath
    manifest.attributes["Main-Class"] = "org.jetbrains.kotlin.cli.jvm.K2JVMCompiler"
}