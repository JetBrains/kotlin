// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.largeFilesEditor;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class UtilsTest {

  @Test
  public void calculateProgressValue_firstOf10() {
    assertEquals(0, Utils.calculatePagePositionPercent(0, 10));
  }

  @Test
  public void calculateProgressValue_lastOf10() {
    assertEquals(100, Utils.calculatePagePositionPercent(9, 10));
  }

  @Test
  public void calculateProgressValue_firstOf1000() {
    assertEquals(0, Utils.calculatePagePositionPercent(0, 1000));
  }

  @Test
  public void calculateProgressValue_lastOf1000() {
    assertEquals(100, Utils.calculatePagePositionPercent(999, 1000));
  }

  @Test
  public void calculateProgressValue_penultimateOf1000() {
    int result = Utils.calculatePagePositionPercent(998, 1000);
    assertTrue(result == 99 || result == 100);
  }

  @Test
  public void calculateProgressValue_secondOf1000() {
    int result = Utils.calculatePagePositionPercent(1, 1000);
    assertTrue(result == 0 || result == 1);
  }

  @Test
  public void calculateProgressValue_midOf1000() {
    int progress = Utils.calculatePagePositionPercent(500, 1000);
    assertTrue("progress=" + progress, progress >= 49 && progress <= 51);
  }

  @Test
  public void cutToMaxLength_longStrings_1() {
    assertEquals("012345678901234567890123456789",
                 Utils.cutToMaxLength("012345678901234567890123456789", 100));
  }

  @Test
  public void cutToMaxLength_longStrings_2() {
    assertEquals("01234567...123456789",
                 Utils.cutToMaxLength("012345678901234567890123456789", 20));
  }

  @Test
  public void cutToMaxLength_longStrings_3() {
    assertEquals("01234567...23456789",
                 Utils.cutToMaxLength("012345678901234567890123456789", 19));
  }

  @Test
  public void cutToMaxLength_shortStrings_1() {
    assertEquals("01234567", Utils.cutToMaxLength("01234567", 100));
  }

  @Test
  public void cutToMaxLength_shortStrings_2() {
    assertEquals("01234567", Utils.cutToMaxLength("01234567", 8));
  }

  @Test
  public void cutToMaxLength_shortStrings_3() {
    assertEquals("01...67", Utils.cutToMaxLength("01234567", 7));
  }

  @Test
  public void cutToMaxLength_shortStrings_4() {
    assertEquals("0...67", Utils.cutToMaxLength("01234567", 6));
  }

  @Test
  public void cutToMaxLength_shortStrings_5() {
    assertEquals("0...7", Utils.cutToMaxLength("01234567", 5));
  }
}