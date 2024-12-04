description = "Compatibility artifact with Mutable and ReadOnly annotations"

plugins {
    java
    id("jps-compatible")
}

project.updateJvmTarget("1.8")

sourceSets {
    "main" { projectDefault() }
    "test" {}
}
