// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.largeFilesEditor.file;

import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Random;

import static org.junit.Assert.*;

@RunWith(Parameterized.class)
public class FileAdapterGetPagesAmountTest4 {
  private static File myTempFile;
  private static VirtualFile myVirtualFile;

  @BeforeClass
  public static void beforeClass() throws IOException {
    Random random = new Random(0);
    int fileSize = 1000;
    byte[] data = new byte[fileSize];
    random.nextBytes(data);

    myTempFile = FileUtil.createTempFile("test.", ".txt");
    try (FileOutputStream stream = new FileOutputStream(myTempFile)) {
      stream.write(data);
    }

    myVirtualFile = new MockVirtualFile(myTempFile);
    assertNotNull(myVirtualFile);
  }

  @AfterClass
  public static void afterClass() {
    FileUtil.delete(myTempFile);
    assertFalse(myTempFile.exists());
  }

  @Parameters(name = " {index}: pageSize={0},amountOfPages={1}")
  public static Iterable<Object[]> data() {
    return Arrays.asList(new Object[][]{
      {1, 1000},
      {2, 500},
      {3, 334},
      {10, 100},
      {50, 20},
      {100, 10},
      {500, 2},
      {501, 2},
      {999, 2},
      {1000, 1},
      {1001, 1},
      {2000, 1},
      {1000000, 1}
    });
  }

  @Parameter
  public int pageSize;
  @Parameter(1)
  public int amountOfPages;

  @Test
  public void testGetPagesAmount() throws IOException {
    int pageShiftSize = 1;
    FileAdapter fileAdapter = null;
    try {
      fileAdapter = new FileAdapter(pageSize, pageShiftSize, myVirtualFile);
      assertEquals(amountOfPages, fileAdapter.getPagesAmount());
    }
    finally {
      if (fileAdapter != null) {
        fileAdapter.closeFile();
      }
    }
  }
}