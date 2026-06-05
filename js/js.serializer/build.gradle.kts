plugins {
    kotlin("jvm")
}

dependencies {
    api(kotlinStdlib())
    api(project(":js:js.config"))
    api(project(":core:deserialization.common"))
    implementation(project(":core:descriptors"))
    implementation(project(":core:deserialization"))
    implementation(project(":core:metadata"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}
