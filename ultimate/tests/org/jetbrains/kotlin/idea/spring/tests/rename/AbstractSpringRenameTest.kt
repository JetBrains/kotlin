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

package org.jetbrains.kotlin.idea.spring.tests.rename

import com.google.gson.JsonObject
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.spring.facet.SpringFacet
import org.jetbrains.kotlin.idea.rename.AbstractUltimateRenameTest

abstract class AbstractSpringRenameTest : AbstractUltimateRenameTest() {
    override fun getTestRoot() = "/spring/core/rename/"

    override fun configExtra(rootDir: VirtualFile, renameParamsObject: JsonObject) {
        val fileSet = SpringFacet.getInstance(module)!!.addFileSet("default", "default")!!
        for (filePath in renameParamsObject.getAsJsonArray("springFileSet")) {
            fileSet.addFile(rootDir.findFileByRelativePath(filePath.asString)!!)
        }
    }
}