plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    implementation("com.caoccao.javet:swc4j:1.6.0") // Must Have
    implementation("com.caoccao.javet:swc4j-linux-arm64:1.6.0")
    implementation("com.caoccao.javet:swc4j-linux-x86_64:1.6.0")
    implementation("com.caoccao.javet:swc4j-macos-arm64:1.6.0")
    implementation("com.caoccao.javet:swc4j-macos-x86_64:1.6.0")
    implementation("com.caoccao.javet:swc4j-windows-arm64:1.6.0")
    implementation("com.caoccao.javet:swc4j-windows-x86_64:1.6.0")

    api(kotlinStdlib())
    api(project(":js:js.ast"))
    compileOnly(intellijCore())
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}
