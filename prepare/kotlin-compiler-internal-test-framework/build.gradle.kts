plugins {
    java
}

dependencies {
    embedded(projectTests(":compiler:tests-common-jvm6")) { isTransitive = false }
    embedded(projectTests(":compiler:test-infrastructure")) { isTransitive = false }
    embedded(projectTests(":compiler:test-infrastructure-utils")) { isTransitive = false }
    embedded(projectTests(":compiler:tests-compiler-utils")) { isTransitive = false }
    embedded(projectTests(":compiler:tests-common-new")) { isTransitive = false }
    embedded(intellijJavaRt()) { isTransitive = false }
}

publish()
runtimeJar()
sourcesJar()
javadocJar()
