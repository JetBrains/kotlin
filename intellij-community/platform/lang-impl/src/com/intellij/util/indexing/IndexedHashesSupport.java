// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.vfs.newvfs.persistent.FSRecords;
import com.intellij.openapi.vfs.newvfs.persistent.PersistentFSImpl;
import com.intellij.util.indexing.flavor.FileIndexingFlavorProvider;
import com.intellij.util.indexing.flavor.HashBuilder;
import com.intellij.util.io.DigestUtil;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;

@ApiStatus.Internal
public final class IndexedHashesSupport {
  // TODO replace with sha-256
  private static final HashFunction INDEXED_FILE_CONTENT_HASHER = Hashing.sha1();

  public static int getVersion() {
    return 3;
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
    Hasher hasher = INDEXED_FILE_CONTENT_HASHER.newHasher();

    byte[] contentHash = PersistentFSImpl.getContentHashIfStored(content.getFile());
    if (contentHash == null) {
      contentHash = DigestUtil.calculateContentHash(FSRecords.CONTENT_HASH_DIGEST, ((FileContent)content).getContent());
      // todo store content hash in FS
    }
    hasher.putBytes(contentHash);

    if (!content.getFileTypeWithoutSubstitution().isBinary()) {
      hasher.putString(content.getCharset().name(), StandardCharsets.UTF_8);
    }

    hasher.putString(content.getFileName(), StandardCharsets.UTF_8);

    FileType fileType = content.getFileType();
    hasher.putString(fileType.getName(), StandardCharsets.UTF_8);

    @Nullable
    FileIndexingFlavorProvider<?> provider = FileIndexingFlavorProvider.INSTANCE.forFileType(fileType);
    if (provider != null) {
      buildFlavorHash(content, provider, new HashBuilder() {
        @Override
        public @NotNull HashBuilder putInt(int val) {
          hasher.putInt(val);
          return this;
        }

        @Override
        public @NotNull HashBuilder putBoolean(boolean val) {
          hasher.putBoolean(val);
          return this;
        }

        @Override
        public @NotNull HashBuilder putString(@NotNull CharSequence charSequence) {
          hasher.putString(charSequence, StandardCharsets.UTF_8);
          return this;
        }
      });
    }

    return hasher.hash().asBytes();
  }

  private static <F> void buildFlavorHash(@NotNull FileContent content,
                                          @NotNull FileIndexingFlavorProvider<F> flavorProvider,
                                          @NotNull HashBuilder hashBuilder) {
    F flavor = flavorProvider.getFlavor(content);
    hashBuilder.putString(flavorProvider.getId());
    hashBuilder.putInt(flavorProvider.getVersion());
    if (flavor != null) {
      flavorProvider.buildHash(flavor, hashBuilder);
    }
  }
}