plugins {
    java
}

val embedded by configurations

dependencies {
    embedded(projectTests(":compiler:tests-common")) { isTransitive = false }
}

publish()

runtimeJar()

sourcesJar {
    from {
        project(":compiler:tests-common").sourceSets["test"].allSource
    }
}

javadocJar()
