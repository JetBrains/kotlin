dependencyResolutionManagement {
    repositories {
        mavenLocal()
        mavenCentral()
    }
}

include(":consumerA")

includeBuild("<producer_path>") {
    dependencySubstitution {
        substitute(module("org.jetbrains.sample:producerA")).using(project(":producerA"))
    }
}
