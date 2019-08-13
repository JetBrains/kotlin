// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.util

/**
 * [PrefixTreeMap] uses keys, that is specified by the file system paths.
 * @see PrefixTreeMap
 */
interface PathPrefixTreeMap<V> : PrefixTreeMap<String, V> {

  /**
   * @see PrefixTreeMap.keys
   */
  val paths: List<String>

  /**
   * @see PrefixTreeMap.get
   */
  operator fun get(path: String): V?

  /**
   * @see PrefixTreeMap.set
   */
  operator fun set(path: String, value: V): V?

  /**
   * @see PrefixTreeMap.remove
   */
  fun remove(path: String): V?

  /**
   * @see PrefixTreeMap.contains
   */
  fun contains(path: String): Boolean

  /**
   * @see PrefixTreeMap.getAllDescendants
   */
  fun getAllDescendants(path: String): List<V>

  /**
   * @see PrefixTreeMap.getAllAncestorKeys
   */
  fun getAllAncestorKeys(path: String): List<String>
}