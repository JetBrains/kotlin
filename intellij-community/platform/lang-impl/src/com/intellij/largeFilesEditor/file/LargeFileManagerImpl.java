// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.largeFilesEditor.file;

import com.google.common.collect.EvictingQueue;
import com.intellij.largeFilesEditor.editor.Page;
import com.intellij.largeFilesEditor.search.searchTask.FileDataProviderForSearch;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.concurrency.SequentialTaskExecutor;
import org.jetbrains.annotations.NotNull;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Queue;
import java.util.concurrent.ExecutorService;

public class LargeFileManagerImpl implements LargeFileManager {
  private static final Logger logger = Logger.getInstance(LargeFileManagerImpl.class);
  private static final int MAX_SIZE_OF_PAGE_CASH = 3;

  private FileAdapter fileAdapter;
  private final Queue<Page> notUpdatedPagesCash;
  private final ExecutorService readingPageExecutor =
    SequentialTaskExecutor.createSequentialApplicationPoolExecutor("Large File Editor Reading File Executor");

  public LargeFileManagerImpl(VirtualFile vFile, int pageSize, int maxPageBorderShift) throws FileNotFoundException {
    this.fileAdapter = new FileAdapter(pageSize, maxPageBorderShift, vFile);
    notUpdatedPagesCash = EvictingQueue.create(MAX_SIZE_OF_PAGE_CASH);
  }

  @Override
  synchronized public void reset(Charset charset) {
    notUpdatedPagesCash.clear();
    fileAdapter.setCharset(charset);
  }

  @Override
  public void dispose() {
    try {
      fileAdapter.closeFile();
    }
    catch (IOException e) {
      logger.warn(e);
    }
  }

  @Override
  public String getCharsetName() {
    return fileAdapter.getCharsetName();
  }

  @Override
  public long getPagesAmount() throws IOException {
    return fileAdapter.getPagesAmount();
  }

  @Override
  public int getPageSize() {
    return fileAdapter.getPageSize();
  }

  /**
   * Warning! Thread-blocking method
   *
   * @param pageNumber - page number
   * @return updated page for specified page number.
   * @throws IOException - error working with file`
   */
  @Override
  @NotNull
  synchronized public Page getPage_wait(long pageNumber) throws IOException {
    String notUpdatedPageText;

    Page notUpdatedPage = null;
    for (Page page : notUpdatedPagesCash) {
      if (page.getPageNumber() == pageNumber) {
        notUpdatedPage = page;
      }
    }

    if (notUpdatedPage != null) {
      notUpdatedPageText = notUpdatedPage.getText();
    }
    else {
      notUpdatedPageText = fileAdapter.getPageText(pageNumber);
      notUpdatedPage = new Page(notUpdatedPageText, pageNumber);
      notUpdatedPagesCash.add(notUpdatedPage);
    }

    // TODO: 2019-05-14 Temporary solution for problem: soft-wrapping restricted for document, where isAcceptSlashR flag is TRUE. See SoftWrapModelImpl#areSoftWrapsEnabledInEditor()
    String notUpdatedPageTextWithoutSlashR = destroySlashRSeparators(notUpdatedPageText);

    return new Page(notUpdatedPageTextWithoutSlashR, pageNumber);
  }

  private static String destroySlashRSeparators(String str) {
    return str.replace("\r\n", "\n").replace("\r", "\n");
  }

  @Override
  public boolean hasBOM() {
    VirtualFile vFile = fileAdapter.getVirtualFile();
    return vFile.getBOM() != null && vFile.getBOM().length > 0;
  }

  @Override
  public String getFileName() {
    return fileAdapter.getFileName();
  }

  @Override
  public FileDataProviderForSearch getFileDataProviderForSearch() {
    return new FileDataProviderForSearch() {
      @Override
      public long getPagesAmount() throws IOException {
        return LargeFileManagerImpl.this.getPagesAmount();
      }

      @Override
      public Page getPage_wait(long pageNumber) throws IOException {
        return LargeFileManagerImpl.this.getPage_wait(pageNumber);
      }

      @Override
      public String getName() {
        return LargeFileManagerImpl.this.getFileName();
      }
    };
  }

  @Override
  public void requestReadPage(long pageNumber, ReadingPageResultHandler readingPageResultHandler) {
    readingPageExecutor.execute(() -> {
      try {
        Page page = getPage_wait(pageNumber);
        readingPageResultHandler.run(page);
      }
      catch (IOException e) {
        logger.warn(e);
        readingPageResultHandler.run(null);
      }
    });
  }
}

