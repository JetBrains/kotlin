// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.util

interface GradleProperties {

  val javaHomeProperty: GradleProperty<String>?

  /**
   * Represents property in the properties file that is descripted by [location]
   */
  data class GradleProperty<T>(
    val value: T,
    val location: String
  )

  object EMPTY : GradleProperties {
    override val javaHomeProperty: Nothing? = null
  }
}