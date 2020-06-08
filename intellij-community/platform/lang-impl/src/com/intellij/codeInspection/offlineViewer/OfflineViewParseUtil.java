// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.codeInspection.offlineViewer;

import com.intellij.codeInspection.InspectionsResultUtil;
import com.intellij.codeInspection.offline.OfflineProblemDescriptor;
import com.intellij.codeInspection.reference.SmartRefElementPointerImpl;
import com.intellij.util.containers.Interner;
import com.thoughtworks.xstream.io.xml.XppReader;
import gnu.trove.THashSet;
import gnu.trove.TObjectIntHashMap;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;
import org.xmlpull.mxp1.MXParser;

import java.io.*;
import java.util.*;

public final class OfflineViewParseUtil {
  @NonNls private static final String PACKAGE = "package";
  @NonNls private static final String DESCRIPTION = "description";
  @NonNls private static final String HINTS = "hints";
  @NonNls private static final String LINE = "line";
  @NonNls private static final String MODULE = "module";

  private OfflineViewParseUtil() {
  }

  public static Map<String, Set<OfflineProblemDescriptor>> parse(File problemFile) throws FileNotFoundException {
    return parse(new FileReader(problemFile));
  }

  /**
   * @deprecated use {@link #parse(File)} or {@link #parse(Reader)}
   */
  @Deprecated
  public static Map<String, Set<OfflineProblemDescriptor>> parse(String problemText) {
    return parse(new StringReader(problemText));
  }

  public static Map<String, Set<OfflineProblemDescriptor>> parse(Reader problemReader) {
    TObjectIntHashMap<String> fqName2IdxMap = new TObjectIntHashMap<>();
    Interner<String> stringInterner = Interner.createStringInterner();
    Map<String, Set<OfflineProblemDescriptor>> package2Result = new HashMap<>();
    XppReader reader = new XppReader(problemReader, new MXParser());
    try {
      while(reader.hasMoreChildren()) {
        reader.moveDown(); //problem
        final OfflineProblemDescriptor descriptor = new OfflineProblemDescriptor();
        boolean added = false;
        while(reader.hasMoreChildren()) {
          reader.moveDown();
          if (SmartRefElementPointerImpl.ENTRY_POINT.equals(reader.getNodeName())) {
            descriptor.setType(reader.getAttribute(SmartRefElementPointerImpl.TYPE_ATTR));
            final String fqName = reader.getAttribute(SmartRefElementPointerImpl.FQNAME_ATTR);
            descriptor.setFQName(fqName);

            if (!fqName2IdxMap.containsKey(fqName)) {
              fqName2IdxMap.put(fqName, 0);
            }
            int idx = fqName2IdxMap.get(fqName);
            descriptor.setProblemIndex(idx);
            fqName2IdxMap.put(fqName, idx + 1);
          }
          if (DESCRIPTION.equals(reader.getNodeName())) {
            descriptor.setDescription(stringInterner.intern(reader.getValue()));
          }
          if (LINE.equals(reader.getNodeName())) {
            descriptor.setLine(Integer.parseInt(reader.getValue()));
          }
          if (MODULE.equals(reader.getNodeName())) {
            descriptor.setModule(stringInterner.intern(reader.getValue()));
          }
          if (HINTS.equals(reader.getNodeName())) {
            while(reader.hasMoreChildren()) {
              reader.moveDown();
              List<String> hints = descriptor.getHints();
              if (hints == null) {
                hints = new ArrayList<>();
                descriptor.setHints(hints);
              }
              hints.add(stringInterner.intern(reader.getAttribute("value")));
              reader.moveUp();
            }
          }
          if (PACKAGE.equals(reader.getNodeName())) {
            appendDescriptor(package2Result, reader.getValue(), descriptor);
            added = true;
          }
          while(reader.hasMoreChildren()) {
            reader.moveDown();
            if (PACKAGE.equals(reader.getNodeName())) {
              appendDescriptor(package2Result, reader.getValue(), descriptor);
              added = true;
            }
            reader.moveUp();
          }
          reader.moveUp();
        }
        if (!added) appendDescriptor(package2Result, null, descriptor);
        reader.moveUp();
      }
    }
    finally {
      reader.close();
    }
    return package2Result;
  }


  @Nullable
  public static String parseProfileName(File descriptorFile) throws FileNotFoundException {
    return parseProfileName(new FileReader(descriptorFile));
  }

  /**
   * @deprecated use {@link #parseProfileName(File)} or {@link #parseProfileName(Reader)}
   */
  @Deprecated
  @Nullable
  public static String parseProfileName(String descriptorText) {
    return parseProfileName(new StringReader(descriptorText));
  }

  @Nullable
  public static String parseProfileName(Reader descriptorReader) {
    final XppReader reader = new XppReader(descriptorReader, new MXParser());
    try {
      return reader.getAttribute(InspectionsResultUtil.PROFILE);
    }
    catch (Exception e) {
      return null;
    }
    finally {
      reader.close();
    }
  }

  private static void appendDescriptor(final Map<String, Set<OfflineProblemDescriptor>> package2Result,
                                       final String packageName,
                                       final OfflineProblemDescriptor descriptor) {
    Set<OfflineProblemDescriptor> descriptors = package2Result.get(packageName);
    if (descriptors == null) {
      descriptors = new THashSet<>();
      package2Result.put(packageName, descriptors);
    }
    descriptors.add(descriptor);
  }
}
