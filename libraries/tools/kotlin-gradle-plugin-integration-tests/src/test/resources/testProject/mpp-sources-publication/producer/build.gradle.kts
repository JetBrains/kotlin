plugins {
    kotlin("multiplatform")
    `maven-publish`
}

group = "test"
version = "1.0"

kotlin {
    val disambiguationAttribute = Attribute.of("disambiguationAttribute", String::class.java)
    targets.all { attributes { attribute(disambiguationAttribute, targetName) } }

    jvm {}
    jvm("jvm2") {}
    linuxX64 {}
    linuxArm64 {}
    ios()

    sourceSets {
        val commonMain by getting
        val jvmMain by getting
        val jvm2Main by getting
        val linuxX64Main by getting
        val linuxArm64Main by getting

        val commonTest by getting
        val jvmTest by getting
        val jvm2Test by getting
        val linuxX64Test by getting
        val linuxArm64Test by getting

        val linuxMain by creating {
            dependsOn(commonMain)
            linuxX64Main.dependsOn(this)
            linuxArm64Main.dependsOn(this)
        }

        val linuxTest by creating {
            dependsOn(commonTest)
            linuxX64Test.dependsOn(this)
            linuxArm64Test.dependsOn(this)
        }

        val commonJvmMain by creating {
            dependsOn(commonMain)
            jvmMain.dependsOn(this)
            jvm2Main.dependsOn(this)
        }

        val commonJvmTest by creating {
            dependsOn(commonTest)
            jvmTest.dependsOn(this)
            jvm2Test.dependsOn(this)
        }
    }
}

publishing {
    repositories {
        maven("<localRepo>")
    }
}
