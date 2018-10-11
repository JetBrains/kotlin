
description = "Kotlin NoArg Compiler Plugin"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compileOnly(project(":compiler:frontend"))
    compileOnly(project(":compiler:frontend.java"))
    compileOnly(project(":compiler:backend"))
    compileOnly(project(":compiler:util"))
    compileOnly(project(":compiler:plugin-api"))
    compileOnly(intellijCoreDep()) { includeJars("intellij-core") }
    compileOnly(intellijDep()) { includeJars("asm-all") }
    runtime(project(":kotlin-stdlib"))

    testRuntimeOnly(projectRuntimeJar(":kotlin-compiler"))
    testCompile(project(":compiler:backend"))
    testCompile(project(":compiler:cli"))
    testCompile(projectTests(":compiler:tests-common"))
    testCompile(commonDep("junit:junit"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

val jar = runtimeJar {
    from(fileTree("$projectDir/src")) { include("META-INF/**") }
}

testsJar {}

dist(targetName = the<BasePluginConvention>().archivesBaseName.removePrefix("kotlin-") + ".jar")

ideaPlugin {
    from(jar)
}

projectTest {
    workingDir = rootDir
}
