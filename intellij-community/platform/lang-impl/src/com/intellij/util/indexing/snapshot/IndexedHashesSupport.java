// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing.snapshot;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.util.ShutDownTracker;
import com.intellij.openapi.vfs.newvfs.persistent.FlushingDaemon;
import com.intellij.openapi.vfs.newvfs.persistent.PersistentFS;
import com.intellij.openapi.vfs.newvfs.persistent.PersistentFSImpl;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.util.ArrayUtilRt;
import com.intellij.util.hash.ContentHashEnumerator;
import com.intellij.util.indexing.FileContent;
import com.intellij.util.indexing.FileContentImpl;
import com.intellij.util.indexing.IndexInfrastructure;
import com.intellij.util.io.DigestUtil;
import com.intellij.util.io.IOUtil;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@ApiStatus.Internal
public class IndexedHashesSupport {

  private static final Logger LOG = Logger.getInstance(IndexedHashesSupport.class);

  private static final MessageDigest CONTENT_HASH_WITH_FILE_TYPE_DIGEST = DigestUtil.sha1();

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

  public static void initIndexedHash(@NotNull FileContentImpl content) {
    boolean binary = content.getFileTypeWithoutSubstitution().isBinary();

    byte[] fileContentHash = calculateIndexedHashForFileContent(content, binary);
    byte[] documentHash = binary ? null : calculateIndexedHashForDocument(content);

    content.setHashes(fileContentHash, documentHash != null ? documentHash : fileContentHash);
  }

  public static byte @NotNull [] getOrInitIndexedHash(@NotNull FileContentImpl content, boolean fromDocument) {
    byte[] hash = content.getHash(fromDocument);
    if (hash == null) {
      initIndexedHash(content);
      hash = content.getHash(fromDocument);
      LOG.assertTrue(hash != null);
    }
    return hash;
  }

  private static byte @NotNull [] calculateIndexedHashForFileContent(@NotNull FileContentImpl content, boolean binary) {
    byte[] contentHash = null;
    if (content.isPhysicalContent()) {
      contentHash = ((PersistentFSImpl)PersistentFS.getInstance()).getContentHashIfStored(content.getFile());
    }

    if (contentHash == null) {
      contentHash = calculateContentHash(content);
      // todo store content hash in FS
    }

    return mergeIndexedHash(contentHash, binary ? null : content.getCharset());
  }

  private static byte[] calculateContentHash(@NotNull FileContent content) {
    return DigestUtil.calculateContentHash(CONTENT_HASH_WITH_FILE_TYPE_DIGEST, content.getContent());
  }

  private static byte @Nullable [] calculateIndexedHashForDocument(@NotNull FileContentImpl content) {
    Document document = FileDocumentManager.getInstance().getCachedDocument(content.getFile());
    if (document != null) {  // if document is not committed
      PsiDocumentManager psiDocumentManager = PsiDocumentManager.getInstance(content.getProject());

      if (psiDocumentManager.isUncommited(document)) {
        PsiFile file = psiDocumentManager.getCachedPsiFile(document);

        if (file != null) {
          Charset charset = content.getCharset();
          return mergeIndexedHash(DigestUtil.calculateContentHash(CONTENT_HASH_WITH_FILE_TYPE_DIGEST, file.getText().getBytes(charset)), charset);
        }
      }
    }
    return null;
  }

  private static byte @NotNull [] mergeIndexedHash(byte @NotNull [] binaryContentHash,
                                                   @Nullable Charset charsetOrNullForBinary) {
    byte[] charsetBytes = charsetOrNullForBinary != null
                          ? charsetOrNullForBinary.name().getBytes(StandardCharsets.UTF_8)
                          : ArrayUtilRt.EMPTY_BYTE_ARRAY;
    return DigestUtil.calculateMergedHash(CONTENT_HASH_WITH_FILE_TYPE_DIGEST, new byte[][]{binaryContentHash, charsetBytes});
  }
}