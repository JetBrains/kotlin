/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok

import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.runners.AbstractDiagnosticTest
import org.jetbrains.kotlin.test.runners.AbstractFirPsiDiagnosticTest
import org.jetbrains.kotlin.test.runners.codegen.AbstractBlackBoxCodegenTest
import org.jetbrains.kotlin.test.runners.codegen.AbstractFirLightTreeBlackBoxCodegenTest
import org.jetbrains.kotlin.test.runners.codegen.AbstractIrBlackBoxCodegenTest
import org.jetbrains.kotlin.test.runners.configurationForClassicAndFirTestsAlongside

// ---------------------------- box ----------------------------

open class AbstractBlackBoxCodegenTestForLombok : AbstractBlackBoxCodegenTest() {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.enableLombok()
    }
}

open class AbstractIrBlackBoxCodegenTestForLombok : AbstractIrBlackBoxCodegenTest() {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.enableLombok()
    }
}

open class AbstractFirLightTreeBlackBoxCodegenTestForLombok : AbstractFirLightTreeBlackBoxCodegenTest() {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.enableLombok()
    }
}

// ---------------------------- diagnostics ----------------------------

open class AbstractDiagnosticTestForLombok : AbstractDiagnosticTest() {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.enableLombok()
    }
}

open class AbstractFirPsiDiagnosticTestForLombok : AbstractFirPsiDiagnosticTest() {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.configurationForClassicAndFirTestsAlongside()
        builder.enableLombok()
    }
}

// ---------------------------- configuration ----------------------------

fun TestConfigurationBuilder.enableLombok() {
    useConfigurators(::LombokEnvironmentConfigurator)
    useAdditionalSourceProviders(::LombokAdditionalSourceFileProvider)
    useCustomRuntimeClasspathProviders(::LombokRuntimeClassPathProvider)
}
