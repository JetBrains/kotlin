// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.diagnostic.logging;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public final class LogConsoleBaseTest {
  @Test
  public void resizeBufferLengthIsLessThanSize() {
    StringBuffer buffer = new StringBuffer("06-05 17:59:30.211 1664-1680/? W/zygote: kill(-7199, 9) failed: No such process\n");

    LogConsoleBase.resizeBuffer(buffer, 128);
    assertEquals("06-05 17:59:30.211 1664-1680/? W/zygote: kill(-7199, 9) failed: No such process\n", buffer.toString());
  }

  @Test
  public void resizeBufferNewlineNotFound() {
    StringBuffer buffer = new StringBuffer("06-05 17:59:30.211 1664-1680/? W/zygote: kill(-7199, 9) failed: No such process");

    LogConsoleBase.resizeBuffer(buffer, 64);
    assertEquals("211 1664-1680/? W/zygote: kill(-7199, 9) failed: No such process", buffer.toString());
  }

  @Test
  public void resizeBufferIndexToDeleteToIsInFirstMessage() {
    StringBuffer buffer = new StringBuffer(
      "06-05 17:59:30.211 1664-1680/? W/zygote: kill(-7199, 9) failed: No such process\n" +
      "06-05 17:59:30.214 6745-6745/? I/zygote: Deoptimizing void com.google.android.finsky.af.a.o.a(com.google.android.finsky.af.d) due " +
      "to JIT inline cache\n");

    LogConsoleBase.resizeBuffer(buffer, 190);

    assertEquals(
      "06-05 17:59:30.214 6745-6745/? I/zygote: Deoptimizing void com.google.android.finsky.af.a.o.a(com.google.android.finsky.af.d) due " +
      "to JIT inline cache\n",
      buffer.toString());
  }

  @Test
  public void resizeBufferIndexToDeleteToIsEqualToIndexOfNewline() {
    StringBuffer buffer = new StringBuffer(
      "06-05 17:59:30.211 1664-1680/? W/zygote: kill(-7199, 9) failed: No such process\n" +
      "06-05 17:59:30.214 6745-6745/? I/zygote: Deoptimizing void com.google.android.finsky.af.a.o.a(com.google.android.finsky.af.d) due " +
      "to JIT inline cache\n");

    LogConsoleBase.resizeBuffer(buffer, 151);

    assertEquals(
      "06-05 17:59:30.214 6745-6745/? I/zygote: Deoptimizing void com.google.android.finsky.af.a.o.a(com.google.android.finsky.af.d) due " +
      "to JIT inline cache\n",
      buffer.toString());
  }
}
