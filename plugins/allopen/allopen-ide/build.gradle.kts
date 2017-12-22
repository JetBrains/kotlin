
description = "Kotlin AllOpen IDEA Plugin"

apply { plugin("kotlin") }

jvmTarget = "1.6"

dependencies {
    compile(project(":kotlin-allopen-compiler-plugin"))
    compile(project(":compiler:util"))
    compile(project(":compiler:frontend"))
    compile(project(":compiler:cli-common"))
    compile(project(":idea"))
    compile(project(":idea:idea-jvm"))
    compile(project(":idea:idea-jps-common"))
    compile(project(":plugins:annotation-based-compiler-plugins-ide-support"))
    compileOnly(intellijDep()) { includeJars("openapi", "idea") }
    compileOnly(intellijPluginDep("maven")) { includeJars("maven") }
    compileOnly(intellijPluginDep("gradle")) { includeJars("gradle-tooling-api-3.5", "gradle") }
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

runtimeJar()

ideaPlugin()

