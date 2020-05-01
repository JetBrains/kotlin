// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.autoimport

import com.intellij.openapi.externalSystem.autoimport.ProjectStatus.ModificationType.EXTERNAL
import com.intellij.openapi.externalSystem.autoimport.ProjectStatus.ModificationType.INTERNAL
import com.intellij.openapi.externalSystem.autoimport.ProjectStatus.ProjectState.*
import org.junit.Assert.*
import org.junit.Test

class ProjectStatusTest {
  @Test
  fun `test open project with broken state`() {
    val status = ProjectStatus()
    status.markDirty(10) as Dirty
    status.markModified(20) as Dirty
    assertFalse(status.isUpToDate())
    status.markSynchronized(30) as Synchronized
    assertTrue(status.isUpToDate())
  }

  @Test
  fun `test generate project with broken state`() {
    val status = ProjectStatus()
    status.markModified(10) as Modified
    status.markDirty(20) as Dirty
    assertFalse(status.isUpToDate())
    status.markSynchronized(30) as Synchronized
    assertTrue(status.isUpToDate())
  }

  @Test
  fun `test generate project with lag and broken state`() {
    val status = ProjectStatus()
    status.markDirty(10) as Dirty
    assertFalse(status.isUpToDate())
    status.markSynchronized(30) as Synchronized
    assertTrue(status.isUpToDate())
    status.markModified(20) as Synchronized
    assertTrue(status.isUpToDate())
  }

  @Test
  fun `test delayed modification event`() {
    val status = ProjectStatus()
    status.markSynchronized(20) as Synchronized
    status.markModified(10) as Synchronized
    assertTrue(status.isUpToDate())
  }

  @Test
  fun `test delayed invalidation event`() {
    val status = ProjectStatus()
    status.markSynchronized(20) as Synchronized
    status.markDirty(10) as Synchronized
    assertTrue(status.isUpToDate())
  }

  @Test
  fun `test common sample`() {
    val status = ProjectStatus()
    status.markModified(10) as Modified
    status.markModified(20) as Modified
    status.markModified(30) as Modified
    assertFalse(status.isUpToDate())
    status.markSynchronized(40) as Synchronized
    assertTrue(status.isUpToDate())
  }

  @Test
  fun `test revert changes`() {
    val status = ProjectStatus()
    status.markModified(10) as Modified
    status.markReverted(20) as Reverted
    assertTrue(status.isUpToDate())
    status.markSynchronized(30) as Synchronized
    assertTrue(status.isUpToDate())
  }

  @Test
  fun `test revert dirty changes`() {
    val status = ProjectStatus()
    status.markModified(10) as Modified
    status.markDirty(20) as Dirty
    status.markReverted(30) as Dirty
    assertFalse(status.isUpToDate())
    status.markSynchronized(40) as Synchronized
    assertTrue(status.isUpToDate())
  }

  @Test
  fun `test modification after revert event`() {
    val status = ProjectStatus()
    status.markModified(10) as Modified
    status.markReverted(20) as Reverted
    status.markModified(30) as Modified
    assertFalse(status.isUpToDate())
    status.markSynchronized(40) as Synchronized
    assertTrue(status.isUpToDate())
  }

  @Test
  fun `test modification types`() {
    val status = ProjectStatus()

    status.markModified(10, INTERNAL) as Modified
    assertEquals(INTERNAL, status.getModificationType())
    status.markModified(20, EXTERNAL) as Modified
    assertEquals(INTERNAL, status.getModificationType())

    status.markSynchronized(30) as Synchronized
    assertEquals(null, status.getModificationType())
    status.markModified(40, EXTERNAL) as Modified
    assertEquals(EXTERNAL, status.getModificationType())
    status.markModified(50, INTERNAL) as Modified
    assertEquals(INTERNAL, status.getModificationType())

    status.markSynchronized(60) as Synchronized
    assertEquals(null, status.getModificationType())
    status.markDirty(70, INTERNAL) as Dirty
    assertEquals(INTERNAL, status.getModificationType())
    status.markModified(80, EXTERNAL) as Dirty
    assertEquals(INTERNAL, status.getModificationType())
    status.markModified(90, INTERNAL) as Dirty
    assertEquals(INTERNAL, status.getModificationType())

    status.markSynchronized(100) as Synchronized
    assertEquals(null, status.getModificationType())
    status.markDirty(110, EXTERNAL) as Dirty
    assertEquals(EXTERNAL, status.getModificationType())
    status.markModified(120, INTERNAL) as Dirty
    assertEquals(INTERNAL, status.getModificationType())
  }

  @Test
  fun `test tracking status after failed import`() {
    val status = ProjectStatus()

    status.markModified(10, INTERNAL) as Modified
    status.markSynchronized(20) as Synchronized
    status.markBroken(30) as Broken

    assertFalse(status.isUpToDate())
    assertFalse(status.isDirty())

    status.markModified(40) as Dirty
    status.markReverted(50) as Dirty
    status.markSynchronized(60) as Synchronized
    status.markBroken(70) as Broken

    assertFalse(status.isUpToDate())
    assertFalse(status.isDirty())

    status.markReverted(80) as Broken
    status.markSynchronized(90) as Synchronized
    status.markModified(110) as Modified
    status.markBroken(100) as Dirty

    assertFalse(status.isUpToDate())
    assertTrue(status.isDirty())
  }
}