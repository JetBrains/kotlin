// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
@file:Suppress("UsePropertyAccessSyntax")
package com.intellij.openapi.projectRoots.impl.jdkDownloader

import com.intellij.openapi.util.SystemInfo
import com.intellij.testFramework.rules.TempDirectory
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assume
import org.junit.Rule
import org.junit.Test
import java.io.File

internal fun jdkItemForTest(url: String,
                            packageType: JdkPackageType,
                            size: Long,
                            sha256: String,
                            prefix: String = "") = JdkItem(
  JdkProduct("Vendor", null, null),
  false,
  123,
  "123.123",
  null,
  null,
  "suggested",
  "jetbrains-hardware",
  packageType,
  url,
  sha256,
  size,
  10 * size,
  prefix,
  url.split("/").last(),
  url.split("/").last().removeSuffix(".tar.gz").removeSuffix(".zip")
)

class JdkDownloaderTest {
  @get:Rule
  val fsRule = TempDirectory()


  private val mockTarGZ = jdkItemForTest(packageType = JdkPackageType.TAR_GZ,
                                         url = "https://repo.labs.intellij.net/idea-test-data/jdk-download-test-data.tar.gz",
                                         size = 249,
                                         sha256 = "ffc8825d96e3f89cb4a8ca64b9684c37f55d6c5bd54628ebf984f8282f8a59ff"
  )

  private val mockTarGZ2 = jdkItemForTest(packageType = JdkPackageType.TAR_GZ,
                                          url = "https://repo.labs.intellij.net/idea-test-data/jdk-download-test-data-2.tar.gz",
                                          size = 318,
                                          sha256 = "963af2c1578a376340f60c5adabf217f59006cfc8b2b3fc97edda2e90c0295e2"
  )
  private val mockZip = jdkItemForTest(packageType = JdkPackageType.ZIP,
                                       url = "https://repo.labs.intellij.net/idea-test-data/jdk-download-test-data.zip",
                                       size = 604,
                                       sha256 = "1cf15536c1525f413190fd53243f343511a17e6ce7439ccee4dc86f0d34f9e81")

  @Test
  fun `unpacking tar gz`() = testUnpacking(mockTarGZ) { dir ->
    assertThat(File(dir, "TheApp/FooBar.app/theApp")).isFile()
    assertThat(File(dir, "TheApp/QPCV/ggg.txt")).isFile()
  }

  @Test
  fun `unpacking tar gz cut dirs`() = testUnpacking(mockTarGZ.copy(unpackPrefixFilter = "TheApp/FooBar.app")) { dir ->
    assertThat(File(dir, "theApp")).isFile()
    assertThat(File(dir, "ggg.txt")).doesNotExist()
  }

  @Test
  fun `unpacking tar gz cut dirs 2`() {
    Assume.assumeTrue(SystemInfo.isMac || SystemInfo.isLinux)

    testUnpacking(mockTarGZ2.copy(unpackPrefixFilter = "this/jdk")) { dir ->
      assertThat(File(dir, "bin/java")).isFile()
      assertThat(File(dir, "bin/javac")).isFile()
      assertThat(File(dir, "file")).isFile()
      assertThat(File(dir, "bin/symlink").toPath()).isSymbolicLink().hasSameContentAs(File(dir, "file").toPath())
    }
  }

  @Test
  fun `unpacking tar gz cut dirs complex prefix`() = testUnpacking(mockTarGZ.copy(unpackPrefixFilter = "./TheApp/FooBar.app")) { dir ->
    assertThat(File(dir, "theApp")).isFile()
    assertThat(File(dir, "ggg.txt")).doesNotExist()
  }

  @Test
  fun `unpacking tar gz cut dirs and prefix`() = testUnpacking(
    mockTarGZ.copy(
      unpackPrefixFilter = "TheApp/FooBar.app")
  ) { dir ->
    assertThat(File(dir, "theApp")).isFile()
    assertThat(File(dir, "ggg.txt")).doesNotExist()
  }

  @Test(expected = Exception::class)
  fun `unpacking targz invalid size`() = testUnpacking(mockTarGZ.copy(archiveSize = 234234)) { dir ->
    assertThat(File(dir, "TheApp/FooBar.app/theApp")).isFile()
    assertThat(File(dir, "TheApp/QPCV/ggg.txt")).isFile()
  }

  @Test(expected = Exception::class)
  fun `unpacking targz invalid checksum`() = testUnpacking(mockTarGZ.copy(sha256 = "234234")) { dir ->
    assertThat(File(dir, "TheApp/FooBar.app/theApp")).isFile()
    assertThat(File(dir, "TheApp/QPCV/ggg.txt")).isFile()
  }

  @Test
  fun `unpacking zip`() = testUnpacking(mockZip) { dir ->
    assertThat(File(dir, "folder/readme2")).isDirectory()
    assertThat(File(dir, "folder/file")).isFile()
  }

  @Test(expected = Exception::class)
  fun `unpacking zip invalid size`() = testUnpacking(mockZip.copy(archiveSize = 234)) { dir ->
    assertThat(File(dir, "folder/readme2")).isDirectory()
    assertThat(File(dir, "folder/file")).isFile()
  }

  @Test(expected = Exception::class)
  fun `unpacking zip invalid checksum`() = testUnpacking(mockZip.copy(sha256 = "234")) { dir ->
    assertThat(File(dir, "folder/readme2")).isDirectory()
    assertThat(File(dir, "folder/file")).isFile()
  }

  @Test
  fun `unpacking zip cut dirs and wrong prefix`() = testUnpacking(
    mockZip.copy(
      unpackPrefixFilter = "wrong")
  ) { dir ->
    assertThat(File(dir, "folder/readme2")).doesNotExist()
    assertThat(File(dir, "folder/file")).doesNotExist()
  }

  @Test
  fun `unpacking zip cut dirs and prefix`() = testUnpacking(
    mockZip.copy(
      unpackPrefixFilter = "folder")
  ) { dir ->
    assertThat(File(dir, "readme2")).isDirectory()
    assertThat(File(dir, "file")).isFile()
  }

  @Test
  fun `unpacking zip and prefix`() = testUnpacking(
    mockZip.copy(
      unpackPrefixFilter = "folder/file")
  ) { dir ->
    assertThat(File(dir, "readme2")).doesNotExist()
    assertThat(File(dir, "folder/file")).doesNotExist()
  }

  private inline fun testUnpacking(item: JdkItem, resultDir: (File) -> Unit) {
    val dir = fsRule.newFolder()
    JdkInstaller.installJdk(
      JdkInstallRequest(item, dir), null)
    resultDir(dir)
  }
}
