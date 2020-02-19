// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
@file:Suppress("UsePropertyAccessSyntax")
package com.intellij.openapi.projectRoots.impl.jdkDownloader

import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.io.FileUtil
import com.intellij.testFramework.LightPlatformTestCase
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assume
import java.io.File

internal fun jdkItemForTest(url: String,
                            packageType: JdkPackageType,
                            size: Long,
                            sha256: String,
                            prefix: String = ""): JdkItem {
  val product = JdkProduct(vendor = "Vendor", product = null, flavour = null)
  return JdkItem(
    product,
    isDefaultItem = false,
    isVisibleOnUI = true,
    jdkMajorVersion = 123,
    jdkVersion = "123.123",
    jdkVendorVersion = null,
    suggestedSdkName = "suggested",
    arch = "jetbrains-hardware",
    packageType = packageType,
    url = url,
    sha256 = sha256,
    archiveSize = size,
    unpackedSize = 10 * size,
    unpackPrefixFilter = prefix,
    archiveFileName = url.split("/").last(),
    installFolderName = url.split("/").last().removeSuffix(".tar.gz").removeSuffix(".zip"),
    sharedIndexAliases = listOf()
  )
}

class JdkDownloaderTest : LightPlatformTestCase() {
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

  fun `test unpacking tar gz`() = testUnpacking(mockTarGZ) { dir ->
    assertThat(File(dir, "TheApp/FooBar.app/theApp")).isFile()
    assertThat(File(dir, "TheApp/QPCV/ggg.txt")).isFile()
  }

  fun `test unpacking tar gz cut dirs`() = testUnpacking(mockTarGZ.copy(unpackPrefixFilter = "TheApp/FooBar.app")) { dir ->
    assertThat(File(dir, "theApp")).isFile()
    assertThat(File(dir, "ggg.txt")).doesNotExist()
  }

  fun `test unpacking tar gz cut dirs 2`() {
    Assume.assumeTrue(SystemInfo.isMac || SystemInfo.isLinux)

    testUnpacking(mockTarGZ2.copy(unpackPrefixFilter = "this/jdk")) { dir ->
      assertThat(File(dir, "bin/java")).isFile()
      assertThat(File(dir, "bin/javac")).isFile()
      assertThat(File(dir, "file")).isFile()
      assertThat(File(dir, "bin/symlink").toPath()).isSymbolicLink().hasSameContentAs(File(dir, "file").toPath())
    }
  }

  fun `test unpacking tar gz cut dirs complex prefix`() = testUnpacking(mockTarGZ.copy(unpackPrefixFilter = "./TheApp/FooBar.app")) { dir ->
    assertThat(File(dir, "theApp")).isFile()
    assertThat(File(dir, "ggg.txt")).doesNotExist()
  }

  fun `test unpacking tar gz cut dirs and prefix`() = testUnpacking(
    mockTarGZ.copy(
      unpackPrefixFilter = "TheApp/FooBar.app")
  ) { dir ->
    assertThat(File(dir, "theApp")).isFile()
    assertThat(File(dir, "ggg.txt")).doesNotExist()
  }

  fun `test unpacking targz invalid size`() = expectsException {
    testUnpacking(mockTarGZ.copy(archiveSize = 234234))
  }

  fun `test unpacking targz invalid checksum`() = expectsException { testUnpacking(mockTarGZ.copy(sha256 = "234234")) }

  fun `test unpacking zip`() = testUnpacking(mockZip) { dir ->
    assertThat(File(dir, "folder/readme2")).isDirectory()
    assertThat(File(dir, "folder/file")).isFile()
  }

  fun `test unpacking zip invalid size`() = expectsException {
    testUnpacking(mockZip.copy(archiveSize = 234))
  }

  fun `test unpacking zip invalid checksum`() = expectsException { testUnpacking(mockZip.copy(sha256 = "234")) }

  fun `test unpacking zip cut dirs and wrong prefix`() = testUnpacking(
    mockZip.copy(
      unpackPrefixFilter = "wrong")
  ) { dir ->
    assertThat(File(dir, "folder/readme2")).doesNotExist()
    assertThat(File(dir, "folder/file")).doesNotExist()
  }

  fun `test unpacking zip cut dirs and prefix`() = testUnpacking(
    mockZip.copy(
      unpackPrefixFilter = "folder")
  ) { dir ->
    assertThat(File(dir, "readme2")).isDirectory()
    assertThat(File(dir, "file")).isFile()
  }

  fun `test unpacking zip and prefix`() = testUnpacking(
    mockZip.copy(
      unpackPrefixFilter = "folder/file")
  ) { dir ->
    assertThat(File(dir, "readme2")).doesNotExist()
    assertThat(File(dir, "folder/file")).doesNotExist()
  }

  private fun testUnpacking(item: JdkItem, resultDir: (File) -> Unit = { error("must not reach here") }) {
    val dir = createTempDir()
    try {
      JdkInstaller.getInstance().installJdk(JdkInstallRequest(item, dir), null, null)
      resultDir(dir)
    }
    finally {
      FileUtil.delete(dir)
    }
  }

  private fun expectsException(action: () -> Unit) {
    try {
      action()
      error("Exception was expected")
    }
    catch (t: Exception) {
      return
    }
  }
}
