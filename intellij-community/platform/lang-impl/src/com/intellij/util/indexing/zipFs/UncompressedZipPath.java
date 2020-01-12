// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing.zipFs;

import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.ArrayUtil;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.net.URI;
import java.nio.file.*;
import java.util.Iterator;
import java.util.stream.Stream;

public class UncompressedZipPath implements Path {
  @NotNull
  private final UncompressedZipFileSystem myFileSystem;
  private final String @NotNull [] myNameElements;
  private final boolean myAbsolute;

  public UncompressedZipPath(@NotNull UncompressedZipFileSystem system,
                             String @NotNull [] elements,
                             boolean absolute) {
    myFileSystem = system;
    myNameElements = elements;
    myAbsolute = absolute;
  }

  String @NotNull [] getNameElements() {
    return myNameElements;
  }

  @NotNull
  @Override
  public FileSystem getFileSystem() {
    return myFileSystem;
  }

  @Override
  public boolean isAbsolute() {
    return myAbsolute;
  }

  @Override
  public Path getRoot() {
    return isAbsolute() ? new UncompressedZipPath(myFileSystem, ArrayUtil.EMPTY_STRING_ARRAY, true) : null;
  }

  @Override
  public Path getFileName() {
    return getName(getNameCount() - 1);
  }

  @Override
  public Path getParent() {
    if (myNameElements.length == 0) {
      throw new AssertionError();
    }
    String[] parentElements = ArrayUtil.remove(myNameElements, myNameElements.length - 1, ArrayUtil.STRING_ARRAY_FACTORY);
    return new UncompressedZipPath(myFileSystem, parentElements, myAbsolute);
  }

  @Override
  public int getNameCount() {
    return myNameElements.length;
  }

  @NotNull
  @Override
  public Path getName(int index) {
    return new UncompressedZipPath(myFileSystem, new String[] {myNameElements[index]}, false);
  }

  @NotNull
  @Override
  public Path subpath(int beginIndex, int endIndex) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean startsWith(@NotNull Path other) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean startsWith(@NotNull String other) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean endsWith(@NotNull Path other) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean endsWith(@NotNull String other) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Path normalize() {
    String separator = myFileSystem.getSeparator();
    String path = FileUtil.toCanonicalPath(StringUtil.join(myNameElements, separator), separator.charAt(0));
    return new UncompressedZipPath(myFileSystem, ArrayUtil.toStringArray(StringUtil.split(path, separator)), myAbsolute);
  }

  @NotNull
  @Override
  public Path resolve(@NotNull Path other) {
    if (other.isAbsolute()) return other;
    String[] toAppend = ((UncompressedZipPath)other).myNameElements;
    return new UncompressedZipPath(myFileSystem, ArrayUtil.mergeArrays(myNameElements,toAppend), myAbsolute);
  }

  @NotNull
  @Override
  public Path resolve(@NotNull String other) {
    String[] toAppend = ArrayUtil.toStringArray(StringUtil.split(other, myFileSystem.getSeparator()));
    return new UncompressedZipPath(myFileSystem, ArrayUtil.mergeArrays(myNameElements,toAppend), myAbsolute);
  }

  @NotNull
  @Override
  public Path resolveSibling(@NotNull Path other) {
    return getParent().resolve(other);
  }

  @NotNull
  @Override
  public Path resolveSibling(@NotNull String other) {
    return getParent().resolve(other);
  }

  @NotNull
  @Override
  public Path relativize(@NotNull Path other) {
    throw new UnsupportedOperationException();
  }

  @NotNull
  @Override
  public URI toUri() {
    throw new UnsupportedOperationException();
  }

  @NotNull
  @Override
  public Path toAbsolutePath() {
    String separator = myFileSystem.getSeparator();
    String path = FileUtil.toCanonicalPath(StringUtil.join(myNameElements, separator), separator.charAt(0));
    return new UncompressedZipPath(myFileSystem, ArrayUtil.toStringArray(StringUtil.split(path, separator)), true);
  }

  @NotNull
  @Override
  public Path toRealPath(LinkOption @NotNull ... options) {
    if (!myAbsolute) {
      UncompressedZipPath absolutePath = (UncompressedZipPath)toAbsolutePath();
      return absolutePath.toRealPath(options);
    } else {
      return this;
    }
  }

  @NotNull
  @Override
  public File toFile() {
    throw new UnsupportedOperationException();
  }

  @Override
  public WatchKey register(WatchService watcher, WatchEvent.Kind<?>[] events, WatchEvent.Modifier... modifiers) {
    throw new UnsupportedOperationException();
  }

  @Override
  public WatchKey register(WatchService watcher, WatchEvent.Kind<?>... events) {
    throw new UnsupportedOperationException();
  }

  @NotNull
  @Override
  public Iterator<Path> iterator() {
    return Stream.of(myNameElements).map(n -> (Path)new UncompressedZipPath(myFileSystem, new String[] { n }, false)).iterator();
  }

  @Override
  public int compareTo(Path other) {
    return ArrayUtil.lexicographicCompare(myNameElements, ((UncompressedZipPath)other).myNameElements);
  }

  @Override
  public String toString() {
    return StringUtil.join(myNameElements, myFileSystem.getSeparator());
  }
}
