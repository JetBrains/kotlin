plugins {
    id("org.jetbrains.kotlin.multiplatform")
}

repositories {
    mavenLocal()
    mavenCentral()
}

val disambiguation1Attribute = Attribute.of("myDisambiguation1Attribute", String::class.java)
val disambiguation2Attribute = Attribute.of("myDisambiguation2Attribute", String::class.java)

kotlin {
    sourceSets["commonMain"].apply {
        dependencies {
            api("org.jetbrains.kotlin:kotlin-stdlib-common")
            api(project(":exported"))
        }
    }

    iosArm64("ios") {
        attributes.attribute(disambiguation1Attribute, "someValue")
        binaries {
            framework("main") {
                export(project(":exported"))
            }
            framework("custom") {
                embedBitcode("disable")
                linkerOpts = mutableListOf("-L.")
                freeCompilerArgs = mutableListOf("-Xtime")
                isStatic = true
                attributes.attribute(disambiguation2Attribute, "someValue2")
            }
        }
    }

    iosX64("iosSim") {
        binaries {
            framework("main") {
                export(project(":exported"))
            }
        }
    }
}
