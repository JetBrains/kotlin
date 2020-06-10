// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.configurationStore

import com.intellij.configurationStore.schemeManager.*
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.RoamingType
import com.intellij.openapi.components.StateStorageOperation
import com.intellij.openapi.options.ExternalizableScheme
import com.intellij.openapi.options.SchemeManagerFactory
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.JDOMUtil
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.testFramework.*
import com.intellij.testFramework.rules.InMemoryFsRule
import com.intellij.util.PathUtil
import com.intellij.util.io.*
import com.intellij.util.toByteArray
import com.intellij.util.xmlb.annotations.Tag
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.jdom.Element
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.function.Function

internal const val FILE_SPEC = "REMOTE"

/**
 * Functionality without stream provider covered, ICS has own test suite
 */
internal class SchemeManagerTest {
  companion object {
    @JvmField
    @ClassRule
    val projectRule = ProjectRule()
  }

  @Rule
  @JvmField
  val tempDirManager = TemporaryDirectory()

  @Rule
  @JvmField
  val fsRule = InMemoryFsRule()

  private var localBaseDir: Path? = null
  private var remoteBaseDir: Path? = null

  private fun getTestDataPath() = PlatformTestUtil.getCommunityPath().replace(File.separatorChar, '/') + "/platform/platform-tests/testData/options"

  @Test fun loadSchemes() {
    doLoadSaveTest("options1", "1->first;2->second")
  }

  @Test fun loadSimpleSchemes() {
    doLoadSaveTest("options", "1->1")
  }

  @Test fun deleteScheme() {
    val manager = createAndLoad("options1")
    manager.removeScheme("first")
    manager.save()

    checkSchemes("2->second")
  }

  @Test fun renameScheme() {
    val manager = createAndLoad("options1")

    val scheme = manager.findSchemeByName("first")
    assertThat(scheme).isNotNull
    @Suppress("SpellCheckingInspection")
    scheme!!.name = "Grünwald"
    manager.save()

    @Suppress("SpellCheckingInspection")
    checkSchemes("2->second;Grünwald->Grünwald")
  }

  @Test fun testRenameScheme2() {
    val manager = createAndLoad("options1")

    val first = manager.findSchemeByName("first")
    assertThat(first).isNotNull
    assert(first != null)
    first!!.name = "2"
    val second = manager.findSchemeByName("second")
    assertThat(second).isNotNull
    assert(second != null)
    second!!.name = "1"
    manager.save()

    checkSchemes("1->1;2->2")
  }

  @Test fun testDeleteRenamedScheme() {
    val manager = createAndLoad("options1")

    val firstScheme = manager.findSchemeByName("first")
    assertThat(firstScheme).isNotNull
    assert(firstScheme != null)
    firstScheme!!.name = "first_renamed"
    manager.save()

    checkSchemes(remoteBaseDir!!.resolve("REMOTE"), "first_renamed->first_renamed;2->second", true)
    checkSchemes(localBaseDir!!, "", false)

    firstScheme.name = "first_renamed2"
    manager.removeScheme(firstScheme)
    manager.save()

    checkSchemes(remoteBaseDir!!.resolve("REMOTE"), "2->second", true)
    checkSchemes(localBaseDir!!, "", false)
  }

  @Test fun testDeleteAndCreateSchemeWithTheSameName() {
    val manager = createAndLoad("options1")
    val firstScheme = manager.findSchemeByName("first")
    assertThat(firstScheme).isNotNull

    manager.removeScheme(firstScheme!!)
    manager.addScheme(TestScheme("first"))
    manager.save()
    checkSchemes("2->second;first->first")
  }

  @Test fun testGenerateUniqueSchemeName() {
    val manager = createAndLoad("options1")
    val scheme = TestScheme("first")
    manager.addScheme(scheme, false)

    assertThat("first2").isEqualTo(scheme.name)
  }

  fun TestScheme.save(file: Path) {
    file.write(serialize(this)!!.toByteArray())
  }

  @Test fun `different extensions - old, new`() {
    doDifferentExtensionTest(listOf("1.xml", "1.icls"))
  }

  @Test fun `different extensions - new, old`() {
    doDifferentExtensionTest(listOf("1.icls", "1.xml"))
  }

