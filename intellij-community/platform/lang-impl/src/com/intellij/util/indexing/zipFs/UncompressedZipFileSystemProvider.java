// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing.zipFs;

import com.intellij.util.ArrayUtil;
import com.intellij.util.io.zip.JBZipEntry;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URI;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.spi.FileSystemProvider;
import java.util.*;

public final class UncompressedZipFileSystemProvider extends FileSystemProvider {

  public static final UncompressedZipFileSystemProvider INSTANCE = new UncompressedZipFileSystemProvider();

  private UncompressedZipFileSystemProvider() {
  }

  @Override
  public String getScheme() {
    return "zip0";
  }

  @Override
  public FileSystem newFileSystem(URI uri, Map<String, ?> env) {
    throw new UnsupportedOperationException();
  }

  @Override
  public FileSystem getFileSystem(URI uri) {
    throw new UnsupportedOperationException();
  }

  @NotNull
  public UncompressedZipFileSystem newFileSystem(Path uncompressedZip) throws IOException {
    return newFileSystem(uncompressedZip, Collections.emptyMap());
  }

  @Override
  public UncompressedZipFileSystem newFileSystem(Path uncompressedZip, Map<String, ?> env) throws IOException {
    return new UncompressedZipFileSystem(uncompressedZip, this);
  }

  @NotNull
  @Override
  public Path getPath(@NotNull URI uri) {
    throw new UnsupportedOperationException();
  }

  @Override
  public SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs) throws IOException {
    return newFileChannel(path, options, attrs);
  }

  @Override
  public FileChannel newFileChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs) throws IOException {
    assert options != null;
    //TODO we should not open a zip to write
    //if (options.contains(StandardOpenOption.CREATE) ||
    //    options.contains(StandardOpenOption.CREATE_NEW) ||
    //    options.contains(StandardOpenOption.WRITE)) {
    //  throw new UnsupportedOperationException();
    //}
    UncompressedZipFileSystem.ZipTreeNode node = find(path);
    if (node.isDirectory()) {
      throw new IllegalArgumentException(path.toString());
    }
    JBZipEntry entry = node.getEntry();
    return ((UncompressedZipFileSystem)path.getFileSystem()).openChannel(entry);
  }

  @Override
  public DirectoryStream<Path> newDirectoryStream(Path dir, DirectoryStream.Filter<? super Path> filter) throws IOException {
    UncompressedZipFileSystem.ZipTreeNode element = find(dir);
    if (!element.isDirectory()) {
      throw new NotDirectoryException(dir.toString());
    }
    return new DirectoryStream<Path>() {
      @Override
      public Iterator<Path> iterator() {
        return element.getChildNames().stream().map(n -> dir.resolve(n)).filter(p -> {
          try {
            return filter.accept(p);
          }
          catch (IOException e) {
            throw new RuntimeException(e);
          }
        }).iterator();
      }

      @Override
      public void close() { }
    };
  }

  @Override
  public void createDirectory(Path dir, FileAttribute<?>... attrs) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void delete(Path path) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void copy(Path source, Path target, CopyOption... options) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void move(Path source, Path target, CopyOption... options) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isSameFile(Path path1, Path path2) throws IOException {
    if (path1.getFileSystem().provider() != path2.getFileSystem().provider()) {
      return false;
    }
    UncompressedZipFileSystem system1 = (UncompressedZipFileSystem)path1.getFileSystem();
    UncompressedZipFileSystem system2 = (UncompressedZipFileSystem)path2.getFileSystem();
    if (!Files.isSameFile(system1.getUncompressedZipPath(), system2.getUncompressedZipPath())) {
      return false;
    }
    UncompressedZipPath absolutePath1 = (UncompressedZipPath)path1.toAbsolutePath();
    UncompressedZipPath absolutePath2 = (UncompressedZipPath)path2.toAbsolutePath();
    return Arrays.equals(absolutePath1.getNameElements(), absolutePath2.getNameElements());
  }

  @Override
  public boolean isHidden(Path path) {
    return false;
  }

  @Override
  public FileStore getFileStore(Path path) {
    return new UncompressedZipFileStore(((UncompressedZipFileSystem)path.getFileSystem()));
  }

  @Override
  public void checkAccess(Path path, AccessMode... modes) throws IOException {
    if (ArrayUtil.contains(AccessMode.WRITE, path) || ArrayUtil.contains(AccessMode.EXECUTE, path)) {
      throw new UnsupportedOperationException();
    }
    find(path);
  }

  @Override
  public <V extends FileAttributeView> V getFileAttributeView(Path path, Class<V> type, LinkOption... options) {
    throw new UnsupportedOperationException();
  }

  @Override
  public <A extends BasicFileAttributes> A readAttributes(Path path, Class<A> type, LinkOption... options) throws IOException {
    if (type == BasicFileAttributes.class) {
      return (A)new UncompressedZipEntryFileAttributes(((UncompressedZipPath)path));
    }
    throw new UnsupportedOperationException();
  }

  @Override
  public Map<String, Object> readAttributes(Path path, String attributes, LinkOption... options) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setAttribute(Path path, String attribute, Object value, LinkOption... options) {
    throw new UnsupportedOperationException();
  }

  @NotNull
  static UncompressedZipFileSystem.ZipTreeNode find(@NotNull Path dir) throws IOException {
    Path absoluteDir = dir.toAbsolutePath();
    String[] elements = ((UncompressedZipPath)absoluteDir).getNameElements();
    UncompressedZipFileSystem.ZipTreeNode currentElement = ((UncompressedZipFileSystem)absoluteDir.getFileSystem()).getRoot();
    for (String element : elements) {
      currentElement = currentElement.getChild(element);
      if (currentElement == null) {
        throw new NotDirectoryException(dir.toString());
      }
    }
    return currentElement;
  }
}
