description = "Kotlin NoArg Compiler Plugin"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    embedded(project(":kotlin-noarg-compiler-plugin.k1"))
    embedded(project(":kotlin-noarg-compiler-plugin.backend"))
    embedded(project(":kotlin-noarg-compiler-plugin.cli"))

    testApi(project(":compiler:backend"))
    testApi(project(":compiler:cli"))
    testApi(projectTests(":compiler:tests-common"))
    testApi(commonDependency("junit:junit"))
    testApi(intellijCore())
}

sourceSets {
    "main" { none() }
    "test" { projectDefault() }
}

runtimeJar()
sourcesJar()
javadocJar()
testsJar()

projectTest(parallel = true) {
    workingDir = rootDir
}
