/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.contracts.contextual

import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.jvm.compiler.AbstractLoadJavaTest

abstract class AbstractLoadJavaContractsTest : AbstractLoadJavaTest() {
    override fun configureEnvironment(environment: KotlinCoreEnvironment?) {
        requireNotNull(environment)
        registerExtensions(environment.project)
    }

    override fun getExtraClasspath() = contractsDslClasspath
}