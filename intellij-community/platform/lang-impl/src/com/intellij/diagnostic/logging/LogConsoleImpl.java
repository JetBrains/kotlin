/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.intellij.diagnostic.logging;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.charset.Charset;
import java.util.Arrays;

public abstract class LogConsoleImpl extends LogConsoleBase {
  private final String myPath;
  @NotNull
  private final File myFile;
  @NotNull
  private final Charset myCharset;
  private FileSnapshot myOldSnapshot;

  public LogConsoleImpl(Project project,
                        @NotNull File file,
                        @NotNull Charset charset,
                        long skippedContents,
                        @NotNull String title,
                        final boolean buildInActions,
                        final GlobalSearchScope searchScope) {
    super(project, getReader(file, charset, skippedContents), title, buildInActions, new DefaultLogFilterModel(project),
          searchScope);
    myPath = file.getAbsolutePath();
    myFile = file;
    myCharset = charset;
    myOldSnapshot = new FileSnapshot();
  }

  @Nullable
  private static Reader getReader(@NotNull File file, @NotNull Charset charset, long skippedContents) {
    try {
      try {
        @SuppressWarnings("IOResourceOpenedButNotSafelyClosed")
        FileInputStream inputStream = new FileInputStream(file);
        //do not skip forward
        if (file.length() >= skippedContents) {
          long skipped = 0;
          while (skipped < skippedContents) {
            skipped += inputStream.skip(skippedContents - skipped);
          }
        }
        return new BufferedReader(new InputStreamReader(inputStream, charset));
      }
      catch (FileNotFoundException ignored) {
        if (FileUtilRt.createIfNotExists(file)) {
          return new BufferedReader(new InputStreamReader(new FileInputStream(file), charset));
        }
        return null;
      }
    }
    catch (Throwable ignored) {
      return null;
    }
  }

  @Override
  @Nullable
  public String getTooltip() {
    return myPath;
  }

  public String getPath() {
    return myPath;
  }

  @Nullable
  @Override
  protected BufferedReader updateReaderIfNeeded(@Nullable BufferedReader reader) throws IOException {
    if (reader == null) {
      return null;
    }

    FileSnapshot snapshot = new FileSnapshot();
    if (myOldSnapshot.rolloverDetected(snapshot)) {
      reader.close();
      //noinspection IOResourceOpenedButNotSafelyClosed
      reader = new BufferedReader(new InputStreamReader(new FileInputStream(myFile), myCharset));
    }
    myOldSnapshot = snapshot;
    return reader;
  }

  private class FileSnapshot {
    final long length;
    final byte[] firstBytes;

    FileSnapshot() {
      this.length = myFile.length();

      byte[] bytes = new byte[20];
      try (FileInputStream stream = new FileInputStream(myFile)) {
        //noinspection ResultOfMethodCallIgnored
        stream.read(bytes);
      }
      catch (IOException ignore) {
      }
      this.firstBytes = bytes;
    }

    boolean rolloverDetected(FileSnapshot current) {
      return current.length < length || !Arrays.equals(firstBytes, current.firstBytes);
    }
  }
}
