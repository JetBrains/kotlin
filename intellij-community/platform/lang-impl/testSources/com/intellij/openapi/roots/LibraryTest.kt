// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.roots

import com.intellij.openapi.application.runWriteActionAndWait
import com.intellij.openapi.roots.impl.libraries.LibraryEx
import com.intellij.openapi.vfs.JarFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.testFramework.ApplicationRule
import com.intellij.testFramework.assertions.Assertions
import com.intellij.testFramework.rules.ProjectModelRule
import org.assertj.core.api.Assertions.assertThat
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test

class LibraryTest {
  companion object {
    @JvmField
    @ClassRule
    val appRule = ApplicationRule()
  }

  @Rule
  @JvmField
  val projectModel = ProjectModelRule()

  @Test
  fun `empty library`() {
    val library = projectModel.addProjectLevelLibrary("a")
    assertThat(library.name).isEqualTo("a")
    assertThat(library.getUrls(OrderRootType.CLASSES)).isEmpty()
    assertThat(library.table).isEqualTo(projectModel.projectLibraryTable)
    assertThat(library.excludedRoots).isEmpty()
    assertThat(library.getInvalidRootUrls(OrderRootType.CLASSES)).isEmpty()
    assertThat(library.source).isNull()
    assertThat(library.module).isNull()
    checkConsistency(library)
  }

  @Test
  fun `add remove roots`() {
    val classesRoot = projectModel.baseProjectDir.newVirtualDirectory("classes")
    val sourceRoot = projectModel.baseProjectDir.newVirtualDirectory("src")
    val library = projectModel.addProjectLevelLibrary("a") {
      it.addRoot(classesRoot, OrderRootType.CLASSES)
    }
    checkConsistency(library)
    assertThat(library.getFiles(OrderRootType.CLASSES)).containsExactly(classesRoot)
    assertThat(library.getFiles(OrderRootType.SOURCES)).isEmpty()
    assertThat(library.isValid(classesRoot.url, OrderRootType.CLASSES)).isTrue()
    edit(library) {
      assertThat((it as LibraryEx).source).isSameAs(library)
      assertThat(it.isChanged).isFalse()
      it.addRoot(sourceRoot, OrderRootType.SOURCES)
      assertThat(it.isChanged).isTrue()
      assertThat(it.getFiles(OrderRootType.CLASSES)).containsExactly(classesRoot)
      assertThat(it.getFiles(OrderRootType.SOURCES)).containsExactly(sourceRoot)
      assertThat(library.getFiles(OrderRootType.SOURCES)).isEmpty()
    }
    assertThat(library.getFiles(OrderRootType.CLASSES)).containsExactly(classesRoot)
    assertThat(library.getFiles(OrderRootType.SOURCES)).containsExactly(sourceRoot)
    edit(library) {
      it.removeRoot(sourceRoot.url, OrderRootType.CLASSES)
      assertThat(it.isChanged).isFalse()
      it.removeRoot(classesRoot.url, OrderRootType.CLASSES)
      assertThat(it.isChanged).isTrue()
      assertThat(it.getFiles(OrderRootType.CLASSES)).isEmpty()
      assertThat(it.getFiles(OrderRootType.SOURCES)).containsExactly(sourceRoot)
      assertThat(library.getFiles(OrderRootType.CLASSES)).containsExactly(classesRoot)
    }
    assertThat(library.getFiles(OrderRootType.CLASSES)).isEmpty()
    assertThat(library.getFiles(OrderRootType.SOURCES)).containsExactly(sourceRoot)
  }