  private fun doDifferentExtensionTest(fileNames: List<String>) {
    val dir = tempDirManager.newPath()

    val scheme = TestScheme("local", "true")
    scheme.save(dir.resolve("1.icls"))
    TestScheme("local", "false").save(dir.resolve("1.xml"))

    class ATestSchemeProcessor : TestSchemeProcessor(), SchemeExtensionProvider {
      override val schemeExtension = ".icls"
    }

    // use provider to specify exact order of files (it is critical to test both variants - old, new or new, old)
    val schemeManager = SchemeManagerImpl(FILE_SPEC, ATestSchemeProcessor(), object : StreamProvider {
      override val isExclusive = true

      override fun write(fileSpec: String, content: ByteArray, size: Int, roamingType: RoamingType) {
        getFile(fileSpec).write(content, 0, size)
      }

      override fun read(fileSpec: String, roamingType: RoamingType, consumer: (InputStream?) -> Unit): Boolean {
        getFile(fileSpec).inputStream().use(consumer)
        return true
      }

      override fun processChildren(path: String,
                                   roamingType: RoamingType,
                                   filter: (name: String) -> Boolean,
                                   processor: (name: String, input: InputStream, readOnly: Boolean) -> Boolean): Boolean {
        for (name in fileNames) {
          dir.resolve(name).inputStream().use {
            processor(name, it, false)
          }
        }
        return true
      }

      override fun delete(fileSpec: String, roamingType: RoamingType): Boolean {
        getFile(fileSpec).delete()
        return true
      }

      private fun getFile(fileSpec: String) = dir.resolve(fileSpec.substring(FILE_SPEC.length + 1))
    }, dir)
    schemeManager.loadSchemes()
    assertThat(schemeManager.allSchemes).containsOnly(scheme)

    assertThat(dir.resolve("1.icls")).isRegularFile()
    assertThat(dir.resolve("1.xml")).isRegularFile()

    scheme.data = "newTrue"
    schemeManager.save()

    assertThat(dir.resolve("1.icls")).isRegularFile()
    assertThat(dir.resolve("1.xml")).doesNotExist()
  }

  @Test
  fun setSchemes() {
    val dir = fsRule.fs.getPath("/test")
    val schemeManager = SchemeManagerImpl(FILE_SPEC, TestSchemeProcessor(), null, dir, schemeNameToFileName = MODERN_NAME_CONVERTER)
    schemeManager.loadSchemes()
    assertThat(schemeManager.allSchemes).isEmpty()

    @Suppress("SpellCheckingInspection")
    val schemeName = "Grünwald и русский"
    val scheme = TestScheme(schemeName)
    schemeManager.setSchemes(listOf(scheme))

    val schemes = schemeManager.allSchemes
    assertThat(schemes).containsOnly(scheme)

    assertThat(dir.resolve("$schemeName.xml")).doesNotExist()

    scheme.data = "newTrue"
    schemeManager.save()

    assertThat(dir.resolve("$schemeName.xml")).isRegularFile()

    schemeManager.setSchemes(emptyList())

    schemeManager.save()

    assertThat(dir).doesNotExist()
  }

  @Test
  fun `reload schemes`() {
    val dir = fsRule.fs.getPath("/test").createDirectories()
    val schemeManager = createSchemeManager(dir)
    schemeManager.loadSchemes()
    assertThat(schemeManager.allSchemes).isEmpty()

    val scheme = TestScheme("s1", "oldData")
    schemeManager.setSchemes(listOf(scheme))
    assertThat(schemeManager.allSchemes).containsOnly(scheme)
    schemeManager.save()

    dir.resolve("s1.xml").write("""<scheme name="s1" data="newData" />""")
    schemeManager.reload()

    assertThat(schemeManager.allSchemes).containsOnly(TestScheme("s1", "newData"))
  }

  @Test
  fun `ignore dir named as file`() {
    val dir = fsRule.fs.getPath("/test").createDirectories()
    dir.resolve("foo.xml").createDirectories()

    val schemeManager = createSchemeManager(dir)
    schemeManager.loadSchemes()
    assertThat(schemeManager.allSchemes).isEmpty()
  }

  @Test
  fun `reload several schemes`() {
    doReloadTest(UpdateScheme::class.java)
  }

  @Test
  fun `reload - remove and add`() {
    doReloadTest(RemoveScheme::class.java)
  }

