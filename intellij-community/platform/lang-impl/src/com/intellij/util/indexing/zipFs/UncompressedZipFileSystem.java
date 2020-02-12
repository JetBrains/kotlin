// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing.zipFs;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.ArrayUtil;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.io.zip.JBZipEntry;
import com.intellij.util.io.zip.JBZipFile;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.spi.FileSystemProvider;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.zip.ZipEntry;

public final class UncompressedZipFileSystem extends FileSystem {
  private static final Logger LOG = Logger.getInstance(UncompressedZipFileSystem.class);

  private volatile ZipTreeNode myRoot;

  @NotNull
  private final Path myUncompressedZipPath;
  @NotNull
  private final UncompressedZipFileSystemProvider myProvider;

  private final ReadWriteLock myOpenFilePoolLock = new ReentrantReadWriteLock();
  private final Map<FileChannel, Set<FileBlockReadOnlyFileChannel>> myOpenFilePool = new ConcurrentHashMap<>();
  private volatile FileChannel myCurrentZipChannel;

  UncompressedZipFileSystem(@NotNull Path uncompressedZip, @NotNull UncompressedZipFileSystemProvider provider) throws IOException {
    myUncompressedZipPath = uncompressedZip;
    myProvider = provider;
    assert uncompressedZip.getFileSystem() == FileSystems.getDefault();
    sync();
  }

  @NotNull
  public static UncompressedZipFileSystem create(@NotNull Path uncompressedZip) throws IOException {
    return UncompressedZipFileSystemProvider.INSTANCE.newFileSystem(uncompressedZip);
  }

  @NotNull
  FileBlockReadOnlyFileChannel openChannel(@NotNull LightZipEntry entry) throws IOException {
    Lock lock = myOpenFilePoolLock.readLock();
    lock.lock();
    try {
      final FileChannel zipChannel = myCurrentZipChannel;
      FileBlockReadOnlyFileChannel channel = new FileBlockReadOnlyFileChannel(zipChannel, entry.offset, entry.size) {
        @Override
        protected void implCloseChannel() throws IOException {
          Set<FileBlockReadOnlyFileChannel> channels = myOpenFilePool.get(zipChannel);
          channels.remove(this);
          if (channels.isEmpty() && zipChannel != myCurrentZipChannel) {
            myOpenFilePool.remove(zipChannel);
            zipChannel.close();
          }
        }
      };
      myOpenFilePool.computeIfAbsent(zipChannel, __ -> ContainerUtil.newConcurrentSet()).add(channel);
      return channel;
    }
    finally {
      lock.unlock();
    }
  }

  public void sync() throws IOException {
    // reopen channel
    reopenZipChannel();
    // open
    buildTree();
  }

  @Override
  public FileSystemProvider provider() {
    return myProvider;
  }

  @Override
  public void close() throws IOException {
    closeOpenFiles();
  }

  @NotNull
  Path getUncompressedZipPath() {
    return myUncompressedZipPath;
  }

  @NotNull
  FileChannel getCurrentChannel() {
    return myCurrentZipChannel;
  }

  @Override
  public boolean isOpen() {
    return myCurrentZipChannel.isOpen();
  }

  @Override
  public boolean isReadOnly() {
    return true;
  }

  @Override
  public String getSeparator() {
    return "/";
  }

  @Override
  public Iterable<Path> getRootDirectories() {
    return Collections.singleton(new UncompressedZipPath(this, ArrayUtil.EMPTY_STRING_ARRAY, true));
  }

  @Override
  public Iterable<FileStore> getFileStores() {
    return Collections.singleton(new UncompressedZipFileStore(this));
  }

  @Override
  public Set<String> supportedFileAttributeViews() {
    return Collections.singleton("basic");
  }

  @NotNull
  @Override
  public Path getPath(@NotNull String first, String @NotNull ... more) {
    // should it be absolute
    String[] nameElements = ArrayUtil.toStringArray(ContainerUtil.concat(Collections.singletonList(first), Arrays.asList(more)));
    return new UncompressedZipPath(this, nameElements, true);
  }

