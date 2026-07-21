plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
}

dependencies {
    api(project(":kotlin-stdlib-jdk8"))
    testImplementation(kotlinTest("junit5"))
}

sourceSets {
    "test" {
        kotlin.srcDir("test")
    }
}

tasks.test {
    useJUnitPlatform()
}

tasks.compileTestKotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xcontext-parameters")
        optIn.addAll(
                "kotlin.ExperimentalStdlibApi",
                "kotlin.ExperimentalUnsignedTypes",
                "kotlin.time.ExperimentalTime",
        )
    }
}
