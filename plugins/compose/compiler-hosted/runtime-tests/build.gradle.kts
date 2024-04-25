plugins {
    kotlin("multiplatform")
}

repositories {
    androidxSnapshotRepo()
    google()
}

optInToObsoleteDescriptorBasedAPI()


dependencies {
    // run compilation with the current compiler to ensure that Compose plugin is binary compatible
    kotlinCompilerClasspath(project(":kotlin-compiler-embeddable"))

    kotlinCompilerPluginClasspath(project(":plugins:compose-compiler-plugin:compiler"))
}

kotlin {
    jvm()

    jvmToolchain(11)

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

                // kotlin deps
                implementation(project(":kotlin-stdlib"))

                // external deps
                implementation(composeRuntime())
                implementation(composeRuntimeTestUtils())
            }
        }
    }
}