import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import proguard.gradle.ProGuardTask

description = "Kotlin \"main\" script definition"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

val jarBaseName = property("archivesBaseName") as String

val proguardLibraryJars by configurations.creating

dependencies {
    compileOnly("org.apache.ivy:ivy:2.4.0")
    compileOnly(project(":compiler:cli-common"))
    compileOnly(project(":kotlin-scripting-jvm-host"))
    compileOnly(project(":kotlin-script-util"))
    testCompile(project(":kotlin-scripting-jvm-host"))
    testCompile(project(":kotlin-script-util"))
    runtime(project(":kotlin-compiler-embeddable"))
    runtime(project(":kotlin-scripting-compiler-embeddable"))
    runtime(project(":kotlin-scripting-jvm-host-embeddable"))
    runtime(project(":kotlin-reflect"))
    embedded(project(":kotlin-scripting-common")) { isTransitive = false }
    embedded(project(":kotlin-scripting-jvm")) { isTransitive = false }
    embedded(project(":kotlin-scripting-jvm-host")) { isTransitive = false }
    embedded(project(":kotlin-script-util")) { isTransitive = false }
    embedded(project(":kotlin-script-runtime")) { isTransitive = false }
    embedded("org.apache.ivy:ivy:2.4.0")
    embedded(commonDep("org.jetbrains.kotlinx", "kotlinx-coroutines-core")) { isTransitive = false }
    proguardLibraryJars(files(firstFromJavaHomeThatExists("jre/lib/rt.jar", "../Classes/classes.jar"),
                              firstFromJavaHomeThatExists("jre/lib/jsse.jar", "../Classes/jsse.jar"),
                              toolsJar()))
    proguardLibraryJars(kotlinStdlib())
    proguardLibraryJars(project(":kotlin-reflect"))
    proguardLibraryJars(project(":kotlin-compiler"))
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

    injars(mapOf("filter" to "!META-INF/versions/**"), packJar.get().outputs.files)

    val outputJar = fileFrom(buildDir, "libs", "$jarBaseName-$version-after-proguard.jar")

    outjars(outputJar)

    inputs.files(packJar.get().outputs.files.singleFile)
    outputs.file(outputJar)

    libraryjars(mapOf("filter" to "!META-INF/versions/**"), proguardLibraryJars)
}

val resultJar by task<Jar> {
    val pack = if (kotlinBuildProperties.proguard) proguard else packJar
    dependsOn(pack)
    setupPublicJar(jarBaseName)
    from {
        zipTree(pack.get().outputs.files.singleFile)
    }
}

addArtifact("runtime", resultJar)
addArtifact("archives", resultJar)

sourcesJar()

javadocJar()