  @Override
  public PathMatcher getPathMatcher(String syntaxAndPattern) {
    throw new UnsupportedOperationException();
  }

  @Override
  public UserPrincipalLookupService getUserPrincipalLookupService() {
    throw new UnsupportedOperationException();
  }

  @Override
  public WatchService newWatchService() {
    throw new UnsupportedOperationException();
  }

  ZipTreeNode getRoot() {
    return myRoot;
  }

  // use a separate class to don't hold references for a file tree
  final static class LightZipEntry {
    final long size;
    final long offset;
    LightZipEntry(JBZipEntry entry) throws IOException {
      size = entry.getCompressedSize();
      offset = entry.calcDataOffset();
    }
  }

  final static class ZipTreeNode {
    @Nullable
    private final Map<String, ZipTreeNode> myChildren;
    @Nullable
    private final LightZipEntry myEntry;

    public boolean isDirectory() {
      return myChildren != null;
    }

    @NotNull
    LightZipEntry getEntry() {
      assert myEntry != null;
      return myEntry;
    }

    @NotNull
    @Contract(pure = true)
    Set<String> getChildNames() {
      assert myChildren != null;
      return myChildren.keySet();
    }

    ZipTreeNode(@NotNull JBZipEntry entry) throws IOException {
      myEntry = new LightZipEntry(entry);
      myChildren = null;
    }

    ZipTreeNode() {
      myEntry = null;
      myChildren = new ConcurrentHashMap<>();
    }

    @Nullable
    ZipTreeNode getChild(@NotNull String childName) {
      assert myChildren != null;
      return myChildren.get(childName);
    }

    @NotNull
    ZipTreeNode createDirChild(@NotNull String childName) {
      assert myChildren != null;
      return myChildren.computeIfAbsent(childName, __ -> new ZipTreeNode());
    }

    void createEntryChild(@NotNull String childName, @NotNull JBZipEntry entry) throws IOException {
      assert myChildren != null;
      ZipTreeNode previous = myChildren.put(childName, new ZipTreeNode(entry));
      assert previous == null;
    }
  }

  private void reopenZipChannel() throws IOException {
    Lock lock = myOpenFilePoolLock.writeLock();
    lock.lock();
    try {
      if (!Files.exists(myUncompressedZipPath)) return;
      FileChannel previousZipChannel = myCurrentZipChannel;
      myCurrentZipChannel = FileChannel.open(myUncompressedZipPath, StandardOpenOption.READ);
      if (previousZipChannel != null && ContainerUtil.isEmpty(myOpenFilePool.get(previousZipChannel))) {
        try {
          myOpenFilePool.remove(previousZipChannel);
          previousZipChannel.close();
        }
        catch (IOException e) {
          LOG.error(e);
        }
      }
    } finally {
      lock.unlock();
    }
  }

  private void closeOpenFiles() throws IOException {
    Lock lock = myOpenFilePoolLock.writeLock();
    lock.lock();
    try {
      for (Map.Entry<FileChannel, Set<FileBlockReadOnlyFileChannel>> entry : myOpenFilePool.entrySet()) {
        for (FileBlockReadOnlyFileChannel channel : entry.getValue()) {
          channel.close();
        }
        entry.getKey().close();
      }
      myCurrentZipChannel.close();
    } finally {
      lock.unlock();
    }
  }

  private void buildTree() throws IOException {
    ZipTreeNode root = new ZipTreeNode();
    try (JBZipFile zip = new JBZipFile(myUncompressedZipPath.toFile())) {
      for (JBZipEntry entry : zip.getEntries()) {
        if (entry.isDirectory()) continue;
        List<String> names = StringUtil.split(entry.getName(), getSeparator());
        ZipTreeNode current = root;
        for (int i = 0; i < names.size(); i++) {
          if (i == names.size() - 1) {
            current.createEntryChild(names.get(i), entry);
          }
          else {
            current = current.createDirChild(names.get(i));
          }
        }
      }
    }
    myRoot = root;
  }
}
