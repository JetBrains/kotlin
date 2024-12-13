plugins {
    id("base")
}

val publishedFile = layout.buildDirectory.file("publication/output.txt")

val producerTask by tasks.registering {
    outputs.file(publishedFile)
    val projectName = project.name
    doLast {
        publishedFile.get().asFile.writeText("Hello World from ${projectName}")
    }
}

configurations.create("publication") {
    isCanBeConsumed = true
    isCanBeResolved = false
    outgoing.artifact(producerTask) {
        builtBy(producerTask)
    }
    attributes {
        attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.DOCUMENTATION))
        attribute(Usage.USAGE_ATTRIBUTE, objects.named("custom-aggregation-for-reporting"))
        attribute(Attribute.of("custom-attribute-type", String::class.java), "custom-attribute-value")
    }
}
