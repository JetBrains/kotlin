// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing;

import com.intellij.openapi.util.io.FileUtil;
import com.intellij.util.indexing.zipFs.UncompressedZipFileSystem;
import com.intellij.util.indexing.zipFs.UncompressedZipFileSystemProvider;
import com.intellij.util.io.EnumeratorStringDescriptor;
import com.intellij.util.io.PersistentHashMap;
import com.intellij.util.io.zip.JBZipEntry;
import com.intellij.util.io.zip.JBZipFile;
import junit.framework.TestCase;
import org.junit.Ignore;

import java.io.File;
import java.io.IOException;
import java.util.zip.ZipEntry;

@Ignore
public class PersistentMapPacksTest extends TestCase {

  public void testPersistentHashMapPack() throws IOException {
    File dir = FileUtil.createTempDirectory("persistent-map", "packs");

    EnumeratorStringDescriptor descriptor = EnumeratorStringDescriptor.INSTANCE;

    try (PersistentHashMap<String, String> map1 = new PersistentHashMap<>(dir.toPath().resolve("map1"), descriptor, descriptor);
         PersistentHashMap<String, String> map2 = new PersistentHashMap<>(dir.toPath().resolve("map2"), descriptor, descriptor)) {
      map1.put("XXX", "III");
      map1.put("YYY", "JJJ");

      map2.put("IntelliJ", "IDEA");
      map2.put("IDEA", "IntelliJ");
    }

    File pack = new File(dir, "pack.zip");
    try (JBZipFile file = new JBZipFile(pack)) {
      for (File f : dir.listFiles((__, name) -> name.startsWith("map"))) {
        JBZipEntry entry = file.getOrCreateEntry(f.getName());
        entry.setMethod(ZipEntry.STORED);
        entry.setDataFromFile(f);
      }
    }

    try (UncompressedZipFileSystem fs = new UncompressedZipFileSystem(pack.toPath(), new UncompressedZipFileSystemProvider())) {
      try (PersistentHashMap<String, String> map1 = new PersistentHashMap<String, String>(fs.getPath("map1"), descriptor, descriptor) {
        @Override
        protected boolean isReadOnly() {
          return true;
        }
      };
           PersistentHashMap<String, String> map2 = new PersistentHashMap<String, String>(fs.getPath("map2"), descriptor, descriptor) {
             @Override
             protected boolean isReadOnly() {
               return true;
             }
           }) {
        assertEquals("III", map1.get("XXX"));
        assertEquals("JJJ", map1.get("YYY"));

        assertEquals("IDEA", map2.get("IntelliJ"));
        assertEquals("IntelliJ", map2.get("IDEA"));
      }
    }
  }
}
