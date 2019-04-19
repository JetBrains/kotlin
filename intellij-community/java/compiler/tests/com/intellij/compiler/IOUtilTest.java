/*
 * Copyright (c) 2000-2004 by JetBrains s.r.o. All Rights Reserved.
 * Use is subject to license terms.
 */
package com.intellij.compiler;

import com.intellij.util.io.IOUtil;
import junit.framework.TestCase;

import java.io.*;

/**
 * @author Eugene Zhuravlev
 */
public class IOUtilTest extends TestCase {

  private interface IO {
    void save(String str, DataOutput out) throws IOException;
    String load(DataInput in) throws IOException;
  }

  private static final IO COMPILER_IO_UTIL_SAVE = new IO() {
    @Override
    public void save(String str, DataOutput out) throws IOException {
      CompilerIOUtil.writeString(str, out);
    }
    @Override
    public String load(DataInput in) throws IOException {
      return CompilerIOUtil.readString(in);
    }
  };

  private static final IO IO_UTIL_SAVE = new IO() {
    @Override
    public void save(String str, DataOutput out) throws IOException {
      IOUtil.writeString(str, out);
    }
    @Override
    public String load(DataInput in) throws IOException {
      return IOUtil.readString(in);
    }
  };


  public void testReadWriteCompilerIOUtil() throws IOException {
    final String strL = Character.toString('\u0141'); 
    assertEquals(strL, load(save(strL, COMPILER_IO_UTIL_SAVE), COMPILER_IO_UTIL_SAVE));

    final String strA = Character.toString('\u00C4');
    assertEquals(strA, load(save(strA, COMPILER_IO_UTIL_SAVE), COMPILER_IO_UTIL_SAVE));
  }

  public void testReadWriteIOUtil() throws IOException {
    final String strL = Character.toString('\u0141');
    assertEquals(strL, load(save(strL, IO_UTIL_SAVE), IO_UTIL_SAVE));

    final String strA = Character.toString('\u00C4');
    assertEquals(strA, load(save(strA, IO_UTIL_SAVE), IO_UTIL_SAVE));
  }


  private static byte[] save(String str, IO io) throws IOException {

    try (final ByteArrayOutputStream os = new ByteArrayOutputStream(); DataOutputStream out = new DataOutputStream(os)) {
      io.save(str, out);
      return os.toByteArray();
    }
  }

  private static String load(byte[] bytes, IO io) throws IOException {
    try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes))) {
      return io.load(in);
    }

  }
}
