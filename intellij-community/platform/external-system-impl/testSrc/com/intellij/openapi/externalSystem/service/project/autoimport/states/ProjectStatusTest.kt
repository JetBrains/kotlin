// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.service.project.autoimport.states

import com.intellij.openapi.externalSystem.service.project.autoimport.ProjectStatus
import com.intellij.openapi.externalSystem.service.project.autoimport.ProjectStatus.ProjectState.*
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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
}