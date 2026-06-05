plugins {
    kotlin("jvm")
    application
}

val runtimeOnly = configurations.getByName("runtimeOnly")
val compileOnly = configurations.getByName("compileOnly")
runtimeOnly.extendsFrom(compileOnly)

dependencies {
    implementation(project(":generators:tree-generator-common"))
}

application {
    mainClass.set("org.jetbrains.kotlin.sir.tree.generator.MainKt")
}

sourceSets {
    "main" {
        projectDefault()
    }
    "test" {}
}
