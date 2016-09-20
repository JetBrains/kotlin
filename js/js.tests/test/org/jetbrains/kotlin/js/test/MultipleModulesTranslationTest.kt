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

import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.js.config.EcmaVersion
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.js.config.JsConfig
import org.jetbrains.kotlin.js.facade.MainCallParameters
import org.jetbrains.kotlin.js.test.rhino.RhinoFunctionResultChecker
import org.jetbrains.kotlin.js.test.utils.JsTestUtils.getAllFilesInDir
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.serialization.js.ModuleKind
import org.jetbrains.kotlin.utils.KotlinJavascriptMetadataUtils
import java.io.File
import java.util.*

abstract class MultipleModulesTranslationTest(main: String) : BasicTest(main) {
    private val MAIN_MODULE_NAME: String = "main"
    private val OLD_MODULE_SUFFIX = "-old"
    private var dependencies: Map<String, List<String>>? = null
    protected var moduleKind = ModuleKind.PLAIN

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
        val checker = RhinoFunctionResultChecker(MAIN_MODULE_NAME, packageName, functionName, expectedResult)
        runRhinoTests(moduleDirectoryName, BasicTest.DEFAULT_ECMA_VERSIONS, checker)
    }

    private fun translateModule(dirName: String, pathToDir: String, moduleName: String, dependencies: List<String>) {
        val moduleDirectoryName = getModuleDirectoryName(dirName, moduleName)
        val fullFilePaths = getAllFilesInDir(pathToDir + File.separator + moduleName)

        BasicTest.DEFAULT_ECMA_VERSIONS.forEach { version ->
            val libraries = arrayListOf<String>()
            for (dependencyName in dependencies) {
                val moduleDir = getModuleDirectoryName(dirName, dependencyName)
                libraries.add(getMetaFileOutputPath(moduleDir, version))
            }
            generateJavaScriptFiles(fullFilePaths, moduleDirectoryName, MainCallParameters.noCall(), version,
                                    moduleName.removeSuffix(OLD_MODULE_SUFFIX), libraries)
        }
    }
    
    private fun getMetaFileOutputPath(moduleDirectoryName: String, version: EcmaVersion) =
        KotlinJavascriptMetadataUtils.replaceSuffix(getOutputFilePath(moduleDirectoryName, version))

    override fun setupConfig(configuration: CompilerConfiguration) {
        val method = try {
            javaClass.getMethod(name)
        }
        catch (e: NoSuchMethodException) {
            return
        }

        method.getAnnotation(WithModuleKind::class.java)?.let { moduleKind = it.value }
        configuration.put(JSConfigurationKeys.MODULE_KIND, moduleKind)
    }

    override fun shouldGenerateMetaInfo() = true

    override fun additionalJsFiles(ecmaVersion: EcmaVersion): List<String> {
        val result = mutableListOf(MODULE_EMULATION_FILE)
        result += super.additionalJsFiles(ecmaVersion)
        val dirName = getTestName(true)
        assert(dependencies != null) { "dependencies should not be null" }

        for (moduleName in dependencies!!.keys.filter { !it.endsWith(OLD_MODULE_SUFFIX) }) {
            if (moduleName != MAIN_MODULE_NAME && !moduleName.endsWith(OLD_MODULE_SUFFIX)) {
                result.add(getOutputFilePath(getModuleDirectoryName(dirName, moduleName), ecmaVersion))
            }
        }

        return result
    }

    override fun translateFiles(jetFiles: MutableList<KtFile>, outputFile: File, mainCallParameters: MainCallParameters,
                                config: JsConfig) {
        super.translateFiles(jetFiles, outputFile, mainCallParameters, config)

        if (config.moduleKind == ModuleKind.COMMON_JS) {
            val content = FileUtil.loadFile(outputFile, true)
            val wrappedContent = "__beginModule__();\n" +
                                 "$content\n" +
                                 "__endModule__(\"${StringUtil.escapeStringCharacters(config.moduleId)}\");"
            FileUtil.writeToFile(outputFile, wrappedContent)
            // TODO: it would be better to wrap output before JS file is actually written
        }
    }

    private fun readModuleDependencies(testDataDir: String): Map<String, List<String>> {
        val dependenciesTxt = upsearchFile(testDataDir, "dependencies.txt")
        assert(dependenciesTxt.isFile) { "moduleDependencies should not be null" }

        val result = LinkedHashMap<String, List<String>>()
        for (line in dependenciesTxt.readLines()) {
            val split = line.split("->")
            val module = split[0]
            val dependencies = if (split.size > 1) split[1] else ""
            val dependencyList = dependencies.split(",").filterNot { it.isEmpty() }

            result[module] = dependencyList
        }

        return result
    }

    private fun upsearchFile(startingDir: String, name: String): File {
        var dir: File? = File(startingDir)
        var file = File(dir, name)

        while (dir != null && dir.isDirectory && !file.isFile) {
            dir = dir.parentFile
            file = File(dir, name)
        }

        return file
    }
}
