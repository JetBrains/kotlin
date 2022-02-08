/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "polyhash/PolyHash.h"
#include "polyhash/naive.h"

#include "gmock/gmock.h"
#include "gtest/gtest.h"

namespace {

TEST(PolyHashTest, Correctness) {
  const int maxLength = 10000;
  uint16_t str[maxLength + 100];
  for (int k = 1; k <= maxLength; ++k) {
    for (int i = 0; i < k; ++i)
      str[i] = k * maxLength + i;
    str[k] = 0;

    for (int shift = 0; shift < 8 && k - shift > 0; ++shift)
      EXPECT_EQ(polyHash_naive(k - shift, str + shift), polyHash(k - shift, str + shift));
  }
}

}