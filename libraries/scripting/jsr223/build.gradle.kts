
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
    compile(project(":kotlin-script-util"))
    compile(project(":kotlin-scripting-compiler"))
    compile(project(":kotlin-scripting-compiler-impl"))
    compileOnly(project(":compiler:cli"))
    compileOnly(project(":kotlin-reflect-api"))
    compileOnly(intellijCoreDep())
    runtime(projectRuntimeJar(":kotlin-compiler"))
    runtime(project(":kotlin-reflect"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

publish()

standardPublicJars()

