// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.nativeplatform.tooling.model

import java.util.*
import kotlin.collections.LinkedHashSet

class DeterminedEntityGenerator {
  private val alphaNum = ('a'..'z') + ('A'..'Z') + ('0'..'9')
  private val strings = LinkedHashSet<String>()

  private val random = Random(0)

  fun nextInt(lower: Int, upper: Int) =
    random.nextInt(upper - lower) + lower

  fun nextBoolean() = random.nextBoolean()

  fun nextString(length: Int) = (1..length)
    .map { random.nextInt(alphaNum.size) }
    .map(alphaNum::get)
    .joinToString("")

  tailrec fun nextUString(minLength: Int): String {
    repeat(100) {
      val string = nextString(minLength)
      if (!strings.contains(string)) {
        strings.add(string)
        return string
      }
    }
    return nextUString(minLength + 1)
  }
}