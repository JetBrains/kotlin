// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.compiler.instrumentation;

import org.jetbrains.org.objectweb.asm.ClassReader;
import org.jetbrains.org.objectweb.asm.ClassWriter;
import org.jetbrains.org.objectweb.asm.Opcodes;

/**
* @author Eugene Zhuravlev
*/
public class InstrumenterClassWriter extends ClassWriter {
  private final InstrumentationClassFinder myFinder;

  public InstrumenterClassWriter(ClassReader reader, int flags, InstrumentationClassFinder finder) {
    super(reader, flags);
    myFinder = finder;
  }

  public InstrumenterClassWriter(int flags, InstrumentationClassFinder finder) {
    super(flags);
    myFinder = finder;
  }

  @Override
  protected String getCommonSuperClass(String type1, String type2) {
    try {
      InstrumentationClassFinder.PseudoClass cls1 = myFinder.loadClass(type1);
      InstrumentationClassFinder.PseudoClass cls2 = myFinder.loadClass(type2);

      if (cls1.isAssignableFrom(cls2)) {
        return cls1.getName();
      }
      if (cls2.isAssignableFrom(cls1)) {
        return cls2.getName();
      }
      if (cls1.isInterface() || cls2.isInterface()) {
        return "java/lang/Object";
      }

      InstrumentationClassFinder.PseudoClass c = cls1;
      do {
        c = c.getSuperClass();
      }
      while (!c.isAssignableFrom(cls2));
      return c.getName();
    }
    catch (Exception e) {
      throw new RuntimeException(e.toString(), e);
    }
  }

  /**
   * Returns class file version in the {@code minor << 16 | major} format.<br/>
   * <b>Warning</b>: in classes compiled with <a href="https://openjdk.java.net/jeps/12">JEP 12's</a> {@code --enable-preview} option
   * the minor version is {@code 0xFFFF}, making the whole version negative.
   */
  public static int getClassFileVersion(ClassReader reader) {
    return reader.readInt(4);
  }

  /**
   * Returns version-specific {@link ClassWriter#ClassWriter(int)} flags.
   */
  public static int getAsmClassWriterFlags(int classFileVersion) {
    return (classFileVersion & 0xFFFF) >= Opcodes.V1_6 ? ClassWriter.COMPUTE_FRAMES : ClassWriter.COMPUTE_MAXS;
  }
}