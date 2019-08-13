// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.service.project.manage

import com.intellij.openapi.externalSystem.util.PathPrefixTreeMapImpl
import com.intellij.testFramework.UsefulTestCase
import org.junit.Test

class PathPrefixTreeMapImplTest : UsefulTestCase() {

  @Test
  fun `test map filling`() {
    val map = PathPrefixTreeMapImpl<Int>()
    map["C://path/to/my/dir3"] = 30
    map["C://path/to/my/dir4"] = 10
    map["C://path/to/dir1"] = 11
    map["C://path/to/dir2"] = 21
    map["C://path/to/my"] = 43
    map["C://path"] = 13
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
    ), map.paths.toSet())
  }

  @Test
  fun `test map removes`() {
    val map = PathPrefixTreeMapImpl<Int>()
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
    ), map.paths.toSet())
  }


  @Test
  fun `test containing nullable values`() {
    val map = PathPrefixTreeMapImpl<Int?>()
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
    ), map.paths.toSet())
  }

  @Test
  fun `test get all elements under dir`() {
    val map = PathPrefixTreeMapImpl<Int?>()
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
    val underMy = map.getAllDescendants("C://path/to/my").toSet()
    assertEquals(setOf(10, 20, 30, 10, 43), underMy)
    val underMyDir = map.getAllDescendants("C://path/to/my/").toSet()
    assertEquals(setOf(10, 20, 30, 10, 43), underMyDir)
    val underTo = map.getAllDescendants("C://path/to").toSet()
    assertEquals(setOf(10, 20, 30, 10, 43, 11, 21, 30, 11, 43), underTo)
    val underToDir = map.getAllDescendants("C://path/to/").toSet()
    assertEquals(setOf(10, 20, 30, 10, 43, 11, 21, 30, 11, 43), underToDir)
    val underPath = map.getAllDescendants("C://path").toSet()
    assertEquals(setOf(10, 20, 30, 10, 43, 11, 21, 30, 11, 43, 13), underPath)
    val underProto = map.getAllDescendants("C:/").toSet()
    assertEquals(setOf(10, 20, 30, 10, 43, 11, 21, 30, 11, 43, 13), underProto)
  }

  @Test
  fun `test usage with unix paths`() {
    val map = PathPrefixTreeMapImpl<Int?>()
    map["/path/to/my/dir1/"] = 10
    map["/path/to/my/dir2"] = 20
    map["/path/to/my/dir3/"] = 30
    assertEquals(10, map.remove("/path/to/my/dir1/"))
    assertEquals(null, map.remove("/path/to/my/dir1/"))
    map["/path/to/my/dir4"] = 10
    map["/path/to/dir1"] = 11
    map["/path/to/dir2/"] = 21
    assertEquals(11, map.remove("/path/to/dir1/"))
    map["/path/to/dir3"] = 30
    assertEquals(10, map.remove("/path/to/my/dir4/"))
    map["/path/to/dir4/"] = 11
    assertEquals(null, map.remove("/path"))
    map["/path/to/my"] = 43
    map["/path"] = 13
    assertEquals(13, map.remove("/path"))
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
    val map = PathPrefixTreeMapImpl<Boolean>()
    map["C://path/to/my/dir1/"] = true
    map["C://path/to/my/dir2"] = true
    map["C://path/to/my/dir3/"] = true
    map["C://path/to/my/dir4"] = true
    map["C://path/to/dir1"] = true
    map["C://path/to/dir2/"] = true
    map["C://path/to/dir3"] = true
    map["C://path/to/dir4/"] = true
    map["C://path/to/my"] = true
    map["C://path"] = true
    val underMyDir1Loc = map.getAllAncestorKeys("C://path/to/my/dir1/loc").toSet()
    assertEquals(setOf("C://path/to/my", "C://path", "C://path/to/my/dir1"), underMyDir1Loc)
    val underMyDir1 = map.getAllAncestorKeys("C://path/to/my/dir1").toSet()
    assertEquals(setOf("C://path/to/my", "C://path", "C://path/to/my/dir1"), underMyDir1)
    val underMy = map.getAllAncestorKeys("C://path/to/my").toSet()
    assertEquals(setOf("C://path/to/my", "C://path"), underMy)
    val underMyDir = map.getAllAncestorKeys("C://path/to/my/").toSet()
    assertEquals(setOf("C://path/to/my", "C://path"), underMyDir)
    val underTo = map.getAllAncestorKeys("C://path/to").toSet()
    assertEquals(setOf("C://path"), underTo)
    val underToDir = map.getAllAncestorKeys("C://path/to/").toSet()
    assertEquals(setOf("C://path"), underToDir)
    val underPath = map.getAllAncestorKeys("C://path").toSet()
    assertEquals(setOf("C://path"), underPath)
    val underProto = map.getAllAncestorKeys("C:/").toSet()
    assertEquals(setOf<String>(), underProto)
  }
}