
import java.io.File
import proguard.gradle.ProGuardTask
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.file.DuplicatesStrategy

description = "Kotlin Compiler"

buildscript {
    repositories {
        jcenter()
    }

    dependencies {
        classpath("com.github.jengelman.gradle.plugins:shadow:${property("versions.shadow")}")
        classpath("net.sf.proguard:proguard-gradle:${property("versions.proguard")}")
    }
}

plugins {
    `java`
}

// You can run Gradle with "-Pkotlin.build.proguard=true" to enable ProGuard run on kotlin-compiler.jar (on TeamCity, ProGuard always runs)
val shrink =
        findProperty("kotlin.build.proguard")?.toString()?.toBoolean()
        ?: hasProperty("teamcity")

val compilerManifestClassPath =
        "kotlin-stdlib.jar kotlin-reflect.jar kotlin-script-runtime.jar"

val fatJarContents by configurations.creating
val fatSourcesJarContents by configurations.creating
val proguardLibraryJars by configurations.creating
val fatJar by configurations.creating
val compilerJar by configurations.creating
val archives by configurations
val compile by configurations

val compilerBaseName = name

val outputJar = File(buildDir, "libs", "$compilerBaseName.jar")

val compilerModules: Array<String> by rootProject.extra

val ideaCoreSdkJars: Array<String> by rootProject.extra
val coreSdkJarsSimple = ideaCoreSdkJars.filterNot { it == "jdom" || it == "log4j" }.toTypedArray()

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
    fatJarContents(ideaSdkCoreDeps(*coreSdkJarsSimple))
    fatJarContents(ideaSdkDeps("jna-platform"))
    fatJarContents(commonDep("javax.inject"))
    fatJarContents(commonDep("org.jline", "jline"))
    fatJarContents(commonDep("org.fusesource.jansi", "jansi"))
    fatJarContents(protobufFull())
    fatJarContents(commonDep("com.google.code.findbugs", "jsr305"))
    fatJarContents(commonDep("io.javaslang", "javaslang")) { isTransitive = false }
    fatJarContents(ideaSdkDeps("kotlinx-coroutines-core"))

    proguardLibraryJars(files(firstFromJavaHomeThatExists("lib/rt.jar", "../Classes/classes.jar"),
            firstFromJavaHomeThatExists("lib/jsse.jar", "../Classes/jsse.jar"),
            toolsJar()))
    proguardLibraryJars(projectDist(":kotlin-stdlib"))
    proguardLibraryJars(projectDist(":kotlin-script-runtime"))
    proguardLibraryJars(projectDist(":kotlin-reflect"))

    compile(project(":kotlin-stdlib"))
    compile(project(":kotlin-script-runtime"))
    compile(project(":kotlin-reflect"))
}

val packCompiler by task<ShadowJar> {
    configurations = listOf(fatJar)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    destinationDir = File(buildDir, "libs")
    dependsOn(protobufFullTask)

    setupPublicJar("before-proguard")
    from(fatJarContents)
    ideaSdkDeps("jps-model.jar", subdir = "jps").forEach { from(zipTree(it)) { exclude("META-INF/services/**") } }
    ideaSdkDeps("oro-2.0.8").forEach { from(zipTree(it)) { exclude("META-INF/jb/** META-INF/LICENSE") } }
    ideaSdkCoreDeps("jdom", "log4j").forEach { from(zipTree(it)) { exclude("META-INF/jb/** META-INF/LICENSE") } }

    manifest.attributes.put("Class-Path", compilerManifestClassPath)
    manifest.attributes.put("Main-Class", "org.jetbrains.kotlin.cli.jvm.K2JVMCompiler")
}

val proguard by task<ProGuardTask> {
    dependsOn(packCompiler)
    configuration("$rootDir/compiler/compiler.pro")

    val outputJar = File(buildDir, "libs", "$compilerBaseName-after-proguard.jar")

    inputs.files(packCompiler.outputs.files.singleFile)
    outputs.file(outputJar)

    // TODO: remove after dropping compatibility with ant build
    doFirst {
        System.setProperty("kotlin-compiler-jar-before-shrink", packCompiler.outputs.files.singleFile.canonicalPath)
        System.setProperty("kotlin-compiler-jar", outputJar.canonicalPath)
    }

    libraryjars(proguardLibraryJars)
    printconfiguration("$buildDir/compiler.pro.dump")
}

noDefaultJar()

cleanArtifacts()

val pack = if (shrink) proguard else packCompiler

dist(targetName = compilerBaseName + ".jar",
     fromTask = pack)

runtimeJarArtifactBy(pack, pack.outputs.files.singleFile) {
    name = compilerBaseName
    classifier = ""
}
sourcesJar {
    from(fatSourcesJarContents)
}
javadocJar()

publish()

