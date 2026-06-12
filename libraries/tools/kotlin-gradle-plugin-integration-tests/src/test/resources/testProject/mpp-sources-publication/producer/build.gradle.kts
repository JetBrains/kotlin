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
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        val commonMain = getByName("commonMain")
        val jvmMain = getByName("jvmMain")
        val jvm2Main = getByName("jvm2Main")
        val linuxX64Main = getByName("linuxX64Main")
        val linuxArm64Main = getByName("linuxArm64Main")
        val iosX64Main = getByName("iosX64Main")
        val iosArm64Main = getByName("iosArm64Main")
        val iosSimulatorArm64Main = getByName("iosSimulatorArm64Main")

        val commonTest = getByName("commonTest")
        val jvmTest = getByName("jvmTest")
        val jvm2Test = getByName("jvm2Test")
        val linuxX64Test = getByName("linuxX64Test")
        val linuxArm64Test = getByName("linuxArm64Test")
        val iosX64Test = getByName("iosX64Test")
        val iosArm64Test = getByName("iosArm64Test")
        val iosSimulatorArm64Test = getByName("iosSimulatorArm64Test")

        val iosMain = create("iosMain") {
            dependsOn(commonMain)
            iosX64Main.dependsOn(this)
            iosArm64Main.dependsOn(this)
            iosSimulatorArm64Main.dependsOn(this)
        }

        val iosTest = create("iosTest") {
            dependsOn(commonMain)
            iosX64Test.dependsOn(this)
            iosArm64Test.dependsOn(this)
            iosSimulatorArm64Test.dependsOn(this)
        }

        val linuxMain = create("linuxMain") {
            dependsOn(commonMain)
            linuxX64Main.dependsOn(this)
            linuxArm64Main.dependsOn(this)
        }

        val linuxTest = create("linuxTest") {
            dependsOn(commonTest)
            linuxX64Test.dependsOn(this)
            linuxArm64Test.dependsOn(this)
        }

        val commonJvmMain = create("commonJvmMain") {
            dependsOn(commonMain)
            jvmMain.dependsOn(this)
            jvm2Main.dependsOn(this)
        }

        val commonJvmTest = create("commonJvmTest") {
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
