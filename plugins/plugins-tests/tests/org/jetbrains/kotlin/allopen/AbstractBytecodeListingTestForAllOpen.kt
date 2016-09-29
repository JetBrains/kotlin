/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.allopen

import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.codegen.AbstractBytecodeListingTest
import org.jetbrains.kotlin.extensions.DeclarationAttributeAltererExtension

abstract class AbstractBytecodeListingTestForAllOpen : AbstractBytecodeListingTest() {
    private companion object {
        val ALLOPEN_ANNOTATIONS = listOf("AllOpen", "AllOpen2", "test.AllOpen")
    }

    override fun setUpEnvironment(environment: KotlinCoreEnvironment) {
        DeclarationAttributeAltererExtension.registerExtension(
                environment.project, AllOpenDeclarationAttributeAltererExtension(ALLOPEN_ANNOTATIONS))
    }
}