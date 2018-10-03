
description = "Kotlin Serialization Compiler Plugin"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

jvmTarget = "1.6"

dependencies {
    compileOnly(intellijCoreDep()) { includeJars("intellij-core") }

    compile(project(":compiler:plugin-api"))
    compile(project(":compiler:frontend"))
    compile(project(":compiler:backend"))
    compile(project(":js:js.frontend"))
    compile(project(":js:js.translator"))

    runtime(project(":kotlin-stdlib"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

val jar = runtimeJar {
    from(fileTree("$projectDir/src")) { include("META-INF/**") }
}

dist(targetName = the<BasePluginConvention>().archivesBaseName + ".jar")

ideaPlugin {
    from(jar)
}
