// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.largeFilesEditor.file;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vfs.CharsetToolkit;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;
import java.util.concurrent.locks.ReentrantLock;

class FileAdapter {
  private static final Logger logger = Logger.getInstance(FileAdapter.class);
  private static final int UNDEFINED = -1;

  private final VirtualFile vFile;
  private final RandomAccessFile randomAccessFile;
  private final ReentrantLock randomAccessFileLock = new ReentrantLock();

  private final int pageSize; // in bytes
  private final int maxPageBorderShift; // in bytes

  volatile private long cashedFileSize = UNDEFINED; // in bytes

  FileAdapter(int pageSize, int maxPageBorderShift, @NotNull VirtualFile vFile) throws FileNotFoundException {
    this.pageSize = pageSize;
    this.maxPageBorderShift = maxPageBorderShift;
    this.vFile = vFile;

    randomAccessFile = new RandomAccessFile(VfsUtilCore.virtualToIoFile(vFile), "r");
    try {
      cashedFileSize = randomAccessFile.length();
    }
    catch (IOException e) {
      logger.warn(e);
    }
  }

  Charset getCharset() {
    return vFile.getCharset();
  }

  void setCharset(Charset newCharset) {
    randomAccessFileLock.lock();
    try {
      vFile.setCharset(newCharset);
    }
    finally {
      randomAccessFileLock.unlock();
    }
  }

  void closeFile() throws IOException {
    randomAccessFileLock.lock();
    try {
      if (randomAccessFile != null) {
        randomAccessFile.close();
      }
    }
    finally {
      randomAccessFileLock.unlock();
    }
  }

  String getCharsetName() {
    return vFile.getCharset().name();
  }

  // TODO: 22.01.19 cashedFileSize is not the best solving of race condition, but it's better than "synchronized" in performance. Probably it's still need to be done in another way.
  long getPagesAmount() throws IOException {
    if (randomAccessFileLock.isHeldByCurrentThread()) {
      cashedFileSize = randomAccessFile.length();
    }
    else {
      if (randomAccessFileLock.tryLock()) {
        try {
          cashedFileSize = randomAccessFile.length();
        }
        finally {
          randomAccessFileLock.unlock();
        }
      }
      else {
        if (cashedFileSize == UNDEFINED) {
          randomAccessFileLock.lock();
          try {
            cashedFileSize = randomAccessFile.length();
          }
          finally {
            randomAccessFileLock.unlock();
          }
        }
      }
    }

    return (cashedFileSize + pageSize - 1) / pageSize;
  }

  int getPageSize() {
    return pageSize;
  }

  @NotNull
  VirtualFile getVirtualFile() {
    return vFile;
  }

  String getFileName() {
    return vFile.getName();
  }

  /**
   * @param pageNumber - page number
   * @return text of the page
   * @throws NullPointerException - when access to physical file was not established
   */
  @NotNull
  String getPageText(long pageNumber) throws IOException {

    randomAccessFileLock.lock();
    try {

      /*DEBUG BEGIN*/
      //java.util.Random rand = new java.util.Random(System.currentTimeMillis());
      //if (true) try { Thread.sleep(rand.nextInt(100));} catch (InterruptedException e) {}
      //if (rand.nextInt(10) < 1) throw new FileNotFoundException();
      //try { Thread.sleep(10);} catch (InterruptedException e) {}
      /*DEBUG END*/

      long minProbStartPos;
      long startByte;
      long endByte;

      minProbStartPos = pageNumber * pageSize;
      if (pageNumber == 0) {
        startByte = 0;
      }
      else {
        startByte = minProbStartPos + findPageShiftFrom(minProbStartPos, randomAccessFile, vFile.getCharset(), maxPageBorderShift);
      }

      if (startByte > randomAccessFile.length() - 1) {
        return "";  // situation, when penultimate page overlaps the last page completely
      }

      minProbStartPos += pageSize;
      if (minProbStartPos >= randomAccessFile.length()) {
        endByte = randomAccessFile.length();
      }
      else {
        endByte = minProbStartPos + findPageShiftFrom(minProbStartPos, randomAccessFile, vFile.getCharset(), maxPageBorderShift);
      }

      randomAccessFile.seek(startByte);
      byte[] buffer = new byte[(int)(endByte - startByte)]; // difference should be a value near pageSize, so it is less than maxInteger
      randomAccessFile.readFully(buffer);
      return CharsetToolkit.decodeString(buffer, vFile.getCharset());
    }
    finally {
      randomAccessFileLock.unlock();
    }
  }

