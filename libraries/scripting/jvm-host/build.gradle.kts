
plugins {
    kotlin("jvm")
    id("jps-compatible")
}

jvmTarget = "1.6"

dependencies {
    compile(project(":kotlin-script-runtime"))
    compile(kotlinStdlib())
    compile(project(":kotlin-scripting-common"))
    compile(project(":kotlin-scripting-jvm"))
    compileOnly(project(":kotlin-scripting-compiler"))
    compileOnly(project(":compiler:cli"))
    compileOnly(project(":kotlin-reflect-api"))
    compileOnly(intellijCoreDep()) { includeJars("intellij-core") }
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

publish()

standardPublicJars()

