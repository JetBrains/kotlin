plugins {
    base
}

val sources by configurations.creating

dependencies {
    sources(project(":kotlin-stdlib-common", configuration = "sources"))
}

val buildSources by tasks.registering(Jar::class) {
    dependsOn(sources)
    from(provider { zipTree(sources.singleFile) })
}

artifacts.add("default", buildSources) {
    name = "kotlin-stdlib-common"
    classifier = "sources"
}