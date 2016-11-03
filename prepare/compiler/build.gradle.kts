
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.tasks.bundling.Jar
import java.io.File
import proguard.gradle.ProGuardTask
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.file.DuplicatesStrategy

buildscript {
    repositories {
        jcenter()
        mavenCentral()
    }

    dependencies {
        classpath("com.github.jengelman.gradle.plugins:shadow:1.2.3")
        classpath("net.sf.proguard:proguard-gradle:5.3.1")
    }
}

repositories {
    mavenLocal()
    maven { setUrl(rootProject.extra["repo"]) }
    mavenCentral()
}

fun Jar.setupRuntimeJar(implementationTitle: String): Unit {
    dependsOn(":prepare:build.version:prepare")
    manifest.attributes.apply {
        put("Built-By", rootProject.extra["manifest.impl.vendor"])
        put("Implementation-Vendor", rootProject.extra["manifest.impl.vendor"])
        put("Implementation-Title", implementationTitle)
        put("Implementation-Version", rootProject.extra["build.number"])
    }
    from(configurations.getByName("build-version").files) {
        into("META-INF/")
    }
}

fun DependencyHandler.buildVersion(): Dependency {
    val cfg = configurations.create("build-version")
    return add(cfg.name, project(":prepare:build.version", configuration = "default"))
}

fun commonDep(coord: String): String {
    val parts = coord.split(':')
    return when (parts.size) {
        1 -> "$coord:$coord:${rootProject.extra["versions.$coord"]}"
        2 -> "${parts[0]}:${parts[1]}:${rootProject.extra["versions.${parts[1]}"]}"
        3 -> coord
        else -> throw IllegalArgumentException("Illegal maven coordinates: $coord")
    }
}

fun commonDep(group: String, artifact: String): String = "$group:$artifact:${rootProject.extra["versions.$artifact"]}"

fun DependencyHandler.projectDep(name: String): Dependency = project(name, configuration = "default")
fun DependencyHandler.projectDepIntransitive(name: String): Dependency =
        project(name, configuration = "default").apply { isTransitive = false }

fun Project.getCompiledClasses() = the<JavaPluginConvention>().sourceSets.getByName("main").output
fun Project.getSources() = the<JavaPluginConvention>().sourceSets.getByName("main").allSource
fun Project.getResourceFiles() = the<JavaPluginConvention>().sourceSets.getByName("main").resources

val protobufLiteProject = ":custom-dependencies:protobuf-lite"
fun KotlinDependencyHandler.protobufLite(): ProjectDependency =
        project(protobufLiteProject, configuration = "default").apply { isTransitive = false }
val protobufLiteTask = "$protobufLiteProject:prepare"
fun KotlinDependencyHandler.protobufFull(): ProjectDependency =
        project(protobufLiteProject, configuration = "relocated").apply { isTransitive = false }
val protobufFullTask = "$protobufLiteProject:prepare-relocated-protobuf"

// TODO: common ^ 8< ----

// Set to false to disable proguard run on kotlin-compiler.jar. Speeds up the build
val shrink = true
val bootstrapBuild = false

val compilerManifestClassPath =
    if (bootstrapBuild) "kotlin-runtime-internal-bootstrap.jar kotlin-reflect-internal-bootstrap.jar kotlin-script-runtime-internal-bootstrap.jar"
    else "kotlin-runtime.jar kotlin-reflect.jar kotlin-script-runtime.jar"

val compilerClassesCfg = configurations.create("compiler-classes")
val ideaSdkCoreCfg = configurations.create("ideaSdk-core")
val otherDepsCfg = configurations.create("other-deps")
val proguardLibraryJarsCfg = configurations.create("library-jars")
val mainCfg = configurations.create("default")
val embeddableCfg = configurations.create("embeddable")

val outputBeforeSrinkJar = "$buildDir/libs/kotlin-compiler-before-shrink.jar"
val outputJar = "$buildDir/libs/kotlin-compiler.jar"
val outputEmbeddableJar = "$buildDir/libs/kotlin-compiler-embeddable.jar"

val kotlinEmbeddableRootPackage = "org.jetbrains.kotlin"

artifacts.add(mainCfg.name, file(outputJar))
artifacts.add(embeddableCfg.name, file(outputEmbeddableJar))

val javaHome = System.getProperty("java.home")

val compilerProject = project(":compiler")

