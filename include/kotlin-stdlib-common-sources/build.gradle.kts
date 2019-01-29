plugins {
    kotlin("jvm")
}

val sources by configurations.creating

dependencies {
    sources(project(":kotlin-stdlib-common", configuration = "sources"))
}

val buildSources by tasks.creating(Jar::class.java) {
    dependsOn(sources)
    from(provider { zipTree(sources.singleFile) })
}

artifacts.add("runtime", buildSources) {
    classifier = "sources"
}