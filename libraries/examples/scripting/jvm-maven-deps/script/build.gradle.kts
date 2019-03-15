
plugins {
    kotlin("jvm")
}

dependencies {
    compile(project(":kotlin-scripting-jvm"))
    compile(project(":kotlin-script-util"))
    runtime("com.jcabi:jcabi-aether:0.10.1")
    runtime("org.sonatype.aether:aether-api:1.13.1")
    runtime("org.apache.maven:maven-core:3.0.3")
}

sourceSets {
    "main" { projectDefault() }
    "test" { }
}
