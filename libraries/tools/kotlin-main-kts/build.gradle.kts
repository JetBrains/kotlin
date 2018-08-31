import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.internal.artifacts.publish.ArchivePublishArtifact
import proguard.gradle.ProGuardTask

description = "Kotlin \"main\" script definition"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

// You can run Gradle with "-Pkotlin.build.proguard=true" to enable ProGuard run on the jar (on TeamCity, ProGuard always runs)
val shrink =
    findProperty("kotlin.build.proguard")?.toString()?.toBoolean()
        ?: hasProperty("teamcity")

val jarBaseName = property("archivesBaseName") as String

val fatJarContents by configurations.creating
val proguardLibraryJars by configurations.creating
val fatJar by configurations.creating
val default by configurations
val runtimeJar by configurations.creating

default.apply {
    extendsFrom(runtimeJar)
}

val projectsDependencies = listOf(
    ":kotlin-scripting-common",
    ":kotlin-scripting-jvm",
    ":kotlin-script-util",
    ":kotlin-script-runtime")

dependencies {
    projectsDependencies.forEach {
        compileOnly(project(it))
        fatJarContents(project(it)) { isTransitive = false }
        testCompile(project(it))
    }
    runtime(project(":kotlin-compiler"))
    runtime(project(":kotlin-reflect"))
    fatJarContents("org.apache.ivy:ivy:2.4.0")
    fatJarContents(commonDep("org.jetbrains.kotlinx", "kotlinx-coroutines-core")) { isTransitive = false }
    proguardLibraryJars(files(firstFromJavaHomeThatExists("jre/lib/rt.jar", "../Classes/classes.jar"),
                              firstFromJavaHomeThatExists("jre/lib/jsse.jar", "../Classes/jsse.jar"),
                              toolsJar()))
    proguardLibraryJars(project(":kotlin-stdlib"))
    proguardLibraryJars(project(":kotlin-reflect"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { }
}

noDefaultJar()

val packJar by task<ShadowJar> {
    configurations = listOf(fatJar)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    destinationDir = File(buildDir, "libs")

    setupPublicJar(project.the<BasePluginConvention>().archivesBaseName, "before-proguard")

    from(mainSourceSet.output)
    from(fatJarContents)
}

val proguard by task<ProGuardTask> {
    dependsOn(packJar)
    configuration("main-kts.pro")

    injars(mapOf("filter" to "!META-INF/versions/**"), packJar.outputs.files)

    val outputJar = fileFrom(buildDir, "libs", "$jarBaseName-$version-after-proguard.jar")

    outjars(outputJar)

    inputs.files(packJar.outputs.files.singleFile)
    outputs.file(outputJar)

    libraryjars(mapOf("filter" to "!META-INF/versions/**"), proguardLibraryJars)
    printconfiguration("$buildDir/compiler.pro.dump")
}

val pack = if (shrink) proguard else packJar

runtimeJarArtifactBy(pack, pack.outputs.files.singleFile) {
    name = jarBaseName
    classifier = ""
}

sourcesJar()
javadocJar()

publish()

