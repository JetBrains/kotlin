/*
 * Copyright 2000-2014 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.util.indexing;

import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.util.ShutDownTracker;
import com.intellij.openapi.vfs.newvfs.persistent.ContentHashesUtil;
import com.intellij.openapi.vfs.newvfs.persistent.FlushingDaemon;
import com.intellij.util.io.IOUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.security.MessageDigest;

/**
 * @author Maxim.Mossienko
 */
class ContentHashesSupport {
  private static volatile ContentHashesUtil.HashEnumerator ourHashesWithFileType;

  static void initContentHashesEnumerator() throws IOException {
    if (ourHashesWithFileType != null) return;
    synchronized (ContentHashesSupport.class) {
      if (ourHashesWithFileType != null) return;
      final File hashEnumeratorFile = new File(IndexInfrastructure.getPersistentIndexRoot(), "hashesWithFileType");
      try {
        ContentHashesUtil.HashEnumerator hashEnumerator = new ContentHashesUtil.HashEnumerator(hashEnumeratorFile, null);
        FlushingDaemon.everyFiveSeconds(ContentHashesSupport::flushContentHashes);
        ShutDownTracker.getInstance().registerShutdownTask(ContentHashesSupport::flushContentHashes);
        ourHashesWithFileType = hashEnumerator;
      }
      catch (IOException ex) {
        IOUtil.deleteAllFilesStartingWith(hashEnumeratorFile);
        throw ex;
      }
    }
  }

  static void flushContentHashes() {
    if (ourHashesWithFileType != null && ourHashesWithFileType.isDirty()) ourHashesWithFileType.force();
  }

  static byte[] calcContentHash(@NotNull byte[] bytes, @NotNull FileType fileType) {
    MessageDigest messageDigest = ContentHashesUtil.HASHER_CACHE.getValue();

    Charset defaultCharset = Charset.defaultCharset();
    messageDigest.update(fileType.getName().getBytes(defaultCharset));
    messageDigest.update((byte)0);
    messageDigest.update(String.valueOf(bytes.length).getBytes(defaultCharset));
    messageDigest.update((byte)0);
    messageDigest.update(bytes, 0, bytes.length);
    return messageDigest.digest();
  }

  static int calcContentHashIdWithFileType(@NotNull byte[] bytes, @Nullable Charset charset, @NotNull FileType fileType) throws IOException {
    return enumerateHash(calcContentHashWithFileType(bytes, charset, fileType));
  }

  static int calcContentHashId(@NotNull byte[] bytes, @NotNull FileType fileType) throws IOException {
    return enumerateHash(calcContentHash(bytes, fileType));
  }

  static int enumerateHash(@NotNull byte[] digest) throws IOException {
    return ourHashesWithFileType.enumerate(digest);
  }

  static byte[] calcContentHashWithFileType(@NotNull byte[] bytes, @Nullable Charset charset, @NotNull FileType fileType) {
    MessageDigest messageDigest = ContentHashesUtil.HASHER_CACHE.getValue();

    Charset defaultCharset = Charset.defaultCharset();
    messageDigest.update(fileType.getName().getBytes(defaultCharset));
    messageDigest.update((byte)0);
    messageDigest.update(String.valueOf(bytes.length).getBytes(defaultCharset));
    messageDigest.update((byte)0);
    messageDigest.update((charset != null ? charset.name():"null_charset").getBytes(defaultCharset));
    messageDigest.update((byte)0);

    messageDigest.update(bytes, 0, bytes.length);
    return messageDigest.digest();
  }
}