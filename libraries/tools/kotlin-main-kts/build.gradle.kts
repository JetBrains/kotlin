import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import proguard.gradle.ProGuardTask

description = "Kotlin \"main\" script definition"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

val jarBaseName = property("archivesBaseName") as String

val proguardLibraryJars by configurations.creating

val projectsDependencies = listOf(
    ":kotlin-scripting-common",
    ":kotlin-scripting-jvm",
    ":kotlin-script-util",
    ":kotlin-script-runtime"
)

dependencies {
    projectsDependencies.forEach {
        compileOnly(project(it))
        embedded(project(it)) { isTransitive = false }
        testCompile(project(it))
    }
    compileOnly("org.apache.ivy:ivy:2.4.0")
    runtime(project(":kotlin-compiler-embeddable"))
    runtime(project(":kotlin-scripting-compiler-embeddable"))
    runtime(project(":kotlin-reflect"))
    embedded("org.apache.ivy:ivy:2.4.0")
    embedded(commonDep("org.jetbrains.kotlinx", "kotlinx-coroutines-core")) { isTransitive = false }
    proguardLibraryJars(files(firstFromJavaHomeThatExists("jre/lib/rt.jar", "../Classes/classes.jar"),
                              firstFromJavaHomeThatExists("jre/lib/jsse.jar", "../Classes/jsse.jar"),
                              toolsJar()))
    proguardLibraryJars(kotlinStdlib())
    proguardLibraryJars(project(":kotlin-reflect"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { }
}

publish()

noDefaultJar()

val mainKtsRootPackage = "org.jetbrains.kotlin.mainKts"
val mainKtsRelocatedDepsRootPackage = "$mainKtsRootPackage.relocatedDeps"

val packJar by task<ShadowJar> {
    configurations = emptyList()
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    destinationDirectory.set(File(buildDir, "libs"))
    archiveClassifier.set("before-proguard")

    from(mainSourceSet.output)
    from(project.configurations.embedded)

    // don't add this files to resources classpath to avoid IDE exceptions on kotlin project
    from("jar-resources")

    if (kotlinBuildProperties.relocation) {
        packagesToRelocate.forEach {
            relocate(it, "$mainKtsRelocatedDepsRootPackage.$it")
        }
    }
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
}

val resultJar = tasks.register<Jar>("resultJar") {
    val pack = if (kotlinBuildProperties.proguard) proguard else packJar
    dependsOn(pack)
    setupPublicJar(jarBaseName)
    from {
        zipTree(pack.outputs.files.singleFile)
    }
}

addArtifact("runtime", resultJar)
addArtifact("archives", resultJar)

sourcesJar()

javadocJar()
