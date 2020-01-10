description = "Lightweight annotation processing support – Java compiler plugin"

plugins {
    `java`
    id("jps-compatible")
}

dependencies {
    compileOnly(toolsJar())
}

sourceSets {
    "main" { projectDefault() }
    "test" { }
}

publish()

runtimeJar()
sourcesJar()
javadocJar()