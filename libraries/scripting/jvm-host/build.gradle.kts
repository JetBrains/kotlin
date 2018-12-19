
plugins {
    kotlin("jvm")
    id("jps-compatible")
}

jvmTarget = "1.6"

dependencies {
    compile(project(":kotlin-script-runtime"))
    compile(project(":kotlin-stdlib"))
    compile(project(":kotlin-scripting-common"))
    compile(project(":kotlin-scripting-jvm"))
    compile(project(":kotlin-script-util"))
    compileOnly(project(":compiler:cli"))
    compileOnly(project(":kotlin-reflect-api"))
    compileOnly(intellijCoreDep())
    runtime(projectRuntimeJar(":kotlin-compiler"))
    runtime(project(":kotlin-reflect"))
    testCompile(projectTests(":compiler:tests-common"))
    testCompile(commonDep("junit"))
    testRuntime(project(":kotlin-reflect"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

standardPublicJars()

publish()

projectTest {
    workingDir = rootDir
}
