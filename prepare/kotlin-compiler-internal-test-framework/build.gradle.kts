plugins {
    java
}

val compilerModules: Array<String> by rootProject.extra

dependencies {
    compilerModules.forEach {
        embedded(project(it)) { isTransitive = false }
    }
    embedded(projectTests(":compiler:tests-common-jvm6")) { isTransitive = false }
    embedded(projectTests(":compiler:test-infrastructure")) { isTransitive = false }
    embedded(projectTests(":compiler:test-infrastructure-utils")) { isTransitive = false }
    embedded(projectTests(":compiler:tests-compiler-utils")) { isTransitive = false }
    embedded(projectTests(":compiler:tests-common-new")) { isTransitive = false }
    embedded(protobufFull())
    embedded(kotlinBuiltins())
}

publish()
runtimeJar()
sourcesJar()
javadocJar()