  private fun doReloadTest(kind: Class<out SchemeChangeEvent>) {
    val dir = fsRule.fs.getPath("/test").createDirectories()
    fun writeScheme(index: Int, value: String): TestScheme {
      val name = "s$index"
      val data = "data $value for scheme $index"
      dir.resolve("$name.xml").write("""<scheme name="$name" data="$data" />""")
      return TestScheme(name, data)
    }

    var s1 = writeScheme(1, "foo")
    var s2 = writeScheme(2, "foo")

    fun createVirtualFile(scheme: TestScheme): VirtualFile {
      val fileName = "${scheme.name}.xml"
      val file = dir.resolve(fileName)
      return LightVirtualFile(fileName, null, file.readText(), Charsets.UTF_8, Files.getLastModifiedTime(file).toMillis())
    }

    val schemeManager = createSchemeManager(dir)
    schemeManager.loadSchemes()
    assertThat(schemeManager.allSchemes).containsExactly(s1, s2)

    s1 = writeScheme(1, "bar")
    s2 = writeScheme(2, "bar")

    @Suppress("UNCHECKED_CAST")
    val schemeChangeApplicator = SchemeChangeApplicator(schemeManager as SchemeManagerImpl<Any, Any>)
    if (kind == UpdateScheme::class.java) {
      schemeChangeApplicator.reload(listOf(UpdateScheme(createVirtualFile(s1)), UpdateScheme(createVirtualFile(s2))))
    }
    else {
      val sF2 = createVirtualFile(s2)
      val updateEventS1 = UpdateScheme(createVirtualFile(s1))
      val updateEventS2 = UpdateScheme(sF2)
      val events = listOf(updateEventS1, RemoveScheme(sF2.name), updateEventS2)

      assertThat(sortSchemeChangeEvents(events)).containsExactly(updateEventS1, updateEventS2)

      val removeAllSchemes = RemoveAllSchemes()
      assertThat(sortSchemeChangeEvents(listOf(updateEventS1, RemoveScheme("foo"), updateEventS2, removeAllSchemes))).containsExactly(removeAllSchemes)
      assertThat(sortSchemeChangeEvents(listOf(updateEventS1, RemoveScheme("foo"), removeAllSchemes, updateEventS2))).containsExactly(removeAllSchemes, updateEventS2)
      assertThat(sortSchemeChangeEvents(listOf(removeAllSchemes, updateEventS2, RemoveScheme(sF2.name)))).containsExactly(removeAllSchemes, RemoveScheme(sF2.name))

      schemeChangeApplicator.reload(events)
    }

    schemeManager.save()
    assertThat(schemeManager.allSchemes).containsExactly(s1, s2)
  }

