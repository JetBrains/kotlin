
plugins {
    kotlin("jvm")
}

val compile by configurations
val fatJarContents by configurations.creating {
    isCanBeResolved = true
    isCanBeConsumed = false
    attributes {
        attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.JAR))
    }
}
val fatJarContentsStripMetadata by configurations.creating
val fatJarContentsStripServices by configurations.creating

val compilerModules: Array<String> by rootProject.extra

dependencies {
    compilerModules.forEach { module ->
        compile(project(module)) { isTransitive = false }
    }

    fatJarContents(kotlinBuiltins())
    fatJarContents(commonDep("javax.inject"))
    fatJarContents(commonDep("org.jline", "jline"))
    fatJarContents(commonDep("org.fusesource.jansi", "jansi"))
    fatJarContents(protobufFull())
    fatJarContents(commonDep("com.google.code.findbugs", "jsr305"))
    fatJarContents(commonDep("io.javaslang", "javaslang"))

    fatJarContents(intellijCoreDep()) { includeJars("intellij-core") }
    fatJarContents(intellijDep()) { includeIntellijCoreJarDependencies(project, { !(it.startsWith("jdom") || it.startsWith("log4j")) }) }
    fatJarContents(intellijDep()) { includeJars("jna-platform") }
    fatJarContentsStripServices(jpsStandalone()) { includeJars("jps-model") }
    fatJarContentsStripMetadata(intellijDep()) { includeJars("oro", "jdom", "log4j", rootProject = rootProject) }

    if (Platform.P202()) {
        fatJarContents(intellijDep()) { includeJars("intellij-deps-fastutil-8.3.1-1") }
    } else if (Platform.P203.orHigher()) {
        fatJarContents(intellijDep()) { includeJars("intellij-deps-fastutil-8.3.1-3") }
    }
}

val jar: Jar by tasks
jar.apply {
    dependsOn(fatJarContents)
    from { compile.filter { it.extension == "jar" }.map { zipTree(it) } }
    from { fatJarContents.map { zipTree(it) } }
    from { fatJarContentsStripServices.map { zipTree(it).matching { exclude("META-INF/services/**") } } }
    from { fatJarContentsStripMetadata.map { zipTree(it).matching { exclude("META-INF/jb/** META-INF/LICENSE") } } }

    manifest.attributes["Class-Path"] = compilerManifestClassPath
    manifest.attributes["Main-Class"] = "org.jetbrains.kotlin.cli.jvm.K2JVMCompiler"
}