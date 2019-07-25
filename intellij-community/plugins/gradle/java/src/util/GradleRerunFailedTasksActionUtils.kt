// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.util

import com.intellij.util.containers.FList


fun containsTasksInScriptParameters(scriptParameters: String?, tasks: List<String>): Boolean {
  return scriptParameters?.contains(tasks.joinToString(" ")) ?: false
}

fun containsSubSequenceInSequence(sequence: List<Any>, subSequence: List<Any>) =
  hasSubSequenceInSequence(
    sequence = FList.createFromReversed(sequence.asReversed()),
    subSequence = FList.createFromReversed(subSequence.asReversed())
  )

tailrec fun hasSubSequenceInSequence(sequence: FList<Any>, subSequence: FList<Any>): Boolean {
  if (sequence.size < subSequence.size) return false
  if (sequence.zip(subSequence).all { it.first == it.second }) return true
  return hasSubSequenceInSequence(sequence.tail, subSequence)
}