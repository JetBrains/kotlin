plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compile(kotlinStdlib())
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

testsJar()