  private static int findPageShiftFrom(long minProbStartPos,
                                       RandomAccessFile randomAccessFile,
                                       Charset charset,
                                       int maxPageBorderShift) throws IOException {

    int offsetToSymbolBeginning = findNextSymbolBeginningOffsetFrom(minProbStartPos, randomAccessFile, charset);

    if (offsetToSymbolBeginning >= maxPageBorderShift) {
      return offsetToSymbolBeginning;
    }

    int bufferLength = minProbStartPos + maxPageBorderShift >= randomAccessFile.length() ?
                       (int)(randomAccessFile.length() - minProbStartPos - offsetToSymbolBeginning) :
                       maxPageBorderShift - offsetToSymbolBeginning;
    byte[] buffer1 = new byte[bufferLength];

    randomAccessFile.seek(minProbStartPos + offsetToSymbolBeginning);
    randomAccessFile.readFully(buffer1);
    String text1 = CharsetToolkit.decodeString(buffer1, charset);


    int substringLength = -1;

    int indexOfSlashN = text1.indexOf('\n');
    if (indexOfSlashN != -1) {
      substringLength = indexOfSlashN + 1;  // for cases: "\n" and "\r\n"
    }
    else {
      int indexOfSlashR = text1.indexOf('\r');
      if (indexOfSlashR != -1) {
        substringLength = indexOfSlashR + 1;  // for case: "\r"
      }
    }

    if (substringLength != -1) {
      int substringLengthInBytes;
      if (charset.equals(CharsetToolkit.UTF_16_CHARSET)) {
        substringLengthInBytes = text1.substring(0, substringLength).getBytes(CharsetToolkit.UTF_16BE_CHARSET).length;
      }
      else {
        substringLengthInBytes = text1.substring(0, substringLength).getBytes(charset).length;
      }
      return offsetToSymbolBeginning + substringLengthInBytes;
    }
    else {
      return offsetToSymbolBeginning;
    }
  }

  private static int findNextSymbolBeginningOffsetFrom(long numberOfStartByte,
                                                       RandomAccessFile randomAccessFile,
                                                       Charset charset) throws IOException {
    if (charset.compareTo(CharsetToolkit.UTF8_CHARSET) == 0) {
      return findNextSymbolBeginningOffsetFrom_UTF8(numberOfStartByte, randomAccessFile);
    }
    else if (charset.compareTo(CharsetToolkit.UTF_16_CHARSET) == 0
             || charset.compareTo(CharsetToolkit.UTF_16BE_CHARSET) == 0
             || charset.compareTo(CharsetToolkit.UTF_16LE_CHARSET) == 0) {
      return findNextSymbolBeginningOffsetFrom_UTF16(numberOfStartByte, randomAccessFile, charset);
    }
    else if (charset.compareTo(CharsetToolkit.UTF_32BE_CHARSET) == 0
             || charset.compareTo(CharsetToolkit.UTF_32LE_CHARSET) == 0) {
      return findNextSymbolBeginningOffsetFrom_UTF32(numberOfStartByte);
    }
    else {
      return 0;
    }
    //throw new NotImplementedException("not supported yet");
  }

  private static int findNextSymbolBeginningOffsetFrom_UTF8(long numberOfStartByte,
                                                            RandomAccessFile randomAccessFile) throws IOException {
    final int maxSymbolLengthInUtf8 = 4;
    randomAccessFile.seek(numberOfStartByte);
    int offset = 0;
    byte _byte;
    for (int i = 0; i < maxSymbolLengthInUtf8; i++) {
      try {
        _byte = randomAccessFile.readByte();
      }
      catch (EOFException e) {
        return offset;
      }
      if ((_byte & 0xC0) != 0x80) // _byte != (10xx xxxx)
      {
        return offset;
      }

      offset += 1;
    }

    logger.warn("Can't decode file correctly in UTF-8. There are more than 3 bytes one after another, which match bit mask 10xxxxxx");
    return 0;
  }

  private static int findNextSymbolBeginningOffsetFrom_UTF32(long numberOfStartByte) {
    if (numberOfStartByte % 4 == 0) {
      return 0;
    }
    else {
      return (int)(4 - (numberOfStartByte % 4));
    }
  }

  private static int findNextSymbolBeginningOffsetFrom_UTF16(long numberOfStartByte,
                                                             RandomAccessFile randomAccessFile,
                                                             Charset charset) throws IOException {
    // utf16-text only consists of words (word = 2 bytes)
    long offset = numberOfStartByte % 2;

    // taking the HIGH byte of the word
    if (charset.compareTo(CharsetToolkit.UTF_16_CHARSET) == 0 || charset.compareTo(CharsetToolkit.UTF_16BE_CHARSET) == 0) {
      randomAccessFile.seek(numberOfStartByte + offset);
    }
    else {
      randomAccessFile.seek(numberOfStartByte + offset + 1);
    }

    int unsignedByte = randomAccessFile.readByte() & 0xFF; // (<byte> & 0xFF) returns unsigned integer value of <byte>
    if (unsignedByte <= 0xD8 || unsignedByte >= 0xE0) {
      return (int)offset;  // it is sym=1word
    }
    else if (unsignedByte <= 0xDB) {
      return (int)offset;  // it is high word of sym=2words
    }
    else {
      return (int)offset + 2; // it is low word of syn=2words
    }
  }
}
