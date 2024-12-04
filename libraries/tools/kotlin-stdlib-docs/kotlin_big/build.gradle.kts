plugins {
    base
}

val artifactsVersion: String by project
val artifactsRepo: String by project
val kotlin_libs: String by project

repositories {
    maven(url = artifactsRepo)
    mavenCentral()
}

val modules = listOf(
    "kotlin-stdlib",
    "kotlin-stdlib-jdk7",
    "kotlin-stdlib-jdk8",
    "kotlin-stdlib-js",
    "kotlin-reflect",
    "kotlin-test",
    "kotlin-test-js",
    "kotlin-test-junit5",
    "kotlin-test-junit",
    "kotlin-test-testng",
)


val extractLibs by tasks.registering(Task::class)


modules.forEach { module ->

    val library = configurations.create("kotlin_lib_$module")

    if (module == "kotlin-test-js" || module == "kotlin-stdlib-js") {
        library.attributes {
            attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage::class, "kotlin-runtime"))
            attribute(Attribute.of("org.jetbrains.kotlin.platform.type", String::class.java), "js")
        }
    }

    dependencies {
        library(group = "org.jetbrains.kotlin", name = module, version = artifactsVersion)
    }

    val libsTask = tasks.register<Sync>("extract_lib_$module") {
        dependsOn(library)

        from({ library })
        into("$kotlin_libs/$module")
    }

    extractLibs.configure { dependsOn(libsTask) }
}

val stdlibMetadataAll = configurations.create("kotlin_lib_kotlin-stdlib-metadata") {
    attributes {
        attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category::class, Category.LIBRARY))
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage::class, "kotlin-metadata"))
    }
}
dependencies {
    stdlibMetadataAll(group = "org.jetbrains.kotlin", name = "kotlin-stdlib", version = artifactsVersion) {
        isTransitive = false
    }
}
val extractStdlibCommonMain by tasks.registering(org.gradle.jvm.tasks.Jar::class) {
    archiveBaseName.set("kotlin-stdlib-common")
    archiveExtension.set("klib")
    destinationDirectory.set(file("$kotlin_libs/kotlin-stdlib-common"))
    dependsOn(stdlibMetadataAll)
    from({ zipTree(stdlibMetadataAll.singleFile) }) {
        include("commonMain/**")
        eachFile {
            relativePath = RelativePath(true, *relativePath.segments.drop(1).toTypedArray())
        }
        includeEmptyDirs = false
    }
}
extractLibs.configure { dependsOn(extractStdlibCommonMain) }
