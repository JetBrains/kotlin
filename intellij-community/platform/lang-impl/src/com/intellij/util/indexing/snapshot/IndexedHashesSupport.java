// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing.snapshot;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.util.ShutDownTracker;
import com.intellij.openapi.vfs.newvfs.persistent.*;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.util.indexing.FileContentImpl;
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
public class IndexedHashesSupport {
  private static final Logger LOG = Logger.getInstance("#" + IndexedHashesSupport.class.getPackage().getName());

  private static volatile ContentHashesUtil.HashEnumerator ourHashesWithFileType;

  public static void initContentHashesEnumerator() throws IOException {
    if (ourHashesWithFileType != null) return;
    //noinspection SynchronizeOnThis
    synchronized (IndexedHashesSupport.class) {
      if (ourHashesWithFileType != null) return;
      final File hashEnumeratorFile = new File(IndexInfrastructure.getPersistentIndexRoot(), "hashesWithFileType");
      try {
        ContentHashesUtil.HashEnumerator hashEnumerator = new ContentHashesUtil.HashEnumerator(hashEnumeratorFile.toPath());
        FlushingDaemon.everyFiveSeconds(IndexedHashesSupport::flushContentHashes);
        ShutDownTracker.getInstance().registerShutdownTask(IndexedHashesSupport::flushContentHashes);
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

  static int enumerateHash(@NotNull byte[] digest) throws IOException {
    return ourHashesWithFileType.enumerate(digest);
  }

  public static void initIndexedHash(@NotNull FileContentImpl content) {
    boolean binary = content.getFileTypeWithoutSubstitution().isBinary();

    byte[] fileContentHash = calculateIndexedHashForFileContent(content, binary);
    byte[] documentHash = binary ? null : calculateIndexedHashForDocument(content);

    content.setHashes(fileContentHash, documentHash != null ? documentHash : fileContentHash);
  }

  @NotNull
  public static byte[] getOrInitIndexedHash(@NotNull FileContentImpl content, boolean fromDocument) {
    byte[] hash = content.getHash(fromDocument);
    if (hash == null) {
      initIndexedHash(content);
      hash = content.getHash(fromDocument);
      LOG.assertTrue(hash != null);
    }
    return hash;
  }

  @NotNull
  private static byte[] calculateIndexedHashForFileContent(@NotNull FileContentImpl content, boolean binary) {
    byte[] contentHash = null;
    if (content.isPhysicalContent()) {
      contentHash = ((PersistentFSImpl)PersistentFS.getInstance()).getContentHashIfStored(content.getFile());
    }

    if (contentHash == null) {
      byte[] bytes = content.getContent();
      contentHash = calculateFileContentHash(bytes);
      // todo store content hash in FS
    }

    return mergeIndexedHash(contentHash, binary ? null : content.getCharset(), content.getFileType());
  }

  private static byte[] calculateFileContentHash(byte[] bytes) {
    return FSRecords.calculateContentHash(ContentHashesUtil.HASHER_CACHE.getValue(), bytes, 0, bytes.length);
  }

  @Nullable
  private static byte[] calculateIndexedHashForDocument(@NotNull FileContentImpl content) {
    Document document = FileDocumentManager.getInstance().getCachedDocument(content.getFile());
    if (document != null) {  // if document is not committed
      PsiDocumentManager psiDocumentManager = PsiDocumentManager.getInstance(content.getProject());

      if (psiDocumentManager.isUncommited(document)) {
        PsiFile file = psiDocumentManager.getCachedPsiFile(document);

        if (file != null) {
          Charset charset = content.getCharset();
          FileType fileType = content.getFileType();
          return mergeIndexedHash(calculateFileContentHash(file.getText().getBytes(charset)), charset, fileType);
        }
      }
    }
    return null;
  }

  @NotNull
  private static byte[] mergeIndexedHash(@NotNull byte[] binaryContentHash,
                                         @Nullable Charset charsetOrNullForBinary,
                                         @NotNull FileType fileType) {
    MessageDigest messageDigest = ContentHashesUtil.HASHER_CACHE.getValue();

    LOG.assertTrue(binaryContentHash.length == ContentHashesUtil.SIGNATURE_LENGTH,
                   "binaryContentHash should be a pre-calculated sha1 signature");

    messageDigest.reset();
    messageDigest.update(binaryContentHash);
    messageDigest.update(fileType.getName().getBytes(ContentHashesUtil.HASHER_CHARSET));
    if (charsetOrNullForBinary != null) {
      messageDigest.update(charsetOrNullForBinary.name().getBytes(ContentHashesUtil.HASHER_CHARSET));
    }

    return messageDigest.digest();
  }
}