// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.util.gotoByName;

import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypes;

import java.util.Comparator;

/**
 * A file type comparator. The comparison rules are applied in the following order.
 * <ol>
 * <li>Unknown file type is greatest.</li>
 * <li>Text files are less then binary ones.</li>
 * <li>File type with greater name is greater (case is ignored).</li>
 * </ol>
 */
class FileTypeComparator implements Comparator<FileType> {
  /**
   * an instance of comparator
   */
  static final Comparator<FileType> INSTANCE = new FileTypeComparator();

  /**
   * {@inheritDoc}
   */
  @Override
  public int compare(final FileType o1, final FileType o2) {
    if (o1 == o2) {
      return 0;
    }
    if (o1 == FileTypes.UNKNOWN) {
      return 1;
    }
    if (o2 == FileTypes.UNKNOWN) {
      return -1;
    }
    if (o1.isBinary() && !o2.isBinary()) {
      return 1;
    }
    if (!o1.isBinary() && o2.isBinary()) {
      return -1;
    }
    return o1.getName().compareToIgnoreCase(o2.getName());
  }
}
