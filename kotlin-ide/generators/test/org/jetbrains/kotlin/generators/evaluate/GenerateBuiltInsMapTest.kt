/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.generators.test.evaluate

import junit.framework.TestCase
import org.jetbrains.kotlin.backend.jvm.serialization.JvmIdSignatureDescriptor
import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.generators.evaluate.DESTINATION
import org.jetbrains.kotlin.generators.evaluate.generateMap
import org.jetbrains.kotlin.ir.backend.jvm.serialization.JvmManglerDesc
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.util.ConstantValueGenerator
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.ir.util.TypeTranslator
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.test.KotlinTestUtils

class GenerateBuiltInsMapTest : TestCase() {
    fun testGeneratedDataIsUpToDate() {
        val text = generateMap(getIrBuiltIns())
        KotlinTestUtils.assertEqualsToFile(DESTINATION, text)
    }

    private fun getIrBuiltIns(): IrBuiltIns {
        val builtIns = DefaultBuiltIns.Instance
        val languageSettings = LanguageVersionSettingsImpl(LanguageVersion.KOTLIN_1_3, ApiVersion.KOTLIN_1_3)

        val moduleDescriptor = ModuleDescriptorImpl(Name.special("<test-module>"), LockBasedStorageManager(""), builtIns)
        val mangler = JvmManglerDesc()
        val signaturer = JvmIdSignatureDescriptor(mangler)
        val symbolTable = SymbolTable(signaturer)
        val constantValueGenerator = ConstantValueGenerator(moduleDescriptor, symbolTable)
        val typeTranslator = TypeTranslator(symbolTable, languageSettings, builtIns)
        constantValueGenerator.typeTranslator = typeTranslator
        typeTranslator.constantValueGenerator = constantValueGenerator

        return IrBuiltIns(builtIns, typeTranslator, symbolTable)
    }
}
