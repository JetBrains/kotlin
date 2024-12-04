plugins {
    id("org.jetbrains.kotlin.jvm")
}

repositories {
    mavenLocal()
    mavenCentral()
}

sourceSets {
    main {
        kotlin {
            setSrcDirs(listOf("src/main/kotlin", "src/shared/kotlin"))
        }
    }
}
