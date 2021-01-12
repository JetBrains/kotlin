// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.largeFilesEditor.file;

import com.google.common.collect.EvictingQueue;
import com.intellij.largeFilesEditor.editor.Page;
import com.intellij.largeFilesEditor.search.searchTask.FileDataProviderForSearch;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.Alarm;
import com.intellij.util.concurrency.SequentialTaskExecutor;
import com.intellij.util.containers.ContainerUtil;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.locks.ReentrantLock;

public class LargeFileManagerImpl implements LargeFileManager {
  private static final Logger logger = Logger.getInstance(LargeFileManagerImpl.class);
  private static final int MAX_SIZE_OF_PAGE_CASH = 3;

  private final List<FileChangeListener> myFileChangeListeners = ContainerUtil.createLockFreeCopyOnWriteList();

  private FileAdapter fileAdapter;
  private final Queue<Page> notUpdatedPagesCash;
  private final ReentrantLock myLock = new ReentrantLock();
  private final ExecutorService readingPageExecutor =
    SequentialTaskExecutor.createSequentialApplicationPoolExecutor("Large File Editor Reading File Executor");

  public LargeFileManagerImpl(VirtualFile vFile, int pageSize, int maxPageBorderShift) throws FileNotFoundException {
    this.fileAdapter = new FileAdapter(pageSize, maxPageBorderShift, vFile);
    notUpdatedPagesCash = EvictingQueue.create(MAX_SIZE_OF_PAGE_CASH);
    startFileCheckingThread();
  }

  @Override
  public void reset(Charset charset) {
    myLock.lock();
    try {
      notUpdatedPagesCash.clear();
      fileAdapter.setCharset(charset);
    }
    finally {
      myLock.unlock();
    }
  }

  @Override
  public void dispose() {
    try {
      fileAdapter.closeFile();  // TODO close file in background?
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
  public Page getPage_wait(long pageNumber) throws IOException {
    myLock.lock();
    try {
      String notUpdatedPageText;

      long pagesAmount = fileAdapter.getPagesAmount();
      boolean neededPageShouldBeLastInFile = (pageNumber == pagesAmount - 1);

      Page notUpdatedPage = null;
      for (Iterator<Page> iterator = notUpdatedPagesCash.iterator(); iterator.hasNext(); ) {
        Page pageFromCash = iterator.next();
        if (pageFromCash.getPageNumber() == pageNumber) {
          boolean isOutdated = pageFromCash.isLastInFile() != neededPageShouldBeLastInFile;
          if (isOutdated) {
            iterator.remove();
          }
          else {
            notUpdatedPage = pageFromCash;
            break;
          }
        }
      }

      if (notUpdatedPage != null) {
        notUpdatedPageText = notUpdatedPage.getText();
      }
      else {
        notUpdatedPageText = fileAdapter.getPageText(pageNumber);
        if (notUpdatedPageText == null) {
          return null;
        }
        notUpdatedPage = new Page(notUpdatedPageText, pageNumber, neededPageShouldBeLastInFile);
        notUpdatedPagesCash.add(notUpdatedPage);
      }

      // TODO: 2019-05-14 Temporary solution for problem: soft-wrapping restricted for document, where isAcceptSlashR flag is TRUE. See SoftWrapModelImpl#areSoftWrapsEnabledInEditor()
      String notUpdatedPageTextWithoutSlashR = destroySlashRSeparators(notUpdatedPageText);

      return new Page(notUpdatedPageTextWithoutSlashR, pageNumber, neededPageShouldBeLastInFile);
    }
    finally {
      myLock.unlock();
    }
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

  @Override
  public void addFileChangeListener(FileChangeListener listener) {
    myFileChangeListeners.add(listener);
  }

  private void onFileChanged(boolean isLengthIncreased) {
    try {
      long pagesAmount = getPagesAmount();
      Page lastPage = getPage_wait(pagesAmount - 1);
      for (FileChangeListener listener : myFileChangeListeners) {
        listener.onFileChanged(lastPage, isLengthIncreased);
      }
    }
    catch (IOException e) {
      logger.warn(e);
    }
  }

  private void startFileCheckingThread() {
    Alarm alarm = new Alarm(Alarm.ThreadToUse.POOLED_THREAD, this);
    final Runnable task = new FileChangesChecker(alarm);
    alarm.addRequest(task, 10);
  }

  private class FileChangesChecker implements Runnable {
    private final Alarm alarm;
    private long prevFileSize;

    private FileChangesChecker(Alarm alarm) {
      this.alarm = alarm;
    }

    @Override
    public void run() {
      try {
        doCheck();
      }
      catch (IOException e) {
        logger.info(e);
        return;
      }

      if (alarm.isDisposed()) return;
      alarm.addRequest(this, 5000);
    }

    protected void doCheck() throws IOException {
      if (fileAdapter == null) {  // TODO mb to check if fileAdapter has been already closed? however "catch" will handle this, it's bad style
        return;
      }

      long deltaLength = refreshFileLength();
      if (deltaLength != 0) {
        removeOutdatedPagesFromCash();
        onFileChanged(deltaLength > 0);
      }
    }

    private void removeOutdatedPagesFromCash() throws IOException {
      myLock.lock();
      try {
        Iterator<Page> iterator = notUpdatedPagesCash.iterator();
        while (iterator.hasNext()) {
          Page cashedPage = iterator.next();
          String physicalPageText = fileAdapter.getPageText(cashedPage.getPageNumber());
          if (!cashedPage.getText().equals(physicalPageText)) {
            iterator.remove(); // TODO probably just update text in cashed page here instead of removing of page from cash?
          }
        }
      }
      finally {
        myLock.unlock();
      }
    }

    private long refreshFileLength() throws IOException {
      long newSize = fileAdapter.getFileSize();
      long delta = newSize - prevFileSize;
      prevFileSize = newSize;
      return delta;
    }
  }
}

