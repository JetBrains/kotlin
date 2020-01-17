// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.service.project.manage

import com.intellij.openapi.externalSystem.util.PathPrefixTreeMap
import com.intellij.testFramework.UsefulTestCase
import org.junit.Test

class PathPrefixTreeMapTest : UsefulTestCase() {

  @Test
  fun `test map filling`() {
    val map = PathPrefixTreeMap<Int>()
    map["C://path/to/my/dir3"] = 30
    map["C://path/to/my/dir4"] = 10
    map["C://path/to/dir1"] = 11
    map["C://path/to/dir2"] = 21
    map["C://path/to/my"] = 43
    map["C://path"] = 13
    assertEquals(6, map.size)
    assertEquals(map["C://path/to/my/dir1"], null)
    assertEquals(map["C://path/to/my/dir2"], null)
    assertEquals(map["C://path/to/my/dir3"], 30)
    assertEquals(map["C://path/to/my/dir4"], 10)
    assertEquals(map["C://path/to/dir1"], 11)
    assertEquals(map["C://path/to/dir2"], 21)
    assertEquals(map["C://path/to/dir3"], null)
    assertEquals(map["C://path/to/dir4"], null)
    assertEquals(map["C://path/to/my"], 43)
    assertEquals(map["C://path"], 13)
    assertFalse(map.contains("C://path/to/my/dir1"))
    assertFalse(map.contains("C://path/to/my/dir2"))
    assertTrue(map.contains("C://path/to/my/dir3"))
    assertTrue(map.contains("C://path/to/my/dir4"))
    assertTrue(map.contains("C://path/to/dir1"))
    assertTrue(map.contains("C://path/to/dir2"))
    assertFalse(map.contains("C://path/to/dir3"))
    assertFalse(map.contains("C://path/to/dir4"))
    assertTrue(map.contains("C://path/to/my"))
    assertTrue(map.contains("C://path"))
    map["C://path/to/my/dir1"] = 10
    map["C://path/to/my/dir2"] = 20
    map["C://path/to/dir3"] = 30
    map["C://path/to/dir4"] = 11
    assertEquals(10, map.size)
    assertEquals(map["C://path/to/my/dir1"], 10)
    assertEquals(map["C://path/to/my/dir2"], 20)
    assertEquals(map["C://path/to/my/dir3"], 30)
    assertEquals(map["C://path/to/my/dir4"], 10)
    assertEquals(map["C://path/to/dir1"], 11)
    assertEquals(map["C://path/to/dir2"], 21)
    assertEquals(map["C://path/to/dir3"], 30)
    assertEquals(map["C://path/to/dir4"], 11)
    assertEquals(map["C://path/to/my"], 43)
    assertEquals(map["C://path"], 13)
    assertTrue(map.contains("C://path/to/my/dir1"))
    assertTrue(map.contains("C://path/to/my/dir2"))
    assertTrue(map.contains("C://path/to/my/dir3"))
    assertTrue(map.contains("C://path/to/my/dir4"))
    assertTrue(map.contains("C://path/to/dir1"))
    assertTrue(map.contains("C://path/to/dir2"))
    assertTrue(map.contains("C://path/to/dir3"))
    assertTrue(map.contains("C://path/to/dir4"))
    assertTrue(map.contains("C://path/to/my"))
    assertTrue(map.contains("C://path"))
    assertEquals(setOf(10, 20, 30, 10, 11, 21, 30, 11, 43, 13), map.values.toSet())
    assertEquals(setOf(
      "C://path/to/my/dir1",
      "C://path/to/my/dir2",
      "C://path/to/my/dir3",
      "C://path/to/my/dir4",
      "C://path/to/dir1",
      "C://path/to/dir2",
      "C://path/to/dir3",
      "C://path/to/dir4",
      "C://path/to/my",
      "C://path"
    ), map.keys)
  }

