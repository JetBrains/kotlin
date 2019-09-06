// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.configurationStore

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ConfigImportHelper
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.openapi.components.stateStore
import com.intellij.openapi.diagnostic.logger
import com.intellij.testFramework.ApplicationRule
import com.intellij.testFramework.rules.InMemoryFsRule
import com.intellij.util.io.*
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.description.Description
import org.junit.Assert.assertEquals
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.attribute.FileTime
import java.util.*

private val LOG = logger<ConfigImportHelperTest>()

class ConfigImportHelperTest {
  companion object {
    @JvmField
    @ClassRule
    val appRule = ApplicationRule()

    @JvmField
    @ClassRule
    val localTempFolder = TemporaryFolder()
  }

  @JvmField
  @Rule
  val fsRule = InMemoryFsRule()

  @Test
  fun configDirectoryIsValidForImport() {
    PropertiesComponent.getInstance().setValue("property.ConfigImportHelperTest", true)
    try {
      useAppConfigDir {
        runBlocking { ApplicationManager.getApplication().stateStore.save(forceSavingAllSettings = true) }

        val config = Paths.get(PathManager.getConfigPath())
        assertThat(ConfigImportHelper.isConfigDirectory(config))
          .`as`(description {
            "${config} exists=${config.exists()} options=${config.resolve("options").directoryStreamIfExists { it.toList() }}"
          })
          .isTrue()
      }
    }
    finally {
      PropertiesComponent.getInstance().unsetValue("property.ConfigImportHelperTest")
    }
  }

  @Test
  fun `find recent config directory on macOS`() {
    doTest(true)
  }

  @Test
  fun `find recent config directory`() {
    doTest(false)
  }

  private fun doTest(isMacOs: Boolean) {
    val fs = fsRule.fs
    writeStorageFile("2020.1", 100, isMacOs)
    writeStorageFile("2021.1", 200, isMacOs)
    writeStorageFile("2022.1", 300, isMacOs)

    val newConfigPath = fs.getPath("/data/${constructConfigPath("2022.3", isMacOs)}")
    // create new config dir to test that it will be not suggested too (as on start of new version config dir can be created)
    newConfigPath.createDirectories()

    assertThat(ConfigImportHelper.findConfigDirectories(newConfigPath, isMacOs, false).joinToString("\n")).isEqualTo("""
        /data/${constructConfigPath("2022.1", isMacOs)}
        /data/${constructConfigPath("2021.1", isMacOs)}
        /data/${constructConfigPath("2020.1", isMacOs)}
      """.trimIndent())

    writeStorageFile("2021.1", 400, isMacOs)
    assertThat(ConfigImportHelper.findConfigDirectories(newConfigPath, isMacOs, false).joinToString("\n")).isEqualTo("""
        /data/${constructConfigPath("2021.1", isMacOs)}
        /data/${constructConfigPath("2022.1", isMacOs)}
        /data/${constructConfigPath("2020.1", isMacOs)}
      """.trimIndent())
  }

  @Test
  fun `sort if no anchor files`() {
    val isMacOs = true
    fun storageDir(version: String) {
      fsRule.fs.getPath("/data/" + (constructConfigPath(version, isMacOs))).createDirectories()
    }

    storageDir("2022.1")
    storageDir("2021.1")
    storageDir("2020.1")

    val newConfigPath = fsRule.fs.getPath("/data/${constructConfigPath("2022.3", isMacOs)}")
    // create new config dir to test that it will be not suggested too (as on start of new version config dir can be created)
    newConfigPath.createDirectories()

    assertThat(ConfigImportHelper.findConfigDirectories(newConfigPath, isMacOs, false)
                 .joinToString("\n", transform = { it.fileName.toString().removePrefix("IntelliJIdea") }))
      .isEqualTo("""
        2022.1
        2021.1
        2020.1
      """.trimIndent())
  }

