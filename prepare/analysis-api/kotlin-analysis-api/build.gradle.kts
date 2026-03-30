plugins {
    `java-library`
    id("analysis-api-artifact")
}

dependencies {
    api(project(":prepare:analysis-api:kotlin-analysis-api-surface"))
    implementation(project(":prepare:analysis-api:kotlin-analysis-api-implementation"))
}
