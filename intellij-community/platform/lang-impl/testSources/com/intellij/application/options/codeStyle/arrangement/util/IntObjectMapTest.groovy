/*
 * Copyright 2000-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.application.options.codeStyle.arrangement.util

import org.junit.Test

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertNull

/**
 * @author Denis Zhdanov
 */
class IntObjectMapTest {
  
  @Test
  void "set and get"() {
    def map = new IntObjectMap<Integer>(4)
    map.set(0, 10)
    map.set(1, 11)
    assertEquals(10, map.get(0))
    assertEquals(11, map.get(1))
    assertEquals(2, map.size())
  }

  @Test
  void "set and get with expand"() {
    def map = new IntObjectMap<Integer>(4)
    map.set(5, 10)
    assertEquals(10, map.get(5))
    assertEquals(1, map.size())
  }

  @Test
  void "expand and enough space"() {
    def map = new IntObjectMap<Integer>(4)
    map.set(1, 10)
    map.shiftKeys(0, 2)
    assertNull(map.get(1))
    assertEquals(10, map.get(3))
    assertEquals(1, map.size())
  }

  @Test
  void "expand and not enough space"() {
    def map = new IntObjectMap<Integer>(4)
    map.set(3, 10)
    map.shiftKeys(3, 2)
    assertNull(map.get(3))
    assertEquals(10, map.get(5))
    assertEquals(1, map.size())
  }

  @Test
  void "expand on first insert"() {
    def map = new IntObjectMap<Integer>(4)
    map.set(4, 10)
    assertEquals(10, map.get(4))
    assertEquals(1, map.size())
  }

  @Test
  void "composite"() {
    def map = new IntObjectMap<Integer>(4)
    map.set(1, 10)
    map.set(2, 20)
    map.set(5, 50)
    map.shiftKeys(2, 10)
    
    assertEquals(10, map.get(1))
    assertNull(map.get(2))
    assertNull(map.get(5))
    assertEquals(20, map.get(12))
    assertEquals(50, map.get(15))
    
    map.remove(15)
    assertNull(map.get(15))
    
    map.shiftKeys(0, 10)
    assertNull(map.get(1))
    assertEquals(10, map.get(11))
    assertNull(map.get(15))
    assertEquals(20, map.get(22))
    assertEquals(2, map.size())
  }
  
  @Test
  void "shift backwards starting from null"() {
    def map = new IntObjectMap<Integer>(4)
    map.set(1, 10)
    map.set(3, 30)
    map.set(4, 40)
    
    map.shiftKeys(2, -1)
    assertEquals(10, map.get(1))
    assertEquals(30, map.get(2))
    assertEquals(40, map.get(3))
    assertNull(map.get(4))
    assertEquals(3, map.size())
  }
  
  @Test
  void "clear"() {
    def map = new IntObjectMap<Integer>()
    map.set(1, 10)
    map.set(2, 20)
    assertEquals(2, map.size())
    
    map.clear()
    assertEquals(0, map.size())
    assertNull(map.get(1))
    assertNull(map.get(2))
  }

  @Test
  void "duplicate operations and size"() {
    def map = new IntObjectMap<Integer>()
    
    map.set(1, 10)
    assertEquals(1, map.size())
    assertEquals(10, map.get(1))

    map.set(1, 100)
    assertEquals(1, map.size())
    assertEquals(100, map.get(1))
    
    map.remove(1)
    assertEquals(0, map.size())
    assertNull(map.get(1))

    map.remove(1)
    assertEquals(0, map.size())
    assertNull(map.get(1))
  }
}