  @Test
  fun `sort some real historic config dirs`() {
    val isMacOs = true
    fun storageDir(version: String, millis: Long) {
      val path = fsRule.fs.getPath("/data/" + (constructConfigPath(version, isMacOs, "DataGrip")))
      path.createDirectories()
      if (millis > 0) {
        val child = path.resolve(PathManager.OPTIONS_DIRECTORY + "/" + StoragePathMacros.NON_ROAMABLE_FILE)
        child.parent.createDirectories()
        Files.createFile(child)
        Files.setLastModifiedTime(child, FileTime.fromMillis(millis))
      }
    }

    storageDir("2016.1", 1485460719000)
    storageDir("2016.2", 1466345662000)
    storageDir("2017.1", 1503006763000)
    storageDir("10", 1455035096000)
    storageDir("2018.1", 1531238044000)
    storageDir("2017.2", 1510866492000)
    storageDir("2016.3", 1520869009000)
    storageDir("2017.3", 1549092322000)
    storageDir("2018.2", 1548076635000)
    storageDir("2019.1", 1548225505000)
    storageDir("2019.2", 0)
    storageDir("2018.3", 1545844523000)

    val newConfigPath = fsRule.fs.getPath("/data/${constructConfigPath("2019.2", isMacOs, "DataGrip")}")
    assertThat(ConfigImportHelper.findConfigDirectories(newConfigPath, isMacOs, false)
                 .joinToString("\n", transform = { it.fileName.toString().removePrefix("DataGrip") }))
      .isEqualTo("""
        2017.3
        2019.1
        2018.2
        2018.3
        2018.1
        2016.3
        2017.2
        2017.1
        2016.1
        2016.2
        10""".trimIndent())
  }

  @Test
  fun `set keymap - old version`() {
    doKeyMapTest("2016.4", isMigrationExpected = true)
    doKeyMapTest("2019.1", isMigrationExpected = true)
  }

  @Test
  fun `set keymap - new version`() {
    doKeyMapTest("2019.2", isMigrationExpected = false)
    doKeyMapTest("2019.3", isMigrationExpected = false)
  }

  private fun doKeyMapTest(version: String, isMigrationExpected: Boolean) {
    val oldConfigDir = fsRule.fs.getPath("/data/" + (constructConfigPath(version, true, "DataGrip")))
    oldConfigDir.createDirectories()
    val newConfigDir = fsRule.fs.getPath("/data/" + (constructConfigPath("2019.2", true, "DataGrip")))
    ConfigImportHelper.setKeymapIfNeeded(oldConfigDir, newConfigDir, LOG)

    val optionFile = newConfigDir.resolve("options/keymap.xml")
    if (isMigrationExpected) {
      assertThat(optionFile.readText()).isEqualTo("""
        <application>
          <component name="KeymapManager">
            <active_keymap name="Mac OS X" />
          </component>
        </application>
      """.trimIndent())
    }
    else {
      assertThat(optionFile).doesNotExist()
    }
  }

  @Test
  fun `migrate settings to empty folder`() {
    val unique = UUID.randomUUID()
    val oldPath = localTempFolder.newFolder("oldConfig$unique")
    val newPath = localTempFolder.newFolder("newConfig$unique")

    val oldPluginPath = File(oldPath, "plugins")
    oldPluginPath.mkdirs()
    File(oldPluginPath, "myOldPlugin").createNewFile()

    val newPluginPath = File(newPath, "plugins")

    ConfigImportHelper.doImport(oldPath.toPath(), newPath.toPath(), oldPath.toPath(), oldPluginPath.toPath(), newPluginPath.toPath(), LOG)

    val newPathFiles = newPluginPath.listFiles()!!
    assertEquals(1, newPathFiles.size)
    assertEquals("myOldPlugin", newPathFiles[0].name)
  }

  @Test
  fun `migrate settings to folder with existing plugin`() {
    val unique = UUID.randomUUID()
    val oldPath = localTempFolder.newFolder("oldConfig$unique")
    val newPath = localTempFolder.newFolder("newConfig$unique")

    val oldPluginPath = File(oldPath, "plugins")
    oldPluginPath.mkdirs()
    File(oldPluginPath, "myOldPlugin").createNewFile()

    val newPluginPath = File(newPath, "plugins")
    newPluginPath.mkdirs()
    File(newPluginPath, "myNewPlugin").createNewFile()

    ConfigImportHelper.doImport(oldPath.toPath(), newPath.toPath(), oldPath.toPath(), oldPluginPath.toPath(), newPluginPath.toPath(), LOG)

    val newPathFiles = newPluginPath.listFiles()!!
    assertEquals(1, newPathFiles.size)
    assertEquals("myNewPlugin", newPathFiles[0].name)
  }

  private fun writeStorageFile(version: String, lastModified: Long, isMacOs: Boolean) {
    val dir = fsRule.fs.getPath("/data/" + (constructConfigPath(version, isMacOs)))
    val file = dir.resolve(PathManager.OPTIONS_DIRECTORY + '/' + StoragePathMacros.NON_ROAMABLE_FILE)
    Files.setLastModifiedTime(file.write(version), FileTime.fromMillis(lastModified))
  }
}

private fun constructConfigPath(version: String, isMacOs: Boolean, product: String = "IntelliJIdea"): String {
  return "${if (isMacOs) "" else "."}$product$version" + if (isMacOs) "" else "/${ConfigImportHelper.CONFIG}"
}

private fun description(block: () -> String) = object : Description() {
  override fun value() = block()
}
