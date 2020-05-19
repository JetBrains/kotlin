// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.projectRoots.impl.jdkDownloader

import com.intellij.idea.TestFor
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.ui.configuration.UnknownSdkLocalSdkFix
import com.intellij.testFramework.LightPlatformTestCase
import org.junit.Assert
import org.junit.Test

class JdkRequirementTest : LightPlatformTestCase() {

  @Test
  @TestFor(issues = ["IDEA-235968"])
  fun test_j9() = doTest("adopt-openj9-14",
                         matches = listOf(),
                         fails = listOf("1.8", "9", "11", "12", "9.14")
  )

  @Test
  @TestFor(issues = ["IDEA-235968"])
  fun test_bare_j9() = doTestNoParse("openj9")

  @Test
  @TestFor(issues = ["IDEA-235968"])
  fun test_j9m() = doTestNoParse("adopt-openj9-14-")

  @Test
  fun test_corretto_11_0_4() = doTest("corretto-11.0.4",
                                      matches = listOf(),
                                      fails = listOf("11", "11.0.4", "12", "10"))

  @Test
  fun test_corretto_jdk_11_0_4() = doTestJdkItems("corretto-11.0.4",
                                                  matches = listOf(
                                                    jdkItem(Corretto, "11.0.5"),
                                                    jdkItem(Corretto, "11.0.6")
                                                  ),
                                                  fails = listOf(
                                                    jdkItem(Corretto, "12.0.5"),
                                                    jdkItem(Zulu, "11.0.4")
                                                  ))

  @Test
  fun test_corretto_sp_jdk_11_0_4() = doTestJdkItems("corretto 11.0.4",
                                                  matches = listOf(
                                                    jdkItem(Corretto, "11.0.5"),
                                                    jdkItem(Corretto, "11.0.6")
                                                  ),
                                                  fails = listOf(
                                                    jdkItem(Corretto, "12.0.5"),
                                                    jdkItem(Zulu, "11.0.4")
                                                  ))

  @Test
  fun test_amazon_jdk_11_0_4() = doTestJdkItems("amazon-11.0.4",
                                                  matches = listOf(
                                                    jdkItem(Corretto, "11.0.5"),
                                                    jdkItem(Corretto, "11.0.6")
                                                  ),
                                                  fails = listOf(
                                                    jdkItem(Corretto, "12.0.5"),
                                                    jdkItem(Zulu, "11.0.4")
                                                  ))
  @Test
  fun test_exact_amazon_jdk_11_0_4() = doTestJdkItems("==amazon-11.0.4",
                                                  matches = listOf(
                                                    jdkItem(Corretto, "11.0.4")
                                                  ),
                                                  fails = listOf(
                                                    jdkItem(Corretto, "12.0.5"),
                                                    jdkItem(Zulu, "11.0.4"),
                                                    jdkItem(Corretto, "11.0"),
                                                    jdkItem(Corretto, "11.0.3"),
                                                    jdkItem(Corretto, "11.0.5")
                                                  ))
  @Test
  fun test_qqq_jdk_11_0_4() = doTestJdkItems("qqq-11.0.4",
                                                  fails = listOf(
                                                    jdkItem(Corretto, "11.0.5"),
                                                    jdkItem(Corretto, "11.0.6"),
                                                    jdkItem(Corretto, "12.0.5"),
                                                    jdkItem(Zulu, "11.0.4")
                                                  ))

  @Test
  fun test_corretto_11() = doTest("corretto-11", fails = listOf("11"))

  @Test
  fun test_11() = doTest("11", matches = listOf("11", "11.0", "11.1", "11.0.4"), fails = listOf("12", "1.8", "6", "19", "8"))

  @Test
  fun test_exact_11_0_4() = doTest("=11.0.4", matches = listOf("11.0.4"), fails = listOf("12", "1.8", "6", "19", "8", "11", "11.0", "11.0.3", "11.0.5"))

  @Test
  fun test_9() = doTest("9", matches = listOf("9", "9.0.1"), fails = listOf("1.7", "1.8", "8", "10"))

  @Test
  fun test_8() = doTest("1.8", matches = listOf("1.8", "1.8.2333", "1.8.0_232"), fails = listOf("1.7", "1.9", "9", "11"))

  @Test
  fun test_8_special() = doTest("1.8.0_123", matches = listOf("1.8.0_232", "1.8.8_123"), fails = listOf("1.7", "1.9", "9", "11", "1.8", "1.8.0", "1.8.0_12"))

  @Test
  fun test_bare_8() = doTest("8", matches = listOf("1.8.0_232", "1.8", "8"), fails = listOf("1.7", "1.9", "9", "11"))

  @Test
  fun test_7() = doTest("1.7", matches = listOf("1.7", "1.7.0", "1.7.0_2342", "7"), fails = listOf("1.6", "1.8", "11"))

  @Test
  fun test_6() = doTest("1.6", matches = listOf("1.6", "1.6.0", "1.6.0_123", "6"), fails = listOf("1.5", "7", "1.7", "8", "11"))

  @Test
  fun test_6_0() = doTest("1.6.0", matches = listOf("1.6", "1.6.0", "1.6.0_123", "6"), fails = listOf("1.5", "7", "1.7", "8", "11"))

  @Test
  fun test_6_0_b() = doTest("1.6.0_234", matches = listOf("1.6.0_235", "1.6.0_444"), fails = listOf("1.5", "7", "1.7", "8", "11", "1.6", "1.6.0", "1.6.0_123", "6"))

  @Test
  fun test_custom() = doFailedTest("idea jdk")

  @Test
  fun test_custom2() = doFailedTest("myjava sdk")

  private fun doFailedTest(text: String) {
    Assert.assertNull(JdkRequirements.parseRequirement(text))
  }

  private fun doTestNoParse(text: String) {
    val req = JdkRequirements.parseRequirement(text)
    Assert.assertNull(req)
  }

  private fun doTest(text: String,
                     matches: List<String> = listOf(),
                     fails: List<String> = listOf()) {
    val req = JdkRequirements.parseRequirement(text)
    Assert.assertNotNull(req)
    req!!
    LOG.debug("parsed requirement: $req")

    for (match in matches) {
      Assert.assertTrue("$req matches $match", req.matches(match))
    }

    for (match in fails) {
      Assert.assertFalse("$req !matches $match", req.matches(match))
    }
  }

  private fun JdkRequirement.matches(version: String) = matches(toLocalMatch(version))

  private fun toLocalMatch(versionString: String) = object : UnknownSdkLocalSdkFix {
    override fun getExistingSdkHome() = "mock-home"
    override fun getVersionString() = versionString
    override fun getSuggestedSdkName() = versionString
    override fun configureSdk(sdk: Sdk) {}
  }

  private val Corretto = JdkProduct("Amazon", "Corretto", null)
  private val Zulu = JdkProduct("Azul", "Zulu", null)

  private fun jdkItem(vendor: JdkProduct, version: String): JdkItem {
    val mock = jdkItemForTest("https://jonnyzzz.com", JdkPackageType.ZIP, 123, "sha23")
    return mock.copy(
      product = vendor,
      jdkVersion = version
    )
  }

  private fun doTestJdkItems(text: String,
                             matches: List<JdkItem> = listOf(),
                             fails: List<JdkItem> = listOf()) {
    val req = JdkRequirements.parseRequirement(text)
    Assert.assertNotNull(req)
    req!!
    LOG.debug("parsed requirement: $req")

    for (match in matches) {
      Assert.assertTrue("$req matches $match", req.matches(match))
    }

    for (match in fails) {
      Assert.assertFalse("$req !matches $match", req.matches(match))
    }
  }

}