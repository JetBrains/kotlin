plugins {
    java
    kotlin("jvm")
    id("jps-compatible")
    antlr
}

dependencies {
    implementation("com.caoccao.javet:swc4j:1.6.0") // Must Have
    implementation("com.caoccao.javet:swc4j-linux-arm64:1.6.0")
    implementation("com.caoccao.javet:swc4j-linux-x86_64:1.6.0")
    implementation("com.caoccao.javet:swc4j-macos-arm64:1.6.0")
    implementation("com.caoccao.javet:swc4j-macos-x86_64:1.6.0")
    implementation("com.caoccao.javet:swc4j-windows-arm64:1.6.0")
    implementation("com.caoccao.javet:swc4j-windows-x86_64:1.6.0")

    antlr("org.antlr:antlr4:4.13.2")
    implementation("org.antlr:antlr4-runtime:4.13.2")

    api(kotlinStdlib())
    api(project(":js:js.ast"))
    compileOnly(intellijCore())
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

tasks.generateGrammarSource {
    maxHeapSize = "64m"
    arguments = arguments + listOf("-visitor", "-long-messages")
}

tasks.compileKotlin {
    dependsOn(tasks.generateGrammarSource)
}
