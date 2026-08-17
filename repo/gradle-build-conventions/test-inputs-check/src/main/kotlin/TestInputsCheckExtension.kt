import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.property
import javax.inject.Inject

/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

abstract class TestInputsCheckExtension @Inject constructor(objects: ObjectFactory) {
    /**
     * Enable or disable test input checking
     */
    val enabled: Property<Boolean> = objects.property<Boolean>().convention(true)

    /**
     * In fail fast mode, an exception will be thrown immediately after accessing an undeclared input.
     * It's mostly useful for debugging when the tests take a long time to finish.
     */
    val failFast: Property<Boolean> = objects.property<Boolean>().convention(false)

    /**
     * Directories whose content is created while the tests run, and may then be read back.
     *
     * The list of declared inputs is a snapshot taken before the test task executes, so a file that a test
     * generates inside one of its own input directories is not in it and would be reported as undeclared.
     * Listing the directory here allows anything underneath it.
     *
     * Use it only for that case. A directory listed here is exempt from the check entirely, so reads of files
     * that genuinely should have been declared stop being reported.
     */
    val allowedDirectories: ConfigurableFileCollection = objects.fileCollection()
}
