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
    "kotlin-stdlib-common",
    "kotlin-stdlib-jdk7",
    "kotlin-stdlib-jdk8",
    "kotlin-stdlib-js",
    "kotlin-reflect",
    "kotlin-test",
    "kotlin-test-js",
    "kotlin-test-junit5",
    "kotlin-test-junit",
    "kotlin-test-testng",
    "kotlin-test-common",
)


val extractLibs by tasks.registering(Task::class)


modules.forEach { module ->

    val library = configurations.create("kotlin_lib_$module")

    if (module == "kotlin-test-js" || module == "kotlin-stdlib-js") {
        library.attributes {
            attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage::class, "kotlin-runtime"))
            attribute(Attribute.of("org.jetbrains.kotlin.js.compiler", String::class.java), "ir")
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

