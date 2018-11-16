
description = "Kotlin Serialization Compiler Plugin"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

jvmTarget = "1.6"

repositories {
    maven("https://kotlin.bintray.com/kotlinx")
}

dependencies {
    compileOnly(intellijCoreDep()) { includeJars("intellij-core") }

    compile(project(":compiler:plugin-api"))
    compile(project(":compiler:frontend"))
    compile(project(":compiler:backend"))
    compile(project(":js:js.frontend"))
    compile(project(":js:js.translator"))

    runtime(project(":kotlin-stdlib"))

    testRuntimeOnly(projectRuntimeJar(":kotlin-compiler"))

    testCompile(project(":compiler:cli"))
    testCompile(projectTests(":compiler:tests-common"))
    testCompile(commonDep("junit:junit"))

    // Compile only used to avoid dependency leak from kotlinx repository to generators module
    testCompileOnly(commonDep("org.jetbrains.kotlinx:kotlinx-serialization-runtime")) { isTransitive = false }
    testRuntime(commonDep("org.jetbrains.kotlinx:kotlinx-serialization-runtime")) { isTransitive = false }
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

val jar = runtimeJar {
    from(fileTree("$projectDir/src")) { include("META-INF/**") }
}

testsJar {}

dist(targetName = the<BasePluginConvention>().archivesBaseName + ".jar")

ideaPlugin {
    from(jar)
}

projectTest {
    workingDir = rootDir
}
