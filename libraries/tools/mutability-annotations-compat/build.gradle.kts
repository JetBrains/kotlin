description = "Compatibility artifact with Mutable and ReadOnly annotations"

plugins {
    id("java-instrumentation")
    java
    id("jps-compatible")
}

project.updateJvmTarget("1.8")

sourceSets {
    "main" { projectDefault() }
    "test" {}
}
