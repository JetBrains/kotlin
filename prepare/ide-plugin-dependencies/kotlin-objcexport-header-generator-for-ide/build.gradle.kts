plugins {
    kotlin("jvm")
}

publishJarsForIde(
    listOf(
      ":compiler:ir.serialization.native",
      ":native:analysis-api-klib-reader",
      ":native:base",
      ":native:objcexport-header-generator",
      ":native:objcexport-header-generator-analysis-api",
      ":native:objcexport-header-generator-k1",
    )
)
