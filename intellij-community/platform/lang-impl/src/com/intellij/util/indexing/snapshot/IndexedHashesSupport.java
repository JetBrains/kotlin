// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing.snapshot;

import com.intellij.openapi.util.ShutDownTracker;
import com.intellij.openapi.vfs.newvfs.persistent.FlushingDaemon;
import com.intellij.openapi.vfs.newvfs.persistent.PersistentFSImpl;
import com.intellij.util.ArrayUtilRt;
import com.intellij.util.hash.ContentHashEnumerator;
import com.intellij.util.indexing.FileContent;
import com.intellij.util.indexing.FileContentImpl;
import com.intellij.util.indexing.IndexInfrastructure;
import com.intellij.util.io.DigestUtil;
import com.intellij.util.io.IOUtil;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@ApiStatus.Internal
public class IndexedHashesSupport {

  private static final MessageDigest TEXT_CONTENT_HASH_DIGEST = DigestUtil.sha1();

  private static volatile ContentHashEnumerator ourTextContentHashes;

  public static int getVersion() {
    return 1;
  }

  public static void initContentHashesEnumerator() throws IOException {
    if (ourTextContentHashes != null) return;
    //noinspection SynchronizeOnThis
    synchronized (IndexedHashesSupport.class) {
      if (ourTextContentHashes != null) return;
      final File hashEnumeratorFile = new File(IndexInfrastructure.getPersistentIndexRoot(), "textContentHashes");
      try {
        ContentHashEnumerator hashEnumerator = new ContentHashEnumerator(hashEnumeratorFile.toPath());
        FlushingDaemon.everyFiveSeconds(IndexedHashesSupport::flushContentHashes);
        ShutDownTracker.getInstance().registerShutdownTask(IndexedHashesSupport::flushContentHashes);
        ourTextContentHashes = hashEnumerator;
      }
      catch (IOException ex) {
        IOUtil.deleteAllFilesStartingWith(hashEnumeratorFile);
        throw ex;
      }
    }
  }

  public static void flushContentHashes() {
    if (ourTextContentHashes != null && ourTextContentHashes.isDirty()) ourTextContentHashes.force();
  }

  static int enumerateHash(byte @NotNull [] digest) throws IOException {
    return ourTextContentHashes.enumerate(digest);
  }

  public static byte @NotNull [] getOrInitIndexedHash(@NotNull FileContentImpl content) {
    byte[] hash = content.getHash();
    if (hash == null) {
      hash = calculateIndexedHashForFileContent(content);
      content.setHashes(hash);
    }
    return hash;
  }

  private static byte @NotNull [] calculateIndexedHashForFileContent(@NotNull FileContentImpl content) {
    byte[] contentHash = PersistentFSImpl.getContentHashIfStored(content.getFile());
    if (contentHash == null) {
      contentHash = DigestUtil.calculateContentHash(TEXT_CONTENT_HASH_DIGEST, ((FileContent)content).getContent());
      // todo store content hash in FS
    }

    boolean isBinary = content.getFileTypeWithoutSubstitution().isBinary();
    Charset charset = isBinary ? null : content.getCharset();
    byte[] charsetBytes = charset != null ? charset.name().getBytes(StandardCharsets.UTF_8) : ArrayUtilRt.EMPTY_BYTE_ARRAY;
    return DigestUtil.calculateMergedHash(TEXT_CONTENT_HASH_DIGEST, new byte[][]{contentHash, charsetBytes});
  }
}