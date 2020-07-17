plugins {
    base
}

val sources by configurations.creating {
    attributes {
        isCanBeResolved = true
        isCanBeConsumed = false
        attributes {
            attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.JAR))
            attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.DOCUMENTATION))
            attribute(DocsType.DOCS_TYPE_ATTRIBUTE, objects.named(DocsType.SOURCES))
        }
    }
}

configurations["embeddedElements"].isCanBeConsumed = false

dependencies {
    sources(project(":kotlin-stdlib-common"))
}

val buildSources by tasks.registering(Jar::class) {
    dependsOn(sources)
    from(provider { zipTree(sources.singleFile) })
}

artifacts.add("default", buildSources) {
    name = "kotlin-stdlib-common"
    classifier = "sources"
}