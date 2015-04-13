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

package org.jetbrains.kotlin.js.test

import org.jetbrains.kotlin.js.config.EcmaVersion
import org.jetbrains.kotlin.js.facade.MainCallParameters
import org.jetbrains.kotlin.js.test.rhino.RhinoFunctionResultChecker
import org.jetbrains.kotlin.js.test.utils.JsTestUtils.getAllFilesInDir
import org.jetbrains.kotlin.utils.KotlinJavascriptMetadataUtils
import java.io.File
import java.util.ArrayList
import java.util.LinkedHashMap

public abstract class MultipleModulesTranslationTest(main: String) : BasicTest(main) {

    private val MAIN_MODULE_NAME: String = "main"
    private var dependencies: Map<String, List<String>>? = null

    override fun checkFooBoxIsOkByPath(filePath: String) {
        val dirName = getTestName(true)
        dependencies = readModuleDependencies(filePath)

        // KT-7428: !! is necessary here
        for ((moduleName, dependencies) in dependencies!!) {
            translateModule(dirName, filePath, moduleName, dependencies)
        }

        val filename = getInputFilePath(getModuleDirectoryName(dirName, MAIN_MODULE_NAME) + File.separator + MAIN_MODULE_NAME + ".kt")
        val packageName = getPackageName(filename)
        runMultiModuleTest(dirName, packageName, BasicTest.TEST_FUNCTION, "OK")
    }

    private fun runMultiModuleTest(dirName: String, packageName: String, functionName: String, expectedResult: Any) {
        val moduleDirectoryName = getModuleDirectoryName(dirName, MAIN_MODULE_NAME)
        runRhinoTests(moduleDirectoryName, BasicTest.DEFAULT_ECMA_VERSIONS, RhinoFunctionResultChecker(MAIN_MODULE_NAME, packageName, functionName, expectedResult))
    }

    private fun translateModule(dirName: String, pathToDir: String, moduleName: String, dependencies: List<String>) {
        val moduleDirectoryName = getModuleDirectoryName(dirName, moduleName)
        val fullFilePaths = getAllFilesInDir(pathToDir + File.separator + moduleName)
        val libraries = ArrayList<String>()
        for (dependencyName in dependencies) {
            libraries.add(getMetaFileOutputPath(getModuleDirectoryName(dirName, dependencyName)))
        }
        generateJavaScriptFiles(fullFilePaths, moduleDirectoryName, MainCallParameters.noCall(), BasicTest.DEFAULT_ECMA_VERSIONS, moduleName, libraries)
    }

    override fun getMetaFileOutputPath(moduleId: String): String? =
        getOutputPath() + moduleId + KotlinJavascriptMetadataUtils.META_JS_SUFFIX

    override fun additionalJsFiles(ecmaVersion: EcmaVersion): List<String> {
        val result = super.additionalJsFiles(ecmaVersion)
        val dirName = getTestName(true)
        assert(dependencies != null, "dependencies should not be null")

        for (moduleName in dependencies!!.keySet()) {
            if (moduleName != MAIN_MODULE_NAME) {
                result.add(getOutputFilePath(getModuleDirectoryName(dirName, moduleName), ecmaVersion))
            }
        }

        return result
    }

    private fun readModuleDependencies(testDataDir: String): Map<String, List<String>> {
        val dependenciesTxt = File(testDataDir, "dependencies.txt")
        assert(dependenciesTxt.exists(), "moduleDependencies should not be null")

        val result = LinkedHashMap<String, List<String>>()
        for (line in dependenciesTxt.readLines()) {
            val split = line.splitBy("->")
            val module = split[0]
            val dependencies = if (split.size() > 1) split[1] else ""
            val dependencyList = dependencies.splitBy(",").filterNot { it.isEmpty() }

            result[module] = dependencyList
        }

        return result
    }


    private fun getModuleDirectoryName(dirName: String, moduleName: String) = dirName + File.separator + moduleName
}
