// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.service.project.autoimport;

import com.intellij.lang.Language;
import com.intellij.lang.cacheBuilder.CacheBuilderRegistry;
import com.intellij.lang.cacheBuilder.WordOccurrence;
import com.intellij.lang.cacheBuilder.WordsScanner;
import com.intellij.lang.findUsages.LanguageFindUsages;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.impl.LoadTextUtil;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.zip.CRC32;

/**
 * @author Vladislav.Soroka
 */
public class ConfigurationFileCrcFactory {

  private static final Logger LOG = Logger.getInstance(ConfigurationFileCrcFactory.class);

  private final VirtualFile myFile;

  @Deprecated // left for plugin compatibility
  public ConfigurationFileCrcFactory(@NotNull Project project, @NotNull VirtualFile file) {
    this(file);
  }

  public ConfigurationFileCrcFactory(VirtualFile file) {
    myFile = file;
  }

  public long create() {
    if (myFile.isDirectory()) {
      debug("Cannot calculate CRC for directory '" + myFile.getPath() + "'");
      return myFile.getModificationStamp();
    }

    WordsScanner wordsScanner = getScanner(myFile);
    if (wordsScanner == null) {
      debug("WordsScanner not found for file '" + myFile.getPath() + "'");
      return myFile.getModificationStamp();
    }

    CRC32 crc32 = new CRC32();
    Document document = FileDocumentManager.getInstance().getCachedDocument(myFile);
    CharSequence text = document != null ? document.getImmutableCharSequence() : LoadTextUtil.loadText(myFile);
    wordsScanner.processWords(text, occurrence -> {
      if (occurrence.getKind() != WordOccurrence.Kind.COMMENTS) {
        CharSequence currentWord = occurrence.getBaseText().subSequence(occurrence.getStart(), occurrence.getEnd());
        for (int i = 0, end = currentWord.length(); i < end; i++) {
          crc32.update(currentWord.charAt(i));
        }
      }
      return true;
    });
    return crc32.getValue();
  }

  @Nullable
  private static WordsScanner getScanner(VirtualFile file) {
    FileType fileType = file.getFileType();
    final WordsScanner customWordsScanner = CacheBuilderRegistry.getInstance().getCacheBuilder(fileType);
    if (customWordsScanner != null) {
      return customWordsScanner;
    }

    if (fileType instanceof LanguageFileType) {
      final Language lang = ((LanguageFileType)fileType).getLanguage();
      return LanguageFindUsages.getWordsScanner(lang);
    }
    return null;
  }

  private static void debug(@NotNull String message) {
    if (LOG.isDebugEnabled()) {
      LOG.debug(message);
    }
  }
}
