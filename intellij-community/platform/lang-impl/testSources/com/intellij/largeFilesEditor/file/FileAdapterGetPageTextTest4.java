// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.largeFilesEditor.file;

import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.CharsetToolkit;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.PlatformTestCase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;

import static org.junit.Assert.*;

@RunWith(Parameterized.class)
public class FileAdapterGetPageTextTest4 {

  @Test
  public void testGetPageText() throws IOException {
    File tempFile = null;
    try {
      tempFile = FileUtil.createTempFile("test", ".txt");
      byte[] bom = writeBOM ? CharsetToolkit.getPossibleBom(charset) : null;
      PlatformTestCase.setContentOnDisk(tempFile, bom, fileText, charset);
      VirtualFile virtualFile = new MockVirtualFile(tempFile);
      assertNotNull(virtualFile);

      virtualFile.setCharset(charset);
      FileAdapter fileAdapter = new FileAdapter(pageSize, maxBorderShift, virtualFile);
      if (!writeBOM) {
        fileAdapter.setCharset(charset);
      }

      assertEquals(expectedPages.length, fileAdapter.getPagesAmount());
      for (int i = 0; i < expectedPages.length; i++) {
        assertEquals("page[" + i + "]", expectedPages[i], fileAdapter.getPageText(i));
      }

      fileAdapter.closeFile();
    }
    finally {
      if (tempFile != null) {
        FileUtil.delete(tempFile);
        assertFalse(tempFile.exists());
      }
    }
  }

  @Parameter
  public String fileText;
  @Parameter(1)
  public Charset charset;
  @Parameter(2)
  public boolean writeBOM;
  @Parameter(3)
  public int pageSize;
  @Parameter(4)
  public int maxBorderShift;
  @Parameter(5)
  public String[] expectedPages;

