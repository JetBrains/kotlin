@file:DependsOn("org.slf4j:slf4j-api:1.7.36")
@file:DependsOn("org.slf4j:slf4j-simple:1.7.36")

val logger = org.slf4j.LoggerFactory.getLogger("")
logger.info("test-{}", "slf4j" as Any)
