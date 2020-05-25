plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compileOnly(project(":compiler:psi"))
    compileOnly(project(":idea:idea-frontend-independent"))
    compileOnly(project(":idea:idea-core"))
    compileOnly(project(":compiler:fir:fir2ir"))
    compileOnly(project(":compiler:fir:resolve"))
    compileOnly(project(":compiler:fir:checkers"))
    compileOnly(project(":compiler:fir:java"))
    compileOnly(project(":compiler:fir:jvm"))
    compileOnly(intellijCoreDep())
    compileOnly(intellijDep())

    Platform[191].orLower {
        compileOnly(intellijDep()) { includeJars("java-api", "java-impl") }
    }

    Platform[192].orHigher {
        compileOnly(intellijPluginDep("java")) { includeJars("java-api", "java-impl") }
    }
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

testsJar()

projectTest {
    dependsOn(":dist")
    workingDir = rootDir
}
