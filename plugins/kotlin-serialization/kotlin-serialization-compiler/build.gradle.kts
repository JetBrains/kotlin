import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

description = "Kotlin Serialization Compiler Plugin"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compileOnly(intellijCoreDep()) { includeJars("intellij-core") }

    compile(project(":compiler:plugin-api"))
    compile(project(":compiler:frontend"))
    compile(project(":compiler:backend"))
    compile(project(":compiler:ir.backend.common"))
    compile(project(":js:js.frontend"))
    compile(project(":js:js.translator"))

    runtime(kotlinStdlib())
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

val jar = runtimeJar {}

dist(targetName = the<BasePluginConvention>().archivesBaseName + ".jar")

ideaPlugin {
    from(jar)
}

val compileKotlin: KotlinCompile by tasks

compileKotlin.kotlinOptions {
    freeCompilerArgs = freeCompilerArgs + listOf(
        "-Xuse-experimental=kotlin.Experimental",
        "-Xuse-experimental=org.jetbrains.kotlin.ir.DescriptorInIrDeclaration")
}
