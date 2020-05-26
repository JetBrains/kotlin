// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing

import com.intellij.testFramework.assertions.Assertions.assertThat
import com.intellij.testFramework.assertions.Assertions.assertThatThrownBy
import com.intellij.util.indexing.diagnostic.MaxNTimeBucket
import org.junit.Test
import kotlin.random.Random

class MaxNTimeBucketTest {
  @Test
  fun `empty bucket`() {
    val bucket = MaxNTimeBucket(1)
    assertThat(bucket.isEmpty)
    assertThatThrownBy { bucket.minTime }
    assertThatThrownBy { bucket.maxTime }
    assertThatThrownBy { bucket.maxNTimes }
    assertThatThrownBy { bucket.meanTime }
  }

  @Test
  fun `one time`() {
    val bucket = MaxNTimeBucket(1)
    val time = 42L
    bucket.addTime(time)
    assertThat(!bucket.isEmpty)
    assertThat(bucket.minTime).isEqualTo(time)
    assertThat(bucket.maxTime).isEqualTo(time)
    assertThat(bucket.meanTime).isEqualTo(time.toDouble())
    assertThat(bucket.maxNTimes).containsExactly(time)
  }

  @Test
  fun `two times`() {
    val bucket = MaxNTimeBucket(2)
    val one = 10L
    val two = 20L
    bucket.addTime(one)
    bucket.addTime(two)
    assertThat(!bucket.isEmpty)
    assertThat(bucket.minTime).isEqualTo(one)
    assertThat(bucket.maxTime).isEqualTo(two)
    assertThat(bucket.meanTime).isEqualTo((one + two).toDouble() / 2)
    assertThat(bucket.maxNTimes).containsExactly(one, two)
  }

  @Test
  fun limit() {
    val numberOfTimes = 1000
    val bucketSize = 10
    val bucket = MaxNTimeBucket(bucketSize)
    val times = arrayListOf<Long>()
    repeat(numberOfTimes) {
      val time = Random.nextLong()
      times += time
      bucket.addTime(time)
    }
    assertThat(bucket.minTime).isEqualTo(times.min())
    assertThat(bucket.maxTime).isEqualTo(times.max())
    assertThat(bucket.meanTime).isEqualTo(times.sum().toDouble() / times.size)
    assertThat(bucket.maxNTimes).containsExactlyInAnyOrderElementsOf(times.sortedDescending().take(bucketSize))
  }
}