  /**
   * This test shows how the interaction between [SchemeManagerImpl] and a []StreamProvider] with different
   * naming styles (e.g. a custom naming logic exposed by [SchemeManagerIprProvider.load]) can put [SchemeManagerImpl]
   * into a bad state where it deletes a scheme right after it tries to save it.
   *
   * This errors shows up as inconsistent outputs from the stream provider used by the scheme manager.  An in-production
   * example of this is [com.intellij.execution.impl.RunManagerImpl], where two identical consecutive calls to
   * [RunManagerImpl.getState] can return different results.
   *
   * The steps to reproduce the error is inlined with the code below.
   */
  @Test
  fun `scheme manager with dependencies using different scheme naming styles`() {
    /**
     * A simple schemes processor that names it's scheme keys with a custom suffix.
     */

    val dir = tempDirManager.newPath()

    /**
     * 1. Create a [StreamProvider] that will later be used to load scheme elements with custom scheme names.
     *    An instance of [SchemeManagerIprProvider] satisfies this criteria.
     */
    val streamProvider = SchemeManagerIprProvider("scheme")

    /**
     * 2. Create a [SchemeProcessor] with custom naming scheme.  See SchemesProcessorWithUniqueNaming in the test
     *    as an example.
     */
    class SchemeProcessorWithUniqueNaming : TestSchemeProcessor() {
      override fun getSchemeKey(scheme: TestScheme) = scheme.name + "someSuffix"
    }

    val schemeProcessor = SchemeProcessorWithUniqueNaming()

    /**
     * 3. Create a [SchemeManagerImpl] with the [StreamProvider] from #1 and [SchemeProcessor] from #2.  We now have
     *    a SchemeManager that can be manipulated to exhibit the error.
     */
    val schemeManager = SchemeManagerImpl(FILE_SPEC, schemeProcessor, streamProvider, dir)

    /**
     * 4. Add a scheme and save it. The scheme manager will now have a scheme named in the style of our
     * [SchemeProcessorWithUniqueNaming] from #2.
     */
    schemeManager.addScheme(TestScheme("first"))
    schemeManager.save()

    /**
     * 5. Obtain the scheme by writing its contents into an element, and then load the element with a different naming scheme.
     *    This creates the scenario where schemeManager and streamProvider refers to the same scheme with different names.
     */
    val element = Element("state")
    streamProvider.writeState(element)
    streamProvider.load(element) { elementToLoad -> elementToLoad.name + "someOtherSuffix" }

    /**
     * 6. [SchemeManagerImpl.reload] reloads it's schemes by deleting it's current set of schemes and reloading it.
     *    Note that the file to delete here and what scheme manager thinks the scheme belongs to have different names.  These
     *    different names come from the different naming styles we defined earlier in the test.
     */
    schemeManager.reload()

    /**
     * 7. By calling [SchemeManagerImpl.save], we delete the file our currently existing scheme uses.
     *    Now [SchemeManagerImpl.save] should remove that deleted file from it's list of staged files to delete.
     *    However, because the file names don't match, the file isn't removed.  This means the file is STILL staged
     *    for deletion.  The saving process also corrects the scheme's file name if it's different from what [SchemeManager]
     *    sees; this restores our scheme to use the same name given by our scheme processor from #2.
     */
    schemeManager.save()
    val firstElement = Element("state")
    streamProvider.writeState(firstElement)

    /**
     * We have now successfully put our SchemeManagerImpl in the BAD STATE:
     * - [SchemeManagerImpl] has a file staged for deletion.
     * - [SchemeManagerImpl] ALSO has an existing scheme that is backed by the same file.
     *
     * This means [SchemeManagerImpl] will delete the file backing a scheme that's still in use.  The deletion happens
     * on the next call to [SchemeManagerImpl.save].
     */

    /**
     * 8. Calling save will delete the file backing our scheme that's still in use.  [streamProvider.writeState] will now
     *    write an empty element, because the backing file was deleted.
     */
    schemeManager.save()
    val secondElement = Element("state")
    streamProvider.writeState(secondElement)

    assertThat(firstElement.children.size).isEqualTo(secondElement.children.size)
  }

  @Test fun `save only if scheme differs from bundled`() {
    val dir = tempDirManager.newPath()
    var schemeManager = createSchemeManager(dir)
    val bundledPath = "/com/intellij/configurationStore/bundledSchemes/default"
    schemeManager.loadBundledScheme(bundledPath, this)
    val customScheme = TestScheme("default")
    assertThat(schemeManager.allSchemes).containsOnly(customScheme)

    schemeManager.save()
    assertThat(dir).doesNotExist()

    schemeManager.save()
    schemeManager.setSchemes(listOf(customScheme))
    assertThat(dir).doesNotExist()

    assertThat(schemeManager.allSchemes).containsOnly(customScheme)

    customScheme.data = "foo"
    schemeManager.save()
    assertThat(dir.resolve("default.xml")).isRegularFile()

    schemeManager = createSchemeManager(dir)
    schemeManager.loadBundledScheme(bundledPath, this)
    schemeManager.loadSchemes()

    assertThat(schemeManager.allSchemes).containsOnly(customScheme)
  }

  @Test fun `don't remove dir if no schemes but at least one non-hidden file exists`() {
    val dir = tempDirManager.newPath()
    val schemeManager = createSchemeManager(dir)

    val scheme = TestScheme("s1")
    schemeManager.setSchemes(listOf(scheme))

    schemeManager.save()

    val schemeFile = dir.resolve("s1.xml")
    assertThat(schemeFile).isRegularFile()

    schemeManager.setSchemes(emptyList())

    dir.resolve("empty").write(byteArrayOf())

    schemeManager.save()

    assertThat(schemeFile).doesNotExist()
    assertThat(dir).isDirectory()
  }