  @Parameters(name = " {index}: fileText=\"{0}\",charset={1},writeBOM={2},pageSize={3},maxBorderShiftSize={4},expectedPagesTexts={5}")
  public static Iterable<Object[]> data() {
    return Arrays.asList(new Object[][]{
      { // case number 0
        "str1\nstr2\n",
        CharsetToolkit.UTF8_CHARSET, false,
        5, 4,
        new String[]{
          "str1\n", "str2\n"}
      },
      { // case number 1
        "str1\nstr2\n",
        CharsetToolkit.UTF8_CHARSET, false,
        3, 0,
        new String[]{
          "str", "1\ns", "tr2", "\n"}
      },
      { // case number 2
        "str1\nstr2\n",
        CharsetToolkit.UTF8_CHARSET, false,
        10, 0,
        new String[]{
          "str1\nstr2\n"}
      },
      { // case number 3
        "str1\nstr2\n",
        CharsetToolkit.UTF8_CHARSET, false,
        9, 4,
        new String[]{
          "str1\nstr2\n", ""}
      },
      { // case number 4
        "ыыы1\nstr2\n",
        CharsetToolkit.UTF8_CHARSET, false,
        3, 0,
        new String[]{
          "ыы", "ы", "1\ns", "tr2", "\n"}
      },
      { // case number 5
        "ыыы1\nstr2\n",
        CharsetToolkit.UTF8_CHARSET, false,
        3, 0,
        new String[]{
          "ыы", "ы", "1\ns", "tr2", "\n"}
      },
      { // case number 6
        "str1\nstr2\n",
        CharsetToolkit.UTF_32BE_CHARSET, false,
        16, 0,
        new String[]{
          "str1", "\nstr", "2\n"}
      },
      { // case number 7
        "str1\nstr2\n",
        CharsetToolkit.UTF_32BE_CHARSET, true,
        16, 0,
        new String[]{
          "str", "1\nst", "r2\n"}
      },
      { // case number 8
        "ыыы1\nstr2\n",
        CharsetToolkit.UTF_32LE_CHARSET, true,
        16, 0,
        new String[]{
          "ыыы", "1\nst", "r2\n"}
      },
      { // case number 9
        "ыыы1\nstr2\n",
        CharsetToolkit.UTF_16LE_CHARSET, false,
        8, 0,
        new String[]{
          "ыыы1", "\nstr", "2\n"}
      },
      { // case number 10
        "ыыы1\nstr2\n",
        CharsetToolkit.UTF_16BE_CHARSET, false,
        8, 0,
        new String[]{
          "ыыы1", "\nstr", "2\n"}
      },
      { // case number 11
        "\uD840\uDC00" + "\uD840\uDC00" + "\uD840\uDC00",
        CharsetToolkit.UTF_16BE_CHARSET, false,
        5, 0,
        new String[]{
          "\uD840\uDC00" + "\uD840\uDC00", "\uD840\uDC00", ""}
      },
      { // case number 12
        "\uD840\uDC00" + "\uD840\uDC00" + "\uD840\uDC00",
        CharsetToolkit.UTF_16BE_CHARSET, false,
        6, 0,
        new String[]{
          "\uD840\uDC00" + "\uD840\uDC00", "\uD840\uDC00"}
      },
      { // case number 13
        "\uD840\uDC00" + "\uD840\uDC00" + "\uD840\uDC00",
        CharsetToolkit.UTF_16BE_CHARSET, false,
        7, 0,
        new String[]{
          "\uD840\uDC00" + "\uD840\uDC00", "\uD840\uDC00"}
      },
      { // case number 14
        "\uD840\uDC00" + "\uD840\uDC00" + "\uD840\uDC00" + "1\nstr2\n",
        CharsetToolkit.UTF_16BE_CHARSET, false,
        8, 0,
        new String[]{
          "\uD840\uDC00" + "\uD840\uDC00", "\uD840\uDC00" + "1\n", "str2", "\n"}
      },
      { // case number 15
        "\uD840\uDC00" + "\uD840\uDC00" + "\uD840\uDC00",
        CharsetToolkit.UTF_16BE_CHARSET, false,
        2, 0,
        new String[]{
          "\uD840\uDC00", "", "\uD840\uDC00", "", "\uD840\uDC00", ""}
      },
      { // case number 16
        "\uD840\uDC00" + "\uD840\uDC00" + "\uD840\uDC00",
        CharsetToolkit.UTF_16LE_CHARSET, false,
        5, 0,
        new String[]{
          "\uD840\uDC00" + "\uD840\uDC00", "\uD840\uDC00", ""}
      },
      { // case number 17
        "\uD840\uDC00" + "\uD840\uDC00" + "\uD840\uDC00",
        CharsetToolkit.UTF_16LE_CHARSET, false,
        6, 0,
        new String[]{
          "\uD840\uDC00" + "\uD840\uDC00", "\uD840\uDC00"}
      },
      { // case number 18
        "\uD840\uDC00" + "\uD840\uDC00" + "\uD840\uDC00",
        CharsetToolkit.UTF_16LE_CHARSET, false,
        7, 0,
        new String[]{
          "\uD840\uDC00" + "\uD840\uDC00", "\uD840\uDC00"}
      },
      { // case number 19
        "\uD840\uDC00" + "\uD840\uDC00" + "\uD840\uDC00" + "1\nstr2\n",
        CharsetToolkit.UTF_16LE_CHARSET, false,
        8, 0,
        new String[]{
          "\uD840\uDC00" + "\uD840\uDC00", "\uD840\uDC00" + "1\n", "str2", "\n"}
      },
      { // case number 20
        "ыыы1\nstr2\n",
        CharsetToolkit.UTF_16BE_CHARSET, false,
        8, 0,
        new String[]{
          "ыыы1", "\nstr", "2\n"}
      },
      { // case number 21
        "ыыы1\nstr2\n",
        CharsetToolkit.UTF_16BE_CHARSET, false,
        8, 6,
        new String[]{
          "ыыы1\n", "str2\n", ""}
      },
      { // case number 22
        "ыыы1\rstr2\r",
        CharsetToolkit.UTF_16BE_CHARSET, false,
        8, 6,
        new String[]{
          "ыыы1\r", "str2\r", ""}
      },
      { // case number 23
        "ыыы1\r\nstr2\r\n",
        CharsetToolkit.UTF_16BE_CHARSET, false,
        8, 6,
        new String[]{
          "ыыы1\r\n", "str2\r", "\n"}
      },
    });
  }
}