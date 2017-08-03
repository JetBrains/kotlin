
import java.io.File
import proguard.gradle.ProGuardTask
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.file.DuplicatesStrategy

buildscript {
    repositories {
        jcenter()
    }

    dependencies {
        classpath("com.github.jengelman.gradle.plugins:shadow:1.2.3")
        classpath("net.sf.proguard:proguard-gradle:5.3.1")
    }
}

apply { plugin("maven") }

// Set to false to disable proguard run on kotlin-compiler.jar. Speeds up the build
val shrink = true
val bootstrapBuild = false

val compilerManifestClassPath =
    if (bootstrapBuild) "kotlin-runtime-internal-bootstrap.jar kotlin-reflect-internal-bootstrap.jar kotlin-script-runtime-internal-bootstrap.jar"
    else "kotlin-runtime.jar kotlin-reflect.jar kotlin-script-runtime.jar"

val ideaSdkCoreCfg = configurations.create("ideaSdk-core")
val otherDepsCfg = configurations.create("other-deps")
val proguardLibraryJarsCfg = configurations.create("library-jars")
val mainCfg = configurations.create("default_")
val packedCfg = configurations.create("packed")
//val withBootstrapRuntimeCfg = configurations.create("withBootstrapRuntime")

val compilerBaseName: String by rootProject.extra

val outputJar = File(buildDir, "libs", "$compilerBaseName.jar")

val javaHome = System.getProperty("java.home")

val compilerProject = project(":compiler")

dependencies {
    ideaSdkCoreCfg(ideaSdkCoreDeps(*(rootProject.extra["ideaCoreSdkJars"] as Array<String>)))
    ideaSdkCoreCfg(ideaSdkDeps("jna-platform", "oromatcher"))
    ideaSdkCoreCfg(ideaSdkDeps("jps-model.jar", subdir = "jps"))
    otherDepsCfg(commonDep("javax.inject"))
    otherDepsCfg(commonDep("org.jline", "jline"))
    otherDepsCfg(protobufFull())
    otherDepsCfg(commonDep("com.github.spullara.cli-parser", "cli-parser"))
    otherDepsCfg(commonDep("com.google.code.findbugs", "jsr305"))
    otherDepsCfg(commonDep("io.javaslang","javaslang"))
    otherDepsCfg(preloadedDeps("json-org"))
    buildVersion()
    proguardLibraryJarsCfg(files("$javaHome/lib/rt.jar".takeIf { File(it).exists() } ?: "$javaHome/../Classes/classes.jar",
                                 "$javaHome/lib/jsse.jar".takeIf { File(it).exists() } ?: "$javaHome/../Classes/jsse.jar"))
    proguardLibraryJarsCfg(kotlinDep("stdlib"))
    proguardLibraryJarsCfg(kotlinDep("script-runtime"))
    proguardLibraryJarsCfg(kotlinDep("reflect"))
    proguardLibraryJarsCfg(files("${System.getProperty("java.home")}/../lib/tools.jar"))
//    proguardLibraryJarsCfg(project(":prepare:runtime", configuration = "default").apply { isTransitive = false })
//    proguardLibraryJarsCfg(project(":prepare:reflect", configuration = "default").apply { isTransitive = false })
//    proguardLibraryJarsCfg(project(":core:script.runtime").apply { isTransitive = false })
}

val packCompilerTask = task<ShadowJar>("internal.pack-compiler") {
    configurations = listOf(packedCfg)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    destinationDir = File(buildDir, "libs")
    baseName = compilerBaseName + "-before-shrink"
    dependsOn(protobufFullTask)
    setupRuntimeJar("Kotlin Compiler")
    (rootProject.extra["compilerModules"] as Array<String>).forEach {
        dependsOn("$it:classes")
        from(project(it).getCompiledClasses())
    }
    from(ideaSdkCoreCfg.files)
    from(otherDepsCfg.files)
    from(project(":core:builtins").getResourceFiles()) { include("kotlin/**") }

    manifest.attributes.put("Class-Path", compilerManifestClassPath)
    manifest.attributes.put("Main-Class", "org.jetbrains.kotlin.cli.jvm.K2JVMCompiler")
}

val proguardTask = task<ProGuardTask>("internal.proguard-compiler") {
    dependsOn(packCompilerTask)
    configuration("$rootDir/compiler/compiler.pro")

    inputs.files(packCompilerTask.outputs.files.singleFile)
    outputs.file(outputJar)

    // TODO: remove after dropping compatibility with ant build
    doFirst {
        System.setProperty("kotlin-compiler-jar-before-shrink", packCompilerTask.outputs.files.singleFile.canonicalPath)
        System.setProperty("kotlin-compiler-jar", outputJar.canonicalPath)
    }

    proguardLibraryJarsCfg.files.forEach { jar ->
        libraryjars(jar)
    }
    printconfiguration("$buildDir/compiler.pro.dump")
}

dist {
    if (shrink) {
        from(proguardTask)
    } else {
        from(packCompilerTask)
        rename("-before-shrink", "")
    }
}

artifacts.add(mainCfg.name, proguardTask.outputs.files.singleFile) {
    builtBy(proguardTask)
    classifier = ""
}