  @Test
  fun `test map removes`() {
    val map = PathPrefixTreeMap<Int>()
    map["C://path/to/my/dir1"] = 10
    map["C://path/to/my/dir2"] = 20
    map["C://path/to/my/dir3"] = 30
    map["C://path/to/my/dir4"] = 10
    map["C://path/to/dir1"] = 11
    map["C://path/to/dir2"] = 21
    map["C://path/to/dir3"] = 30
    map["C://path/to/dir4/"] = 11
    map["C://path/to/my"] = 43
    map["C://path"] = 13
    assertEquals(10, map.size)
    assertEquals(map["C://path/to/my/dir1"], 10)
    assertEquals(map["C://path/to/my/dir2"], 20)
    assertEquals(map["C://path/to/my/dir3"], 30)
    assertEquals(map["C://path/to/my/dir4"], 10)
    assertEquals(map["C://path/to/dir1"], 11)
    assertEquals(map["C://path/to/dir2"], 21)
    assertEquals(map["C://path/to/dir3"], 30)
    assertEquals(map["C://path/to/dir4"], 11)
    assertEquals(map["C://path/to/my"], 43)
    assertEquals(map["C://path"], 13)
    assertTrue(map.contains("C://path/to/my/dir1"))
    assertTrue(map.contains("C://path/to/my/dir2"))
    assertTrue(map.contains("C://path/to/my/dir3"))
    assertTrue(map.contains("C://path/to/my/dir4"))
    assertTrue(map.contains("C://path/to/dir1"))
    assertTrue(map.contains("C://path/to/dir2"))
    assertTrue(map.contains("C://path/to/dir3"))
    assertTrue(map.contains("C://path/to/dir4"))
    assertTrue(map.contains("C://path/to/my"))
    assertTrue(map.contains("C://path"))
    map.remove("C://path/to/my/dir1/")
    map.remove("C://path/to/my/dir2")
    map.remove("C://path/to/dir3")
    map.remove("C://path/to/dir4")
    assertEquals(6, map.size)
    assertEquals(map["C://path/to/my/dir1"], null)
    assertEquals(map["C://path/to/my/dir2"], null)
    assertEquals(map["C://path/to/my/dir3"], 30)
    assertEquals(map["C://path/to/my/dir4"], 10)
    assertEquals(map["C://path/to/dir1"], 11)
    assertEquals(map["C://path/to/dir2"], 21)
    assertEquals(map["C://path/to/dir3/"], null)
    assertEquals(map["C://path/to/dir4"], null)
    assertEquals(map["C://path/to/my"], 43)
    assertEquals(map["C://path"], 13)
    assertFalse(map.contains("C://path/to/my/dir1"))
    assertFalse(map.contains("C://path/to/my/dir2"))
    assertTrue(map.contains("C://path/to/my/dir3"))
    assertTrue(map.contains("C://path/to/my/dir4"))
    assertTrue(map.contains("C://path/to/dir1"))
    assertTrue(map.contains("C://path/to/dir2"))
    assertFalse(map.contains("C://path/to/dir3"))
    assertFalse(map.contains("C://path/to/dir4"))
    assertTrue(map.contains("C://path/to/my"))
    assertTrue(map.contains("C://path"))
    assertEquals(setOf(30, 10, 11, 21, 43, 13), map.values.toSet())
    assertEquals(setOf(
      "C://path/to/my/dir3",
      "C://path/to/my/dir4",
      "C://path/to/dir1",
      "C://path/to/dir2",
      "C://path/to/my",
      "C://path"
    ), map.keys)
  }