  @Test fun `remove empty directory only if some file was deleted`() {
    val dir = tempDirManager.newPath()
    val schemeManager = createSchemeManager(dir)
    schemeManager.loadSchemes()

    dir.createDirectories()
    schemeManager.save()
    assertThat(dir).isDirectory()

    schemeManager.addScheme(TestScheme("test"))
    schemeManager.save()
    assertThat(dir).isDirectory()

    schemeManager.setSchemes(emptyList())
    schemeManager.save()
    assertThat(dir).doesNotExist()
  }

  @Test fun rename() {
    val dir = tempDirManager.newPath()
    val schemeManager = createSchemeManager(dir)
    schemeManager.loadSchemes()
    assertThat(schemeManager.allSchemes).isEmpty()

    val scheme = TestScheme("s1")
    schemeManager.setSchemes(listOf(scheme))

    val schemes = schemeManager.allSchemes
    assertThat(schemes).containsOnly(scheme)

    assertThat(dir.resolve("s1.xml")).doesNotExist()

    scheme.data = "newTrue"
    schemeManager.save()

    assertThat(dir.resolve("s1.xml")).isRegularFile()

    scheme.name = "s2"

    schemeManager.save()

    assertThat(dir.resolve("s1.xml")).doesNotExist()
    assertThat(dir.resolve("s2.xml")).isRegularFile()
  }

  @Test fun `rename A to B and B to A`() {
    val dir = tempDirManager.newPath()
    val schemeManager = createSchemeManager(dir)

    val a = TestScheme("a", "a")
    val b = TestScheme("b", "b")
    schemeManager.setSchemes(listOf(a, b))
    schemeManager.save()

    assertThat(dir.resolve("a.xml")).isRegularFile()
    assertThat(dir.resolve("b.xml")).isRegularFile()

    a.name = "b"
    b.name = "a"

    schemeManager.save()

    assertThat(dir.resolve("a.xml").readText()).isEqualTo("""<scheme name="a" data="b" />""")
    assertThat(dir.resolve("b.xml").readText()).isEqualTo("""<scheme name="b" data="a" />""")
  }

  @Test
  fun `VFS - rename A to B and B to A`() {
    val dir = tempDirManager.newPath(refreshVfs = true)
    val busDisposable = Disposer.newDisposable()
    try {
      val schemeManager = SchemeManagerImpl(FILE_SPEC, TestSchemeProcessor(), null, dir, fileChangeSubscriber = { schemeManager ->
        @Suppress("UNCHECKED_CAST")
        val schemeFileTracker = SchemeFileTracker(schemeManager as SchemeManagerImpl<Any, Any>, projectRule.project)
        ApplicationManager.getApplication().messageBus.connect(busDisposable).subscribe(VirtualFileManager.VFS_CHANGES, schemeFileTracker)
      })

      val a = TestScheme("a", "a")
      val b = TestScheme("b", "b")
      schemeManager.setSchemes(listOf(a, b))
      runInEdtAndWait { schemeManager.save() }

      assertThat(dir.resolve("a.xml")).isRegularFile()
      assertThat(dir.resolve("b.xml")).isRegularFile()

      a.name = "b"
      b.name = "a"

      runInEdtAndWait { schemeManager.save() }

      assertThat(dir.resolve("a.xml").readText()).isEqualTo("""<scheme name="a" data="b" />""")
      assertThat(dir.resolve("b.xml").readText()).isEqualTo("""<scheme name="b" data="a" />""")
    }
    finally {
      Disposer.dispose(busDisposable)
    }
  }

  @Test
  fun `VFS - vf resolver`() {
    val dir = tempDirManager.newPath(refreshVfs = true)
    val busDisposable = Disposer.newDisposable()
    try {
      val requestedPaths = linkedSetOf<String>()
      val schemeManager = SchemeManagerImpl(FILE_SPEC, TestSchemeProcessor(), null, dir, fileChangeSubscriber = null, virtualFileResolver = object: VirtualFileResolver {
        override fun resolveVirtualFile(path: String, reasonOperation: StateStorageOperation): VirtualFile? {
          requestedPaths.add(PathUtil.getFileName(path))
          return super.resolveVirtualFile(path, reasonOperation)
        }
      })

      val a = TestScheme("a", "a")
      val b = TestScheme("b", "b")
      schemeManager.setSchemes(listOf(a, b))
      runInEdtAndWait { schemeManager.save() }

      schemeManager.reload()
      assertThat(requestedPaths).containsExactly("VFS - vf resolver")
    }
    finally {
      Disposer.dispose(busDisposable)
    }
  }