  @Test
  fun `add remove invalid root`() {
    val classesRootDir = projectModel.baseProjectDir.newVirtualDirectory("lib")
    val classesUrl = "${classesRootDir.url}/classes"
    val library = projectModel.addProjectLevelLibrary("a") {
      it.addRoot(classesUrl, OrderRootType.CLASSES)
      assertThat(it.getUrls(OrderRootType.CLASSES)).containsExactly(classesUrl)
      assertThat(it.getFiles(OrderRootType.CLASSES)).isEmpty()
    }
    checkConsistency(library)
    assertThat(library.getFiles(OrderRootType.CLASSES)).isEmpty()
    assertThat(library.getUrls(OrderRootType.CLASSES)).containsExactly(classesUrl)
    assertThat(library.isValid(classesUrl, OrderRootType.CLASSES)).isFalse()
    assertThat(library.getInvalidRootUrls(OrderRootType.CLASSES)).containsExactly(classesUrl)
    edit(library) {
      val removed = it.removeRoot(classesUrl, OrderRootType.SOURCES)
      assertThat(removed).isFalse()
      assertThat(it.isChanged).isFalse()
      val removed2 = it.removeRoot(classesUrl, OrderRootType.CLASSES)
      assertThat(removed2).isTrue()
      assertThat(it.isChanged).isTrue()
    }
    assertThat(library.getUrls(OrderRootType.CLASSES)).isEmpty()
    assertThat(library.getInvalidRootUrls(OrderRootType.CLASSES)).isEmpty()
    assertThat(library.isValid(classesUrl, OrderRootType.CLASSES)).isFalse()
  }

  @Test
  fun `add invalid root and make it valid`() {
    val classesRootDir = projectModel.baseProjectDir.newVirtualDirectory("lib")
    val classesUrl = "${classesRootDir.url}/classes"
    val library = projectModel.addProjectLevelLibrary("a") {
      it.addRoot(classesUrl, OrderRootType.CLASSES)
    }
    checkConsistency(library)
    assertThat(library.isValid(classesUrl, OrderRootType.CLASSES)).isFalse()
    assertThat(library.getInvalidRootUrls(OrderRootType.CLASSES)).containsExactly(classesUrl)
    val classesRoot = projectModel.baseProjectDir.newVirtualDirectory("lib/classes")
    assertThat(library.getUrls(OrderRootType.CLASSES)).containsExactly(classesUrl)
    assertThat(library.getFiles(OrderRootType.CLASSES)).containsExactly(classesRoot)
    assertThat(library.getInvalidRootUrls(OrderRootType.CLASSES)).isEmpty()
    assertThat(library.isValid(classesUrl, OrderRootType.CLASSES)).isTrue()
  }

