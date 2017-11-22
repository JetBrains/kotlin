import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import proguard.gradle.ProGuardTask

description = "Kotlin Full Reflection Library"

buildscript {
    repositories {
        jcenter()
    }
    dependencies {
        classpath("net.sf.proguard:proguard-gradle:5.2.1")
        classpath("com.github.jengelman.gradle.plugins:shadow:${property("versions.shadow")}")
    }
}

plugins { java }

callGroovy("configureJavaOnlyJvm6Project", this)
publish()

val core = "$rootDir/core"
val annotationsSrc = "$buildDir/annotations"
val relocatedCoreSrc = "$buildDir/core-relocated"
val libsDir = property("libsDir")

sourceSets {
    "main" {
        java.srcDir(annotationsSrc)
    }
}

val proguardDeps by configurations.creating
val shadows by configurations.creating {
    isTransitive = false
}
configurations.getByName("compileOnly").extendsFrom(shadows)
val mainJar by configurations.creating

dependencies {
    compile(project(":kotlin-stdlib"))

    proguardDeps(project(":kotlin-stdlib"))

    shadows(project(":kotlin-reflect-api"))
    shadows(project(":core:descriptors"))
    shadows(project(":core:descriptors.jvm"))
    shadows(project(":core:deserialization"))
    shadows(project(":core:descriptors.runtime"))
    shadows(project(":core:util.runtime"))
    shadows("javax.inject:javax.inject:1")
    shadows(project(":custom-dependencies:protobuf-lite", configuration = "default"))
}

val copyAnnotations by task<Sync> {
    // copy just two missing annotations
    from("$core/runtime.jvm/src") {
        include("**/Mutable.java")
        include("**/ReadOnly.java")
    }
    into(annotationsSrc)
    includeEmptyDirs = false
}

tasks.getByName("compileJava").dependsOn(copyAnnotations)

val reflectShadowJar by task<ShadowJar> {
    classifier = "shadow"
    version = null
    callGroovy("manifestAttributes", manifest, project, "Main")

    from(the<JavaPluginConvention>().sourceSets.getByName("main").output)
    from(project(":core:descriptors.jvm").the<JavaPluginConvention>().sourceSets.getByName("main").resources) {
        include("META-INF/services/**")
    }
    from(project(":core:deserialization").the<JavaPluginConvention>().sourceSets.getByName("main").resources) {
        include("META-INF/services/**")
    }

    transform(KotlinModuleShadowTransformer(logger))

    configurations = listOf(shadows)
    relocate("org.jetbrains.kotlin", "kotlin.reflect.jvm.internal.impl")
    relocate("javax.inject", "kotlin.reflect.jvm.internal.impl.javax.inject")
    mergeServiceFiles()
}

val stripMetadata by tasks.creating {
    dependsOn("reflectShadowJar")
    val inputJar = reflectShadowJar.archivePath
    val outputJar = File("$libsDir/kotlin-reflect-stripped.jar")
    inputs.file(inputJar)
    outputs.file(outputJar)
    doLast {
        stripMetadata(logger, "kotlin/reflect/jvm/internal/impl/.*", inputJar, outputJar)
    }
}

val mainArchiveName = "${property("archivesBaseName")}-$version.jar"
val outputJarPath = "$libsDir/$mainArchiveName"
val rtJar = listOf("jre/lib/rt.jar", "../Classes/classes.jar").map { File("${property("JDK_16")}/$it") }.first(File::isFile)

val proguard by task<ProGuardTask> {
    dependsOn(stripMetadata)
    inputs.files(stripMetadata.outputs.files)
    outputs.file(outputJarPath)

    injars(stripMetadata.outputs.files)
    outjars(outputJarPath)

    libraryjars(proguardDeps)
    libraryjars(rtJar)

    configuration("$core/reflection.jvm/reflection.pro")
}

val relocateCoreSources by task<Copy> {
    doFirst {
        delete(relocatedCoreSrc)
    }

    from("$core/descriptors/src")
    from("$core/descriptors.jvm/src")
    from("$core/descriptors.runtime/src")
    from("$core/deserialization/src")
    from("$core/util.runtime/src")

    into(relocatedCoreSrc)
    includeEmptyDirs = false

    eachFile {
        path = path.replace("org/jetbrains/kotlin", "kotlin/reflect/jvm/internal/impl")
    }

    filter { line ->
        line.replace("org.jetbrains.kotlin", "kotlin.reflect.jvm.internal.impl")
    }
}

tasks.getByName("jar").enabled = false

val relocatedSourcesJar by task<Jar> {
    dependsOn(relocateCoreSources)
    classifier = "sources"
    from(relocatedCoreSrc)
    from("$core/reflection.jvm/src")
}

val result = proguard

val dexMethodCount by task<DexMethodCount> {
    dependsOn(result)
    jarFile = File(outputJarPath)
    ownPackages = listOf("kotlin.reflect")
}
tasks.getByName("check").dependsOn(dexMethodCount)

artifacts {
    val artifactJar = mapOf(
            "file" to File(outputJarPath),
            "builtBy" to result,
            "name" to property("archivesBaseName")
    )

    add(mainJar.name, artifactJar)
    add("runtime", artifactJar)
    add("archives", artifactJar)
    add("archives", relocatedSourcesJar)
}

javadocJar()

dist(fromTask = result) {
    from(relocatedSourcesJar)
}
