plugins {
    kotlin("jvm")
    id("java-instrumentation")
    id("jps-compatible")
}

dependencies {
    api(project(":core:descriptors"))
    compileOnly(intellijCore())
    compileOnly(commonDependency("org.jetbrains.intellij.deps.fastutil:intellij-deps-fastutil"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}
