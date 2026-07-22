plugins {
    kotlin("multiplatform")
}

val disambiguationAttribute = Attribute.of("disambiguationAttribute", String::class.java)

kotlin {
    jvm {
        withJava()
        attributes { attribute(disambiguationAttribute, "jvmWithJava") }
    }

    sourceSets {
        val jvmMain = getByName("jvmMain") {
            dependencies {
                api(project(":lib"))
            }
        }
    }
}
