// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing.snapshot;

import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.util.ShutDownTracker;
import com.intellij.openapi.vfs.newvfs.persistent.ContentHashesUtil;
import com.intellij.openapi.vfs.newvfs.persistent.FlushingDaemon;
import com.intellij.util.indexing.IndexInfrastructure;
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
public class ContentHashesSupport {
  private static volatile ContentHashesUtil.HashEnumerator ourHashesWithFileType;

  public static void initContentHashesEnumerator() throws IOException {
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

  public static void flushContentHashes() {
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

  @NotNull
  public static byte[] calculateHash(@NotNull byte[] currentBytes,
                                     @NotNull Charset charset,
                                     @NotNull FileType fileType,
                                     @NotNull FileType substituteFileType) {
    return fileType.isBinary() ?
           calcContentHash(currentBytes, substituteFileType) :
           calcContentHashWithFileType(currentBytes, charset, substituteFileType);
  }
}