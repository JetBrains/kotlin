plugins {
    kotlin("multiplatform")
}

repositories {
    androidxSnapshotRepo()
    google()
}

optInToObsoleteDescriptorBasedAPI()

kotlin {
    jvmToolchain(11)

    jvm()

    sourceSets {
        commonTest.dependencies {
            implementation(project(":kotlin-stdlib-common"))
            implementation(kotlinTest("junit"))
        }

        val jvmTest by getting {
            dependsOn(commonTest.get())

            dependencies {
                // junit
                implementation(libs.junit4)
                implementation(project.dependencies.platform(libs.junit.bom))
                implementation(libs.junit.jupiter.api)
                runtimeOnly(libs.junit.jupiter.engine)


                runtimeOnly(commonDependency("org.jetbrains.intellij.deps", "trove4j"))
                runtimeOnly(commonDependency("org.jetbrains.intellij.deps.fastutil:intellij-deps-fastutil"))
                runtimeOnly(jpsModelImpl())
                implementation(project(":compiler:backend-common"))
                implementation(project(":compiler:ir.backend.common"))
                implementation(project(":compiler:cli"))
                implementation(project(":compiler:backend.jvm"))
                implementation(project(":compiler:fir:fir2ir:jvm-backend"))
                implementation(project(":compiler:backend.jvm.entrypoint"))
                implementation(intellijCore())

                // kotlin deps
                implementation(project(":kotlin-stdlib"))
                implementation(project(":kotlin-reflect"))
                implementation(project(":kotlin-metadata-jvm"))

                // Compose compiler deps
                implementation(project(":plugins:compose-compiler-plugin:compiler-hosted"))
                implementation(project(":plugins:compose-compiler-plugin:compiler-hosted:integration-tests:protobuf-test-classes"))

                // external deps
                implementation(composeRuntime())
                implementation(composeFoundation())
                implementation(composeUi())
                implementation("org.jetbrains.kotlinx:kotlinx-collections-immutable-jvm:0.3.4")
                implementation("com.google.dagger:dagger:2.40.1")
            }
        }
    }
}

tasks.withType(Test::class.java).configureEach {
    this.workingDir = rootDir
    this.maxHeapSize = "1024m"
    this.jvmArgs("--add-opens=jdk.jdi/com.sun.tools.jdi=ALL-UNNAMED")
    // ensure that debugger tests don't launch a separate window
    this.systemProperty("java.awt.headless", "true")
}