  @Test
  fun `add remove jar directory`() {
    val jarDir = projectModel.baseProjectDir.newVirtualDirectory("jarDir")
    val jarDirSrc = projectModel.baseProjectDir.newVirtualDirectory("jarDirSrc")
    val jarDirRec = projectModel.baseProjectDir.newVirtualDirectory("jarDirRec")
    val jarDirSrcRec = projectModel.baseProjectDir.newVirtualDirectory("jarDirSrcRec")
    val library = projectModel.addProjectLevelLibrary("a") {
      it.addJarDirectory(jarDir, false)
      it.addJarDirectory(jarDirRec, true)
      it.addJarDirectory(jarDirSrc.url, false, OrderRootType.SOURCES)
      it.addJarDirectory(jarDirSrcRec.url, true, OrderRootType.SOURCES)
      assertThat(it.isJarDirectory(jarDir.url)).isTrue()
      assertThat(it.isJarDirectory(jarDirSrc.url)).isFalse()
      assertThat(it.isJarDirectory(jarDirSrc.url, OrderRootType.SOURCES)).isTrue()
      assertThat(it.isJarDirectory(jarDirRec.url)).isTrue()
      assertThat(it.isJarDirectory(jarDirSrcRec.url, OrderRootType.SOURCES)).isTrue()
      assertThat(it.getFiles(OrderRootType.CLASSES)).isEmpty()
      assertThat(it.getUrls(OrderRootType.CLASSES)).containsExactly(jarDir.url, jarDirRec.url)
    }
    checkConsistency(library)
    assertThat(library.getUrls(OrderRootType.CLASSES)).containsExactly(jarDir.url, jarDirRec.url)
    assertThat(library.isJarDirectory(jarDir.url)).isTrue()
    assertThat(library.isJarDirectory(jarDirSrc.url)).isFalse()
    assertThat(library.isJarDirectory(jarDirSrc.url, OrderRootType.SOURCES)).isTrue()
    assertThat(library.isJarDirectory(jarDirRec.url)).isTrue()
    assertThat(library.isJarDirectory(jarDirSrcRec.url, OrderRootType.SOURCES)).isTrue()
    assertThat(library.getFiles(OrderRootType.CLASSES)).isEmpty()

    fun VirtualFile.toJarRoot() = JarFileSystem.getInstance().getJarRootForLocalFile(this)
    val classesRoot = projectModel.baseProjectDir.newVirtualFile("jarDir/a.jar").toJarRoot()
    projectModel.baseProjectDir.newVirtualFile("jarDir/subDir/b.jar")
    assertThat(library.getFiles(OrderRootType.CLASSES)).containsExactly(classesRoot)
    val classesRootRec1 = projectModel.baseProjectDir.newVirtualFile("jarDirRec/a.jar").toJarRoot()
    val classesRootRec2 = projectModel.baseProjectDir.newVirtualFile("jarDirRec/subDir/a.jar").toJarRoot()
    assertThat(library.getFiles(OrderRootType.CLASSES)).containsExactly(classesRoot, classesRootRec1, classesRootRec2)
    checkConsistency(library)

    edit(library) {
      it.removeRoot(jarDir.url, OrderRootType.CLASSES)
      it.removeRoot(jarDirRec.url, OrderRootType.CLASSES)
      assertThat(it.isChanged)
    }
    assertThat(library.getUrls(OrderRootType.CLASSES)).isEmpty()
  }

  @Test
  fun `add remove excluded root`() {
    val classesRoot = projectModel.baseProjectDir.newVirtualDirectory("classes")
    val excludedRoot = projectModel.baseProjectDir.newVirtualDirectory("classes/exc")
    val library = projectModel.addProjectLevelLibrary("a") {
      it.addRoot(classesRoot, OrderRootType.CLASSES)
      it.addExcludedRoot(excludedRoot.url)
      assertThat(it.excludedRootUrls).containsExactly(excludedRoot.url)
    }
    assertThat(library.excludedRoots).containsExactly(excludedRoot)

    edit(library) {
      it.removeExcludedRoot(excludedRoot.url)
      assertThat(it.isChanged)
    }
    assertThat(library.excludedRoots).isEmpty()
  }

  @Test
  fun `remove excluded root when parent is removed`() {
    val classesRoot = projectModel.baseProjectDir.newVirtualDirectory("classes")
    val excludedRoot = projectModel.baseProjectDir.newVirtualDirectory("classes/exc")
    val library = projectModel.addProjectLevelLibrary("a") {
      it.addRoot(classesRoot, OrderRootType.CLASSES)
      it.addExcludedRoot(excludedRoot.url)
    }
    assertThat(library.excludedRoots).containsExactly(excludedRoot)

    edit(library) {
      it.removeRoot(classesRoot.url, OrderRootType.CLASSES)
    }
    assertThat(library.excludedRoots).isEmpty()
  }

  @Test
  fun rename() {
    val library = projectModel.addProjectLevelLibrary("a")
    edit(library) {
      it.name = "b"
      assertThat(it.isChanged).isTrue()
      assertThat(it.name).isEqualTo("b")
    }
    assertThat(library.name).isEqualTo("b")
  }

