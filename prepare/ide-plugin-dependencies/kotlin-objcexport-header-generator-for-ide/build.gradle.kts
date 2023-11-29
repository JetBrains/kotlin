plugins {
    kotlin("jvm")
}

publishJarsForIde(
    listOf(
      ":native:base",
      ":native:objcexport-header-generator",
      ":compiler:ir.serialization.native"
    )
)
