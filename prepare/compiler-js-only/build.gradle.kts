import java.io.File
import proguard.gradle.ProGuardTask
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.file.DuplicatesStrategy

description = "Kotlin/JS (only) Compiler"

buildscript {
    dependencies {
        classpath("net.sf.proguard:proguard-gradle:${property("versions.proguard")}")
    }
}

plugins { java }
apply { plugin("kotlin") }


///???
//val compilerManifestClassPath =
//        "kotlin-stdlib.jar"

val fatJarContents by configurations.creating
val fatJarContentsStripMetadata by configurations.creating
val fatJarContentsStripServices by configurations.creating
val fatSourcesJarContents by configurations.creating
val proguardLibraryJars by configurations.creating
val fatJar by configurations.creating
val compilerJar by configurations.creating
val archives by configurations
val compile by configurations

val compilerBaseName = name

val outputJar = File(buildDir, "libs", "$compilerBaseName.jar")

val compilerModules = arrayOf(
    ":compiler:util",
    ":core:util.runtime",

    ":compiler:container",

    ":core:descriptors",
    ":core:deserialization",
    ":compiler:serialization",

    ":compiler:resolution",
    ":compiler:frontend",

    ":compiler:backend-common",

    ":js:js.ast",
    ":js:js.serializer",
    ":js:js.parser",
    ":js:js.frontend",
    ":js:js.translator",

    ":compiler:cli-js"

//    ":compiler:conditional-preprocessor",
//    ":compiler:frontend.java",
//    ":compiler:frontend.script",
//    ":compiler:cli-common",
//    ":compiler:daemon-common",
//    ":compiler:daemon",
//    ":compiler:ir.tree",
//    ":compiler:ir.psi2ir",
//    ":compiler:backend",
//    ":compiler:plugin-api",
//    ":compiler:light-classes",
//    ":compiler:cli",
//    ":compiler:incremental-compilation-impl",
//    ":js:js.dce",
//    ":compiler",
//    ":kotlin-build-common",
)

compilerModules.forEach { evaluationDependsOn(it) }

val compiledModulesSources = compilerModules.map {
    project(it).the<JavaPluginConvention>().sourceSets.getByName("main").allSource
}

dependencies {
    compilerModules.forEach {
        fatJarContents(project(it)) { isTransitive = false }
    }
    compiledModulesSources.forEach {
        fatSourcesJarContents(it)
    }

    fatJarContents(project(":core:builtins", configuration = "builtins"))
    fatJarContents(commonDep("javax.inject"))
//    fatJarContents(commonDep("org.jline", "jline"))
//    fatJarContents(commonDep("org.fusesource.jansi", "jansi"))
    fatJarContents(protobufLite())
//    fatJarContents(protobufFull())
//    fatJarContents(commonDep("com.google.code.findbugs", "jsr305"))
    fatJarContents(commonDep("io.javaslang", "javaslang"))
//    fatJarContents(commonDep("org.jetbrains.kotlinx", "kotlinx-coroutines-core")) { isTransitive = false }

    proguardLibraryJars(files(firstFromJavaHomeThatExists("lib/rt.jar", "../Classes/classes.jar"),
            firstFromJavaHomeThatExists("lib/jsse.jar", "../Classes/jsse.jar")
        /*,toolsJar()*/))

//    proguardLibraryJars(projectDist(":kotlin-stdlib"))
//    proguardLibraryJars(projectDist(":kotlin-script-runtime"))
//    proguardLibraryJars(projectDist(":kotlin-reflect"))

    fatJarContents(projectDist(":kotlin-stdlib"))
    fatJarContents(projectDist(":kotlin-script-runtime"))

//    compile(project(":kotlin-stdlib"))
//    compile(project(":kotlin-script-runtime"))
//    compile(project(":kotlin-reflect"))

    fatJarContents(intellijCoreDep()) { includeJars("intellij-core") }
    fatJarContents(intellijDep()) { includeIntellijCoreJarDependencies(project, { !(it.startsWith("jdom") || it.startsWith("log4j")) }) }
    fatJarContents(intellijDep()) { includeJars("jna-platform") }
    fatJarContentsStripServices(intellijDep("jps-standalone")) { includeJars("jps-model") }
    fatJarContentsStripMetadata(intellijDep()) { includeJars("oromatcher", "jdom", "log4j") }
}


val packCompiler by task<ShadowJar> {
    configurations = listOf(fatJar)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    destinationDir = File(buildDir, "libs")

    setupPublicJar("before-proguard")
    from(fatJarContents)
    afterEvaluate {
        fatJarContentsStripServices.files.forEach { from(zipTree(it)) { exclude("META-INF/services/**") } }
        fatJarContentsStripMetadata.files.forEach { from(zipTree(it)) { exclude("META-INF/jb/** META-INF/LICENSE") } }
    }

//    manifest.attributes.put("Class-Path", compilerManifestClassPath)
//    manifest.attributes.put("Main-Class", "org.jetbrains.kotlin.cli.jvm.K2JVMCompiler")
}

val proguard by task<ProGuardTask> {
    dependsOn(packCompiler)
    configuration("${projectDir}/compiler-js-only.pro")

    val outputJar = File(buildDir, "libs", "$compilerBaseName-after-proguard.jar")

    inputs.files(packCompiler.outputs.files.singleFile)
    outputs.file(outputJar)

    // TODO: remove after dropping compatibility with ant build
    doFirst {
        System.setProperty("kotlin-compiler-jar-before-shrink", packCompiler.outputs.files.singleFile.canonicalPath)
        System.setProperty("kotlin-compiler-jar", outputJar.canonicalPath)
    }

    libraryjars(proguardLibraryJars)
    printconfiguration("$buildDir/compiler-js-only.pro.dump")
}

noDefaultJar()

dist(targetName = compilerBaseName + ".jar", fromTask = proguard)

runtimeJarArtifactBy(proguard, proguard.outputs.files.singleFile) {
    name = compilerBaseName
    classifier = ""
}

sourcesJar {
    from(fatSourcesJarContents)
}