  @Test fun `path must not contains ROOT_CONFIG macro`() {
    assertThatThrownBy { SchemeManagerFactory.getInstance().create("\$ROOT_CONFIG$/foo", TestSchemeProcessor()) }.hasMessage("Path must not contains ROOT_CONFIG macro, corrected: foo")
  }

  @Test fun `path must be system-independent`() {
    assertThatThrownBy { SchemeManagerFactory.getInstance().create("foo\\bar", TestSchemeProcessor())}.hasMessage("Path must be system-independent, use forward slash instead of backslash")
  }

  private fun createSchemeManager(dir: Path) = SchemeManagerImpl(FILE_SPEC, TestSchemeProcessor(), null, dir)

  private fun createAndLoad(testData: String): SchemeManagerImpl<TestScheme, TestScheme> {
    createTempFiles(testData)
    return createAndLoad()
  }

  private fun doLoadSaveTest(testData: String, expected: String, localExpected: String = "") {
    val schemesManager = createAndLoad(testData)
    schemesManager.save()
    checkSchemes(remoteBaseDir!!.resolve("REMOTE"), expected, true)
    checkSchemes(localBaseDir!!, localExpected, false)
  }

  private fun checkSchemes(expected: String) {
    checkSchemes(remoteBaseDir!!.resolve("REMOTE"), expected, true)
    checkSchemes(localBaseDir!!, "", false)
  }

  private fun createAndLoad(): SchemeManagerImpl<TestScheme, TestScheme> {
    val schemesManager = SchemeManagerImpl(FILE_SPEC, TestSchemeProcessor(), MockStreamProvider(remoteBaseDir!!), localBaseDir!!)
    schemesManager.loadSchemes()
    return schemesManager
  }

  private fun createTempFiles(testData: String) {
    val temp = tempDirManager.newPath()
    localBaseDir = temp.resolve("__local")
    remoteBaseDir = temp
    FileUtil.copyDir(File("${getTestDataPath()}/$testData"), temp.resolve("REMOTE").toFile())
  }
}

private fun checkSchemes(baseDir: Path, expected: String, ignoreDeleted: Boolean) {
  val filesToScheme = StringUtil.split(expected, ";")
  val fileToSchemeMap = HashMap<String, String>()
  for (fileToScheme in filesToScheme) {
    val index = fileToScheme.indexOf("->")
    fileToSchemeMap.put(fileToScheme.substring(0, index), fileToScheme.substring(index + 2))
  }

  baseDir.directoryStreamIfExists {
    for (file in it) {
      val fileName = FileUtil.getNameWithoutExtension(file.fileName.toString())
      if ("--deleted" == fileName && ignoreDeleted) {
        assertThat(fileToSchemeMap).containsKey(fileName)
      }
    }
  }

  for (file in fileToSchemeMap.keys) {
    assertThat(baseDir.resolve("$file.xml")).isRegularFile()
  }

  baseDir.directoryStreamIfExists {
    for (file in it) {
      val scheme = JDOMUtil.load(file).deserialize(TestScheme::class.java)
      assertThat(fileToSchemeMap.get(FileUtil.getNameWithoutExtension(file.fileName.toString()))).isEqualTo(scheme.name)
    }
  }
}

@Tag("scheme")
data class TestScheme(@field:com.intellij.util.xmlb.annotations.Attribute @field:kotlin.jvm.JvmField var name: String = "", @field:com.intellij.util.xmlb.annotations.Attribute var data: String? = null) : ExternalizableScheme, SerializableScheme {
  override fun getName() = name

  override fun setName(value: String) {
    name = value
  }

  override fun writeScheme() = serialize(this)!!
}

open class TestSchemeProcessor : LazySchemeProcessor<TestScheme, TestScheme>() {
  override fun createScheme(dataHolder: SchemeDataHolder<TestScheme>,
                            name: String,
                            attributeProvider: Function<in String, String?>,
                            isBundled: Boolean): TestScheme {
    val scheme = dataHolder.read().deserialize(TestScheme::class.java)
    dataHolder.updateDigest(scheme)
    return scheme
  }
}