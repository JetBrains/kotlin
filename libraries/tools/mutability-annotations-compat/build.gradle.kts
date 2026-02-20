description = "Compatibility artifact with Mutable and ReadOnly annotations"

plugins {
    id("root-config")
    java
}

project.updateJvmTarget("1.8")

sourceSets {
    "main" { projectDefault() }
    "test" {}
}
