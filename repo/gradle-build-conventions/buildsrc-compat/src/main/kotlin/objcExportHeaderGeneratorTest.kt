import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test

/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

/**
 * Wrapper for [nativeTest] which helps to apply defaults expected by
 * projects under ':native:objcexport-header-generator:*'
 */
fun Project.objCExportHeaderGeneratorTest(
    taskName: String,
    testDisplayNameTag: String? = null,
    configure: Test.() -> Unit = {},
) = nativeTest(
    taskName = taskName,
    tag = null,
    requirePlatformLibs = false,
) {
    /* Prepare klibs that can be used in tests as dependencies */
    dependsOn(":native:objcexport-header-generator:testLibraryA:prepareTestKlib")
    dependsOn(":native:objcexport-header-generator:testLibraryB:prepareTestKlib")

    useJUnitPlatform()
    enableJunit5ExtensionsAutodetection()

    /* Special 'Kotlin in Fleet' flag that can switch test mode to 'local development' */
    systemProperty("kif.local", project.providers.gradleProperty("kif.local").isPresent)

    /* Tests will show this displayName as an additional tag (e.g., to differentiate between K1 and AA tests) */
    if (testDisplayNameTag != null) {
        systemProperty("testDisplayName.tag", testDisplayNameTag)
    }

    configure()
}
