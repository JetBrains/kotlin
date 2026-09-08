plugins {
    kotlin("multiplatform")
}

val disambiguationAttribute = Attribute.of("disambiguationAttribute", String::class.java)

kotlin {
    jvm {
        attributes { attribute(disambiguationAttribute, "plainJvm") }
    }

    sourceSets {
        val commonMain = getByName("commonMain") {
            dependencies {
                api(project(":lib"))
            }
        }
    }
}