  @Test
  fun `test containing nullable values`() {
    val map = PathPrefixTreeMap<Int?>()
    map["C://path/to/my/dir1"] = null
    map["C://path/to/my/dir2"] = 20
    map["C://path/to/my/dir3/"] = null
    map["C://path/to/my/dir4/"] = 10
    map["C://path/to/dir1"] = null
    map["C://path/to/dir2"] = 21
    map["C://path/to/dir3"] = null
    map["C://path/to/dir4"] = 11
    map["C://path/to/my"] = 43
    map["C://path"] = 13
    assertEquals(10, map.size)
    assertEquals(map["C://path/to/my/dir1"], null)
    assertEquals(map["C://path/to/my/dir2"], 20)
    assertEquals(map["C://path/to/my/dir3"], null)
    assertEquals(map["C://path/to/my/dir4"], 10)
    assertEquals(map["C://path/to/dir1"], null)
    assertEquals(map["C://path/to/dir2"], 21)
    assertEquals(map["C://path/to/dir3"], null)
    assertEquals(map["C://path/to/dir4/"], 11)
    assertEquals(map["C://path/to/my"], 43)
    assertEquals(map["C://path"], 13)
    assertTrue(map.contains("C://path/to/my/dir1"))
    assertTrue(map.contains("C://path/to/my/dir2"))
    assertTrue(map.contains("C://path/to/my/dir3"))
    assertTrue(map.contains("C://path/to/my/dir4/"))
    assertTrue(map.contains("C://path/to/dir1"))
    assertTrue(map.contains("C://path/to/dir2"))
    assertTrue(map.contains("C://path/to/dir3"))
    assertTrue(map.contains("C://path/to/dir4"))
    assertTrue(map.contains("C://path/to/my"))
    assertTrue(map.contains("C://path"))
    map.remove("C://path/to/my/dir1")
    map.remove("C://path/to/my/dir2")
    map.remove("C://path/to/dir3")
    map.remove("C://path/to/dir4")
    assertEquals(6, map.size)
    assertEquals(map["C://path/to/my/dir1"], null)
    assertEquals(map["C://path/to/my/dir2"], null)
    assertEquals(map["C://path/to/my/dir3"], null)
    assertEquals(map["C://path/to/my/dir4"], 10)
    assertEquals(map["C://path/to/dir1/"], null)
    assertEquals(map["C://path/to/dir2"], 21)
    assertEquals(map["C://path/to/dir3"], null)
    assertEquals(map["C://path/to/dir4"], null)
    assertEquals(map["C://path/to/my"], 43)
    assertEquals(map["C://path"], 13)
    assertFalse(map.contains("C://path/to/my/dir1/"))
    assertFalse(map.contains("C://path/to/my/dir2"))
    assertTrue(map.contains("C://path/to/my/dir3"))
    assertTrue(map.contains("C://path/to/my/dir4"))
    assertTrue(map.contains("C://path/to/dir1"))
    assertTrue(map.contains("C://path/to/dir2"))
    assertFalse(map.contains("C://path/to/dir3"))
    assertFalse(map.contains("C://path/to/dir4"))
    assertTrue(map.contains("C://path/to/my"))
    assertTrue(map.contains("C://path"))
    assertEquals(setOf(null, 10, null, 21, 43, 13), map.values.toSet())
    assertEquals(setOf(
      "C://path/to/my/dir3",
      "C://path/to/my/dir4",
      "C://path/to/dir1",
      "C://path/to/dir2",
      "C://path/to/my",
      "C://path"
    ), map.keys)
  }

  @Test
  fun `test get all elements under dir`() {
    val map = PathPrefixTreeMap<Int?>()
    map["C://path/to/my/dir1/"] = 10
    map["C://path/to/my/dir2"] = 20
    map["C://path/to/my/dir3/"] = 30
    map["C://path/to/my/dir4"] = 10
    map["C://path/to/dir1"] = 11
    map["C://path/to/dir2/"] = 21
    map["C://path/to/dir3"] = 30
    map["C://path/to/dir4/"] = 11
    map["C://path/to/my"] = 43
    map["C://path"] = 13

    assertEquals(setOf("C://path/to/my/dir1" to 10,
                       "C://path/to/my/dir2" to 20,
                       "C://path/to/my/dir3" to 30,
                       "C://path/to/my/dir4" to 10,
                       "C://path/to/my" to 43),
                 map.getAllDescendantPairs("C://path/to/my"))
    assertEquals(setOf("C://path/to/my/dir1" to 10,
                       "C://path/to/my/dir2" to 20,
                       "C://path/to/my/dir3" to 30,
                       "C://path/to/my/dir4" to 10,
                       "C://path/to/dir1" to 11,
                       "C://path/to/dir2" to 21,
                       "C://path/to/dir3" to 30,
                       "C://path/to/dir4" to 11,
                       "C://path/to/my" to 43),
                 map.getAllDescendantPairs("C://path/to"))
    assertEquals(setOf("C://path/to/my/dir1" to 10,
                       "C://path/to/my/dir2" to 20,
                       "C://path/to/my/dir3" to 30,
                       "C://path/to/my/dir4" to 10,
                       "C://path/to/dir1" to 11,
                       "C://path/to/dir2" to 21,
                       "C://path/to/dir3" to 30,
                       "C://path/to/dir4" to 11,
                       "C://path/to/my" to 43,
                       "C://path" to 13),
                 map.getAllDescendantPairs("C://path"))
    assertEquals(setOf("C://path/to/my/dir1" to 10,
                       "C://path/to/my/dir2" to 20,
                       "C://path/to/my/dir3" to 30,
                       "C://path/to/my/dir4" to 10,
                       "C://path/to/dir1" to 11,
                       "C://path/to/dir2" to 21,
                       "C://path/to/dir3" to 30,
                       "C://path/to/dir4" to 11,
                       "C://path/to/my" to 43,
                       "C://path" to 13),
                 map.getAllDescendantPairs("C:/"))
  }