  @Test
  fun `move roots up and down`() {
    val root1 = projectModel.baseProjectDir.newVirtualDirectory("root1")
    val src = projectModel.baseProjectDir.newVirtualDirectory("src")
    val root2 = projectModel.baseProjectDir.newVirtualDirectory("root2")
    val library = projectModel.addProjectLevelLibrary("a") {
      it.addRoot(root1, OrderRootType.CLASSES)
      it.addRoot(src, OrderRootType.SOURCES)
      it.addRoot(root2, OrderRootType.CLASSES)
      assertThat(it.getFiles(OrderRootType.CLASSES)).containsExactly(root1, root2)
      it.moveRootDown(root1.url, OrderRootType.CLASSES)
      assertThat(it.getFiles(OrderRootType.CLASSES)).containsExactly(root2, root1)
    }
    assertThat(library.getFiles(OrderRootType.CLASSES)).containsExactly(root2, root1)

    edit(library) {
      it.moveRootUp(root2.url, OrderRootType.CLASSES)
      assertThat(it.isChanged).isFalse()
      assertThat(it.getFiles(OrderRootType.CLASSES)).containsExactly(root2, root1)
      it.moveRootUp(root1.url, OrderRootType.CLASSES)
      assertThat(it.isChanged).isTrue()
      assertThat(it.getFiles(OrderRootType.CLASSES)).containsExactly(root1, root2)
    }

    assertThat(library.getFiles(OrderRootType.CLASSES)).containsExactly(root1, root2)
  }

  @Test
  fun `root set listener`() {
    val library = projectModel.addProjectLevelLibrary("a")
    var changeCount = 0
    library.rootProvider.addRootSetChangedListener { changeCount++ }
    edit(library) { model ->
      assertThat(changeCount).isEqualTo(0)
      model.addRoot(projectModel.baseProjectDir.newVirtualDirectory("classes"), OrderRootType.CLASSES)
      assertThat(changeCount).isEqualTo(0)
    }
    assertThat(changeCount).isEqualTo(1)
  }

  @Test
  fun `commit without changes`() {
    val library = projectModel.addProjectLevelLibrary("a")
    var changeCount = 0
    library.rootProvider.addRootSetChangedListener { changeCount++ }
    edit(library) { model ->
      assertThat(changeCount).isEqualTo(0)
    }
    assertThat(changeCount).isEqualTo(0)
  }

  private fun checkConsistency(library: LibraryEx) {
    assertThat(library.rootProvider.getFiles(OrderRootType.CLASSES)).containsExactly(*library.getFiles(OrderRootType.CLASSES))
    assertThat(library.rootProvider.getFiles(OrderRootType.SOURCES)).containsExactly(*library.getFiles(OrderRootType.SOURCES))
    assertThat(library.rootProvider.getUrls(OrderRootType.CLASSES)).containsExactly(*library.getUrls(OrderRootType.CLASSES))
    assertThat(library.rootProvider.getUrls(OrderRootType.SOURCES)).containsExactly(*library.getUrls(OrderRootType.SOURCES))
    if (library.getUrls(OrderRootType.CLASSES).none { library.isJarDirectory(it) }) {
      val classesRoots = library.getUrls(OrderRootType.CLASSES).mapNotNull { VirtualFileManager.getInstance().findFileByUrl(it) }
      assertThat(library.getFiles(OrderRootType.CLASSES)).containsExactly(*classesRoots.toTypedArray())
    }
    if (library.getUrls(OrderRootType.SOURCES).none { library.isJarDirectory(it, OrderRootType.SOURCES) }) {
      val sourcesRoots = library.getUrls(OrderRootType.SOURCES).mapNotNull { VirtualFileManager.getInstance().findFileByUrl(it) }
      assertThat(library.getFiles(OrderRootType.SOURCES)).containsExactly(*sourcesRoots.toTypedArray())
    }
    val excludedRoots = library.excludedRootUrls.mapNotNull { VirtualFileManager.getInstance().findFileByUrl(it) }
    assertThat(library.excludedRoots).containsExactly(*excludedRoots.toTypedArray())
  }

  private inline fun edit(library: LibraryEx, action: (LibraryEx.ModifiableModelEx) -> Unit) {
    checkConsistency(library)
    val model = library.modifiableModel
    action(model)
    runWriteActionAndWait { model.commit() }
    checkConsistency(library)
  }
}