dependencies {
    compilerClassesCfg(projectDepIntransitive(":compiler"))
    compilerClassesCfg(projectDepIntransitive(":core:util.runtime"))
    ideaSdkCoreCfg(files(File("$rootDir/ideaSDK/core").listFiles { f -> f.extension == "jar" && f.name != "util.jar" }))
    ideaSdkCoreCfg(files("$rootDir/ideaSDK/lib/jna-platform.jar"))
    ideaSdkCoreCfg(files("$rootDir/ideaSDK/lib//oromatcher.jar"))
    ideaSdkCoreCfg(files("$rootDir/ideaSDK/jps/jps-model.jar"))
    otherDepsCfg(commonDep("javax.inject"))
    otherDepsCfg(commonDep("jline"))
    otherDepsCfg(protobufFull())
    otherDepsCfg(commonDep("com.github.spullara.cli-parser", "cli-parser"))
    otherDepsCfg(commonDep("com.google.code.findbugs", "jsr305"))
    buildVersion()
    proguardLibraryJarsCfg(files("$javaHome/lib/rt.jar", "$javaHome/lib/jsse.jar"))
    proguardLibraryJarsCfg(project(":prepare:runtime", configuration = "default").apply { isTransitive = false })
    proguardLibraryJarsCfg(project(":prepare:reflect", configuration = "default").apply { isTransitive = false })
    proguardLibraryJarsCfg(project(":core.script.runtime").apply { isTransitive = false })
    embeddableCfg(project(":prepare:runtime", configuration = "default"))
    embeddableCfg(project(":prepare:reflect", configuration = "default"))
    embeddableCfg(projectDepIntransitive(":core.script.runtime"))
    embeddableCfg(projectDepIntransitive(":build-common"))
    embeddableCfg(projectDepIntransitive(":libraries:kotlin.test"))
    embeddableCfg(projectDepIntransitive(":libraries:stdlib"))
}

val packCompilerTask = task<ShadowJar>("internal.pack-compiler") {
    configurations = listOf(mainCfg)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    archiveName = if (shrink) outputBeforeSrinkJar else outputJar
    dependsOn(compilerProject.path + ":classes", protobufFullTask)
    setupRuntimeJar("Kotlin Compiler")
    from(compilerProject.getCompiledClasses())
    from(project(":core:util.runtime").getCompiledClasses())
    from(ideaSdkCoreCfg.files)
    from(otherDepsCfg.files)
    from(project(":core.builtins").getResourceFiles()) { include("kotlin/**") }
    from(fileTree("${project(":core").projectDir}/descriptor.loader.java/src")) { include("META-INF/services/**") }
    from(fileTree("${compilerProject.projectDir}/frontend.java/src")) { include("META-INF/services/**") }
    from(fileTree("${compilerProject.projectDir}/backend/src")) { include("META-INF/services/**") }
    from(fileTree("${compilerProject.projectDir}/cli/src")) { include("META-INF/services/**") }
    from(fileTree("$rootDir/idea/src")) {
        include("META-INF/extensions/common.xml",
                "META-INF/extensions/kotlin2jvm.xml",
                "META-INF/extensions/kotlin2js.xml")
    }
    from(fileTree("$rootDir/resources")) { include("kotlinManifest.properties") }

    manifest.attributes.put("Class-Path", compilerManifestClassPath)
    manifest.attributes.put("Main-Class", "org.jetbrains.kotlin.cli.jvm.K2JVMCompiler")
}

val proguardTask = task<ProGuardTask>("internal.proguard-compiler") {
    dependsOn(packCompilerTask)
    configuration("$rootDir/compiler/compiler.pro")

    inputs.files(outputBeforeSrinkJar)
    outputs.file(outputJar)

    doFirst {
        System.setProperty("kotlin-compiler-jar-before-shrink", outputBeforeSrinkJar.toString())
        System.setProperty("kotlin-compiler-jar", outputJar.toString())
    }

    proguardLibraryJarsCfg.files.forEach { jar ->
        libraryjars(jar)
    }
    printconfiguration("$rootDir/prog.txt")
}

val mainTask = task("prepare") {
    dependsOn(if (shrink) proguardTask else packCompilerTask)
}

val embeddableTask = task<ShadowJar>("prepare-embeddable-compiler") {
    archiveName = outputEmbeddableJar
    configurations = listOf(embeddableCfg)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    dependsOn(mainTask, ":build-common:assemble", ":core.script.runtime:assemble", ":libraries:kotlin.test:assemble", ":libraries:stdlib:assemble")
    from(files(outputJar))
    from(embeddableCfg.files)
    relocate("com.google.protobuf", "org.jetbrains.kotlin.protobuf" )
    relocate("com.intellij", "$kotlinEmbeddableRootPackage.com.intellij")
    relocate("com.google", "$kotlinEmbeddableRootPackage.com.google")
    relocate("com.sampullara", "$kotlinEmbeddableRootPackage.com.sampullara")
    relocate("org.apache", "$kotlinEmbeddableRootPackage.org.apache")
    relocate("org.jdom", "$kotlinEmbeddableRootPackage.org.jdom")
    relocate("org.fusesource", "$kotlinEmbeddableRootPackage.org.fusesource") {
        // TODO: remove "it." after #KT-12848 get addressed
        it.exclude("org.fusesource.jansi.internal.CLibrary")
    }
    relocate("org.picocontainer", "$kotlinEmbeddableRootPackage.org.picocontainer")
    relocate("jline", "$kotlinEmbeddableRootPackage.jline")
    relocate("gnu", "$kotlinEmbeddableRootPackage.gnu")
    relocate("javax.inject", "$kotlinEmbeddableRootPackage.javax.inject")
}

defaultTasks(mainTask.name, embeddableTask.name)

