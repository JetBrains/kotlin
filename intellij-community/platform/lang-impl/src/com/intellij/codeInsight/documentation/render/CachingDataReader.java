// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.documentation.render;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.io.FileUtilRt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Cache for accessing remote resources (the ones fetched via FTP, HTTP and HTTPS protocols are stored on disk for further accesses).
 * Disk cache is cleaned on IDE exit.
 */
@Service
public final class CachingDataReader implements Disposable {
  private static final Logger LOG = Logger.getInstance(CachingDataReader.class);
  private static final int MAX_DATA_SIZE = 1_000_000;

  public static CachingDataReader getInstance() {
    return ApplicationManager.getApplication().getService(CachingDataReader.class);
  }

  private final ConcurrentMap<URL, File> myCache = new ConcurrentHashMap<>();
  private final AtomicInteger myFileNameCounter = new AtomicInteger();

  public @Nullable InputStream getInputStream(@NotNull URL url) {
    File cachedResult = myCache.get(url);
    if (cachedResult != null) {
      try {
        return new FileInputStream(cachedResult);
      }
      catch (FileNotFoundException e) {
        LOG.warn("Couldn't open " + cachedResult, e);
      }
    }
    try {
      return shouldCache(url) ? new CachingInputStream(url) : url.openStream();
    }
    catch (IOException e) {
      LOG.debug("Couldn't open " + url, e);
    }
    return null;
  }

  private static boolean shouldCache(@NotNull URL url) {
    String protocol = url.getProtocol();
    return "ftp".equals(protocol) || "http".equals(protocol) || "https".equals(protocol);
  }

  private void cacheResult(@NotNull URL url, byte @NotNull [] data) {
    File folder = getCacheFolder();
    if (!folder.exists() && !folder.mkdirs()) {
      LOG.warn("Couldn't create " + folder);
      return;
    }
    File file = new File(folder, generateFileName());
    try {
      Files.write(file.toPath(), data);
      myCache.put(url, file);
    }
    catch (IOException e) {
      LOG.warn("Error writing to " + file, e);
    }
  }

  private static File getCacheFolder() {
    return new File(PathManager.getTempPath(), "imageCache");
  }

  private String generateFileName() {
    return Integer.toString(myFileNameCounter.incrementAndGet());
  }

  @Override
  public void dispose() {
    File folder = getCacheFolder();
    if (folder.exists() && !FileUtilRt.delete(folder)) {
      LOG.warn("Error deleting folder " + folder);
    }
  }

  private class CachingInputStream extends InputStream {
    private final URL myURL;
    private final InputStream myDelegate;
    private ByteArrayOutputStream myStreamCopy;

    private CachingInputStream(@NotNull URL url) throws IOException {
      myURL = url;
      myDelegate = url.openStream();
      myStreamCopy = new ByteArrayOutputStream();
    }

    @Override
    public int read() throws IOException {
      int data;
      try {
        data = myDelegate.read();
      }
      catch (IOException e) {
        myStreamCopy = null;
        throw e;
      }
      if (myStreamCopy != null) {
        if (data < 0) {
          cacheResult(myURL, myStreamCopy.toByteArray());
          myStreamCopy = null;
        }
        else if (myStreamCopy.size() + 1 > MAX_DATA_SIZE) {
          myStreamCopy = null;
        }
        else {
          myStreamCopy.write(data);
        }
      }
      return data;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
      int result;
      try {
        result = myDelegate.read(b, off, len);
      }
      catch (IOException e) {
        myStreamCopy = null;
        throw e;
      }
      if (myStreamCopy != null) {
        if (result < 0) {
          cacheResult(myURL, myStreamCopy.toByteArray());
          myStreamCopy = null;
        }
        else if (myStreamCopy.size() + result > MAX_DATA_SIZE) {
          myStreamCopy = null;
        }
        else {
          myStreamCopy.write(b, off, result);
        }
      }
      return result;
    }

    @Override
    public long skip(long n) throws IOException {
      myStreamCopy = null;
      return myDelegate.skip(n);
    }

    @Override
    public int available() throws IOException {
      return myDelegate.available();
    }

    @Override
    public void close() throws IOException {
      myDelegate.close();
    }
  }
}
