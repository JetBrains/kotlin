/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package org.jetbrains.builtInWebServer

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.packaging.artifacts.ArtifactManager

internal class ArtifactWebServerRootsProvider : PrefixlessWebServerRootsProvider() {
  override fun resolve(path: String, project: Project, resolver: FileResolver, pathQuery: PathQuery): PathInfo? {
    if (!pathQuery.searchInArtifacts) {
      return null
    }
    
    for (artifact in ArtifactManager.getInstance(project).artifacts) {
      val root = artifact.outputFile ?: continue
      return resolver.resolve(path, root, pathQuery = pathQuery)
    }
    return null
  }

  override fun getPathInfo(file: VirtualFile, project: Project): PathInfo? {
    for (artifact in ArtifactManager.getInstance(project).artifacts) {
      val root = artifact.outputFile ?: continue
      if (VfsUtilCore.isAncestor(root, file, true)) {
        return PathInfo(null, file, root)
      }
    }
    return null
  }
}