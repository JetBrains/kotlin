description = "Kotlin SamWithReceiver Compiler Plugin"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compileOnly(project(":core:descriptors"))
    compileOnly(project(":compiler:frontend"))
    compileOnly(project(":compiler:frontend.java"))
    compileOnly(project(":compiler:plugin-api"))
    compileOnly(intellijCore())

    testApi(project(":compiler:backend"))
    testApi(project(":compiler:cli"))
    testApi(projectTests(":compiler:tests-common"))
    testApi(commonDependency("junit:junit"))
    testCompileOnly(project(":kotlin-compiler"))
    testCompileOnly(intellijCore())
    testApi(project(":kotlin-scripting-jvm-host-unshaded"))
    testRuntimeOnly(intellijCore())
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

publish()

runtimeJar()
sourcesJar()
javadocJar()
testsJar()

projectTest(parallel = true) {
    dependsOn(":dist")
    workingDir = rootDir
}
