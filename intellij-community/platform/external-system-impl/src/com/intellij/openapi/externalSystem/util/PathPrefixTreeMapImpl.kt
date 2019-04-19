// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.util

import com.intellij.openapi.util.io.FileUtil

class PathPrefixTreeMapImpl<V>(
  private val pathSeparator: String = "/",
  private val removeLastSlash: Boolean = true
) : PathPrefixTreeMap<V>, PrefixTreeMapImpl<String, V>(FileUtil.PATH_HASHING_STRATEGY) {

  override val paths: List<String>
    get() = keys.map { it.joinToString(pathSeparator) }

  override operator fun get(path: String): V? = get(path.toPrefixList())

  override operator fun set(path: String, value: V): V? = set(path.toPrefixList(), value)

  override fun remove(path: String): V? = remove(path.toPrefixList())

  override fun contains(path: String): Boolean = contains(path.toPrefixList())

  override fun getAllDescendants(path: String): List<V> = getAllDescendants(path.toPrefixList())

  override fun getAllAncestorKeys(path: String): List<String> = getAllAncestorKeys(path.toPrefixList())
    .map { it.joinToString(pathSeparator) }

  private fun String.toPrefixList(): List<String> {
    val path = if (removeLastSlash) removeSuffix(pathSeparator) else this
    return path.split(pathSeparator)
  }
}