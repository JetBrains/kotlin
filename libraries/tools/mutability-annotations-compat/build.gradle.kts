description = "Compatibility artifact with Mutable and ReadOnly annotations"

plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    java
}

jvmToolchains {
    targetBytecodeVersion = JdkMajorVersion.JDK_1_8
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}
