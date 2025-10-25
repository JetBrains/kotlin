plugins {
    kotlin("jvm")
}

dependencies {
    api(project(":kotlin-stdlib-jdk8"))
    testImplementation(kotlinTest("junit"))
}

sourceSets {
    "test" {
        kotlin.srcDir("test")
    }
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
