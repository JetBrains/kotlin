dependencyResolutionManagement {
    repositories {
        maven(rootProject.projectDir.resolve("build").resolve("repo"))
        mavenLocal()
        mavenCentral()
        google()
    }
}

include(":p1")
include(":p2")
include(":consumer")