
plugins {
    kotlin("jvm")
}

jvmTarget = "1.6"

dependencies {
    compile(project(":kotlin-script-runtime"))
    compile(kotlinStdlib())
    compile(project(":kotlin-scripting-common"))
    compile(project(":kotlin-scripting-jvm"))
    compile(project(":kotlin-scripting-jvm-host"))
    compile(project(":kotlin-scripting-compiler"))
    compileOnly(project(":compiler:cli-common"))
    compileOnly(project(":kotlin-reflect-api"))
    compileOnly(intellijCoreDep())
    runtime(project(":kotlin-compiler"))
    runtime(project(":kotlin-reflect"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

publish()

standardPublicJars()

projectTest(parallel = true)

