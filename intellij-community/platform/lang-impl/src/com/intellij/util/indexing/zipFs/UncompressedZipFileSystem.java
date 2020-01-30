// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing.zipFs;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.ArrayUtil;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.io.zip.JBZipEntry;
import com.intellij.util.io.zip.JBZipFile;
import gnu.trove.THashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.spi.FileSystemProvider;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class UncompressedZipFileSystem extends FileSystem {
  private static final Logger LOG = Logger.getInstance(UncompressedZipFileSystem.class);

  private volatile JBZipFile myUncompressedZip;
  private volatile FileChannel myChannel;
  private volatile ZipTreeNode myRoot;

  @NotNull
  private final Path myUncompressedZipPath;
  @NotNull
  private final UncompressedZipFileSystemProvider myProvider;

  // probably should be eliminated
  private final Set<FileChannel> myOpenFiles = Collections.newSetFromMap(new ConcurrentHashMap<>());

  public UncompressedZipFileSystem(@NotNull Path uncompressedZip, @NotNull UncompressedZipFileSystemProvider provider) throws IOException {
    myUncompressedZipPath = uncompressedZip;
    myProvider = provider;
    assert uncompressedZip.getFileSystem() == FileSystems.getDefault();
    sync();
  }

  @NotNull
  FileChannel openChannel(@NotNull JBZipEntry entry) throws IOException {
    FileBlockReadOnlyFileChannel channel = new FileBlockReadOnlyFileChannel(myChannel, entry.calcDataOffset(), entry.getSize()) {
      @Override
      protected void implCloseChannel() {
        myOpenFiles.remove(this);
      }
    };
    myOpenFiles.add(channel);
    return channel;
  }

  public void sync() throws IOException {
    try {
      if (myUncompressedZip != null) {
        try {
          myUncompressedZip.close();
        }
        catch (IOException e) {
          LOG.error(e);
        }
      }
    } finally {
      myUncompressedZip = new JBZipFile(myUncompressedZipPath.toFile());
      if (myChannel == null) {
        myChannel = FileChannel.open(myUncompressedZipPath, StandardOpenOption.READ);
      } else {
        myChannel.force(true);
      }
      buildTree();
    }
  }

  private void buildTree() {
    myRoot = new ZipTreeNode();
    for (JBZipEntry entry : myUncompressedZip.getEntries()) {
      if (entry.isDirectory()) continue;
      List<String> names = StringUtil.split(entry.getName(), getSeparator());
      ZipTreeNode current = myRoot;
      for (int i = 0; i < names.size(); i++) {
        if (i == names.size() - 1) {
          current.createEntryChild(names.get(i), entry);
        } else {
          current = current.createDirChild(names.get(i));
        }
      }
    }
  }

  @Override
  public FileSystemProvider provider() {
    return myProvider;
  }

  @Override
  public void close() throws IOException {
    try {
      myChannel.close();
    } finally {
      myUncompressedZip.close();
    }
  }

  @NotNull
  Path getUncompressedZipPath() {
    return myUncompressedZipPath;
  }

  @NotNull
  FileChannel getChannel() {
    return myChannel;
  }

  @Override
  public boolean isOpen() {
    return myChannel.isOpen();
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

  static class ZipTreeNode {
    @Nullable
    private final Map<String, ZipTreeNode> myChildren;
    @Nullable
    private final JBZipEntry myEntry;

    public boolean isDirectory() {
      return myChildren != null;
    }

    @NotNull
    JBZipEntry getEntry() {
      assert myEntry != null;
      return myEntry;
    }

    Set<String> getChildNames() {
      assert myChildren != null;
      return myChildren.keySet();
    }

    ZipTreeNode(@NotNull JBZipEntry entry) {
      myEntry = entry;
      myChildren = null;
    }

    ZipTreeNode() {
      myEntry = null;
      myChildren = new THashMap<>();
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

    void createEntryChild(@NotNull String childName, @NotNull JBZipEntry entry) {
      assert myChildren != null;
      ZipTreeNode previous = myChildren.put(childName, new ZipTreeNode(entry));
      assert previous == null;
    }
  }
}
