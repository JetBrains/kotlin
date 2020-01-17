// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.org.jetbrains.plugins.gradle.util

import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import java.io.File

class ProjectInfoBuilder private constructor(id: String, private val base: VirtualFile, var useKotlinDsl: Boolean) {

  val name = "${System.currentTimeMillis()}-$id"

  private val modules = ArrayList<ModuleInfo>()

  private fun createSubDirectory(relativePath: String): VirtualFile {
    val file = File(base.path, relativePath)
    FileUtil.ensureExists(file)
    val fileSystem = LocalFileSystem.getInstance()
    return fileSystem.refreshAndFindFileByIoFile(file)!!
  }

  fun moduleInfo(ideName: String, relativeRoot: String, configure: ModuleInfoBuilder.() -> Unit) {
    val root = createSubDirectory(relativeRoot)
    val builder = ModuleInfoBuilder(root, ideName).apply(configure)
    modules.add(builder.create())
  }

  fun moduleInfo(ideName: String, relativeRoot: String, simpleName: String? = null, useKotlinDsl: Boolean? = null) {
    moduleInfo(ideName, relativeRoot) {
      this.useKotlinDsl = useKotlinDsl
      this.simpleName = simpleName
      modulesPerSourceSet.add("$ideName.main")
      modulesPerSourceSet.add("$ideName.test")
    }
  }

  private fun create(): ProjectInfo {
    return ProjectInfo(modules.first(), modules.drop(1))
  }

  private fun getExternalName(root: VirtualFile): Pair<String, Boolean> {
    val base = modules.firstOrNull()?.root ?: base
    val projectFile = File(base.path)
    val moduleFile = File(root.path)
    val relativeModuleFile = moduleFile.relativeTo(projectFile)
    val parts = FileUtil.splitPath(relativeModuleFile.path)
    return when {
      parts.size == 2 && parts.first() == ".." && parts.last() != ".." -> parts.last() to true
      parts.first() == ".." -> relativeModuleFile.path to false
      else -> parts.joinToString(":") to false
    }
  }

  companion object {
    fun projectInfo(id: String, base: VirtualFile, useKotlinDsl: Boolean = false, configure: ProjectInfoBuilder.() -> Unit): ProjectInfo {
      return ProjectInfoBuilder(id, base, useKotlinDsl).apply(configure).create()
    }
  }

  data class ProjectInfo(val rootModule: ModuleInfo,
                         val modules: List<ModuleInfo>)

  data class ModuleInfo(val simpleName: String,
                        val ideName: String,
                        val externalName: String,
                        val root: VirtualFile,
                        val groupId: String?,
                        val artifactId: String,
                        val version: String?,
                        val isFlat: Boolean,
                        val useKotlinDsl: Boolean,
                        val modulesPerSourceSet: List<String>)

  inner class ModuleInfoBuilder(val root: VirtualFile, val ideName: String) {

    var simpleName: String? = null

    var groupId: String? = null
    var artifactId: String? = null
    var version: String? = null

    var useKotlinDsl: Boolean? = null

    val modulesPerSourceSet: MutableList<String> = ArrayList()

    fun create(): ModuleInfo {
      val (externalName, isFlat) = getExternalName(root)
      return ModuleInfo(
        simpleName ?: root.name,
        ideName,
        externalName,
        root,
        groupId,
        artifactId ?: simpleName ?: root.name,
        version,
        isFlat,
        useKotlinDsl ?: this@ProjectInfoBuilder.useKotlinDsl,
        modulesPerSourceSet
      )
    }
  }
}