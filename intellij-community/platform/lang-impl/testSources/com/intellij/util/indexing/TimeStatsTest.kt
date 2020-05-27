// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing

import com.intellij.testFramework.assertions.Assertions.assertThat
import com.intellij.testFramework.assertions.Assertions.assertThatThrownBy
import com.intellij.util.indexing.diagnostic.TimeStats
import org.junit.Test
import kotlin.random.Random

class TimeStatsTest {
  @Test
  fun `empty bucket`() {
    val bucket = TimeStats()
    assertThat(bucket.isEmpty)
    assertThatThrownBy { bucket.minTime }
    assertThatThrownBy { bucket.maxTime }
    assertThatThrownBy { bucket.meanTime }
  }

  @Test
  fun `one time`() {
    val bucket = TimeStats()
    val time = 42L
    bucket.addTime(time)
    assertThat(!bucket.isEmpty)
    assertThat(bucket.minTime).isEqualTo(time)
    assertThat(bucket.maxTime).isEqualTo(time)
    assertThat(bucket.meanTime).isEqualTo(time.toDouble())
  }

  @Test
  fun `two times`() {
    val bucket = TimeStats()
    val one = 10L
    val two = 20L
    bucket.addTime(one)
    bucket.addTime(two)
    assertThat(!bucket.isEmpty)
    assertThat(bucket.minTime).isEqualTo(one)
    assertThat(bucket.maxTime).isEqualTo(two)
    assertThat(bucket.meanTime).isEqualTo((one + two).toDouble() / 2)
  }

  @Test
  fun `many times`() {
    val numberOfTimes = 1000
    val bucket = TimeStats()
    val times = arrayListOf<Long>()
    repeat(numberOfTimes) {
      val time = Random.nextLong()
      times += time
      bucket.addTime(time)
    }
    assertThat(bucket.minTime).isEqualTo(times.min())
    assertThat(bucket.maxTime).isEqualTo(times.max())
    assertThat(bucket.meanTime).isEqualTo(times.sum().toDouble() / times.size)
  }
}