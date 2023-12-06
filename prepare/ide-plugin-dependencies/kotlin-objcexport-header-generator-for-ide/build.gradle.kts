plugins {
    kotlin("jvm")
}

publishJarsForIde(
    listOf(
      ":native:base",
      ":native:objcexport-header-generator",
      ":native:objcexport-header-generator-k1",
      ":native:objcexport-header-generator-analysis-api",
      ":compiler:ir.serialization.native"
    )
)
