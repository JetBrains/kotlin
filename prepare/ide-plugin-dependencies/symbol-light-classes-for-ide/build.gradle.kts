plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
}

publishJarsForIde(
    listOf(
        ":analysis:light-classes-base",
        ":analysis:light-classes-surface",
        ":analysis:symbol-light-classes",
        ":analysis:decompiled:light-classes-for-decompiled"
    )
)
