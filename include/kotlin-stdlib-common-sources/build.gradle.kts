plugins {
    kotlin("jvm")
}

val sources by configurations.creating

dependencies {
    sources(project(":kotlin-stdlib-common", configuration = "sources"))
}

artifacts {
    add("runtime", provider { sources.singleFile }) {
        classifier = "sources"
    }
}