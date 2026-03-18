plugins {
    kotlin("jvm")
}

dependencies {
    api(kotlinStdlib())
}

sourceSets {
    "main" { projectDefault() }
}
