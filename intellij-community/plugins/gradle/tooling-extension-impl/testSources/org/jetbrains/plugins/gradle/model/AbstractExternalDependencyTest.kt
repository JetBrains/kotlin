// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.model

import org.jetbrains.plugins.gradle.DefaultExternalDependencyId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class AbstractExternalDependencyTest {

  @Test
  fun testEquals() {
    assertEquals("a:a:1".lib().depends("b:b:1".lib(), "c:c:1".lib()),
                 "a:a:1".lib().depends("b:b:1".lib(), "c:c:1".lib()))

    assertEquals("a:a:1".lib().depends("b:b:1".lib().depends("c:c:1".lib())),
                 "a:a:1".lib().depends("b:b:1".lib().depends("c:c:1".lib())))

    val depA = "a:a:1".lib().depends("b:b:1".lib().depends("c:c:1".lib()))
    val depA1 = "a:a:1".lib().depends("b:b:1".lib(), "c:c:1".lib())
    assertNotEquals(depA, depA1)
    assertNotEquals(depA1, depA)

    val depB = "b:b:1".lib()
    val depC = "c:c:1".lib()
    val depD = "d:d:1".lib()
    depB.depends(depC)
    depC.depends(depD)
    depD.depends(depB)
    assertEquals("a:a:1".lib().depends(depB, depD, depC),
                 "a:a:1".lib().depends(depB, depD, depC))
  }
}

private fun String.lib(): AbstractExternalDependency {
  val split = split(":")
  return MyAbstractExternalDependency(split[0], split[1], split[2])
}

private class MyAbstractExternalDependency(group: String, name: String, version: String) :
  AbstractExternalDependency(DefaultExternalDependencyId(group, name, version), "", null)

private fun AbstractExternalDependency.depends(vararg dependencies: AbstractExternalDependency): AbstractExternalDependency {
  this.dependencies.addAll(dependencies)
  return this
}