  @Test
  fun `test usage with unix paths`() {
    val map = PathPrefixTreeMap<Int?>()
    map["/path/to/my/dir1/"] = 10
    map["/path/to/my/dir2"] = 20
    map["/path/to/my/dir3/"] = 30
    assertEquals(3, map.size)
    assertEquals(10, map.remove("/path/to/my/dir1/"))
    assertEquals(2, map.size)
    assertEquals(null, map.remove("/path/to/my/dir1/"))
    assertEquals(2, map.size)
    map["/path/to/my/dir4"] = 10
    map["/path/to/dir1"] = 11
    map["/path/to/dir2/"] = 21
    assertEquals(5, map.size)
    assertEquals(11, map.remove("/path/to/dir1/"))
    assertEquals(4, map.size)
    map["/path/to/dir3"] = 30
    assertEquals(5, map.size)
    assertEquals(10, map.remove("/path/to/my/dir4/"))
    assertEquals(4, map.size)
    map["/path/to/dir4/"] = 11
    assertEquals(5, map.size)
    assertEquals(null, map.remove("/path"))
    assertEquals(5, map.size)
    map["/path/to/my"] = 43
    map["/path"] = 13
    assertEquals(7, map.size)
    assertEquals(13, map.remove("/path"))
    assertEquals(6, map.size)
    assertFalse(map.contains("/path/to/my/dir1"))
    assertTrue(map.contains("/path/to/my/dir2"))
    assertTrue(map.contains("/path/to/my/dir3"))
    assertFalse(map.contains("/path/to/my/dir4/"))
    assertFalse(map.contains("/path/to/dir1"))
    assertTrue(map.contains("/path/to/dir2/"))
    assertTrue(map.contains("/path/to/dir3"))
    assertTrue(map.contains("/path/to/dir4/"))
    assertTrue(map.contains("/path/to/my"))
    assertFalse(map.contains("/path"))
  }

  @Test
  fun `test find all ancestors`() {
    val map = PathPrefixTreeMap<Int>()
    map["C://path/to/my/dir1/"] = 10
    map["C://path/to/my/dir2"] = 20
    map["C://path/to/my/dir3/"] = 30
    map["C://path/to/my/dir4"] = 10
    map["C://path/to/dir1"] = 11
    map["C://path/to/dir2/"] = 21
    map["C://path/to/dir3"] = 30
    map["C://path/to/dir4/"] = 11
    map["C://path/to/my"] = 43
    map["C://path"] = 13
    assertEquals(listOf("C://path" to 13, "C://path/to/my" to 43, "C://path/to/my/dir1" to 10),
                 map.getAllAncestorPairs("C://path/to/my/dir1/loc"))
    assertEquals(listOf("C://path" to 13, "C://path/to/my" to 43, "C://path/to/my/dir1" to 10),
                 map.getAllAncestorPairs("C://path/to/my/dir1"))
    assertEquals(listOf("C://path" to 13, "C://path/to/my" to 43), map.getAllAncestorPairs("C://path/to/my"))
    assertEquals(listOf("C://path" to 13), map.getAllAncestorPairs("C://path/to"))
    assertEquals(listOf("C://path" to 13), map.getAllAncestorPairs("C://path/to/"))
    assertEquals(listOf("C://path" to 13), map.getAllAncestorPairs("C://path"))
    assertEquals(emptyList<Any>(), map.getAllAncestorPairs("C:/"))
  }

  private fun <T> PathPrefixTreeMap<T>.getAllDescendantPairs(key: String) =
    getAllDescendants(key).map { (k, v) -> k to v }.toSet()

  private fun <T> PathPrefixTreeMap<T>.getAllAncestorPairs(key: String) =
    getAllAncestors(key).map { (k, v) -> k to v }
}