plugins {
    id("org.jetbrains.kotlin.jvm")
    `java-library`
    `jvm-test-suite`
}

testing {
    suites {
        named("test", JvmTestSuite::class) {
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-test-junit5")
            }
        }
    }
}
