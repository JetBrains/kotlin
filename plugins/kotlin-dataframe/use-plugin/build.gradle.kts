plugins {
    kotlin("jvm")
    id("jps-compatible")
}


sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}

dependencies {
    kotlinCompilerPluginClasspath(project(":plugins:kotlin-dataframe"))
    implementation(files("/home/nikitak/.m2/repository/org/jetbrains/kotlinx/dataframe-core/0.9.0-dev/dataframe-core-0.9.0-dev.jar"))
    api(kotlinStdlib())
    implementation(kotlin("reflect"))
}

tasks.compileKotlin {
    kotlinOptions {
        useK2 = true
        allWarningsAsErrors = false
    }
}

