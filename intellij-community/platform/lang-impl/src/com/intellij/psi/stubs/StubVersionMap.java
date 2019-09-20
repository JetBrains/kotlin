// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.psi.stubs;

import com.intellij.diagnostic.PluginException;
import com.intellij.lang.Language;
import com.intellij.lang.LanguageParserDefinitions;
import com.intellij.lang.ParserDefinition;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeRegistry;
import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.FileAttribute;
import com.intellij.openapi.vfs.newvfs.persistent.FSRecords;
import com.intellij.psi.templateLanguages.TemplateLanguage;
import com.intellij.psi.tree.IFileElementType;
import com.intellij.psi.tree.IStubFileElementType;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.indexing.IndexInfrastructure;
import com.intellij.util.indexing.IndexingStamp;
import com.intellij.util.io.DataInputOutputUtil;
import gnu.trove.THashMap;
import gnu.trove.THashSet;
import gnu.trove.TLongObjectHashMap;
import gnu.trove.TObjectLongHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

class StubVersionMap {
  private static final String INDEXED_FILETYPES = "indexed_filetypes";
  private static final String RECORD_SEPARATOR = "\uFFFF";
  private static final String LINE_SEPARATOR = "\n";
  private static final Charset ourEncoding = StandardCharsets.UTF_8;
  private static final Logger LOG = Logger.getInstance(StubVersionMap.class);
  private final Map<FileType, Object> fileTypeToVersionOwner = new THashMap<>();
  private final TObjectLongHashMap<FileType> fileTypeToVersion = new TObjectLongHashMap<>();
  private final TLongObjectHashMap<FileType> versionToFileType = new TLongObjectHashMap<>();
  private long myStubIndexStamp;

  StubVersionMap() throws IOException {
    for (final FileType fileType : FileTypeRegistry.getInstance().getRegisteredFileTypes()) {
      Object owner = getVersionOwner(fileType);
      if (owner != null) {
        fileTypeToVersionOwner.put(fileType, owner);
      }
    }

    updateState();
  }

  private void updateState() throws IOException {
    final long currentStubIndexStamp = IndexingStamp.getIndexCreationStamp(StubUpdatingIndex.INDEX_ID);
    File allIndexedFiles = allIndexedFilesRegistryFile();

    List<String> removedFileTypes = new ArrayList<>();
    List<FileType> updatedFileTypes = new ArrayList<>();
    List<FileType> addedFileTypes = new ArrayList<>();
    long lastUsedCounter = currentStubIndexStamp;

    boolean canUsePreviousMappings = allIndexedFiles.exists();
    FileTypeRegistry fileTypeRegistry = FileTypeRegistry.getInstance();
    Set<FileType> loadedFileTypes = new THashSet<>();

    if (canUsePreviousMappings) {
      List<String> stringList = StringUtil.split(FileUtil.loadFile(allIndexedFiles, ourEncoding), LINE_SEPARATOR);
      long allIndexedFilesVersion = Long.parseLong(stringList.get(0));

      if (allIndexedFilesVersion == currentStubIndexStamp) {
        for (int i = 1, size = stringList.size(); i < size; ++i) {
          List<String> strings = StringUtil.split(stringList.get(i), RECORD_SEPARATOR);
          String fileTypeName = strings.get(0);
          long usedTimeStamp = Long.parseLong(strings.get(2));
          lastUsedCounter = Math.min(lastUsedCounter, usedTimeStamp);

          FileType fileType = fileTypeRegistry.findFileTypeByName(fileTypeName);
          if (fileType == null) removedFileTypes.add(fileTypeName);
          else {
            loadedFileTypes.add(fileType);
            Object owner = getVersionOwner(fileType);
            if (owner == null) removedFileTypes.add(fileTypeName);
            else {
              if (!Comparing.equal(strings.get(1), typeAndVersion(owner))) {
                updatedFileTypes.add(fileType);
              }
              else {
                registerStamp(fileType, usedTimeStamp);
              }
            }
          }
        }
      } else {
        canUsePreviousMappings = false;
      }
    }

    for(FileType fileType:fileTypeToVersionOwner.keySet()) {
      if (!loadedFileTypes.contains(fileType)) {
        addedFileTypes.add(fileType);
      }
    }

    if (canUsePreviousMappings && (!addedFileTypes.isEmpty() || !removedFileTypes.isEmpty())) {
      StubUpdatingIndex.LOG.info("requesting complete stub index rebuild due to changes: " +
                                 (addedFileTypes.isEmpty() ? "" : "added file types:" + StringUtil.join(addedFileTypes, FileType::getName, ",") + ";") +
                                 (removedFileTypes.isEmpty() ? "":"removed file types:" + StringUtil.join(removedFileTypes, ",")));
      throw new IOException(); // StubVersionMap will be recreated
    }

    long counter = lastUsedCounter - 1; // important to start with value smaller and progress downwards
    for(FileType fileType: ContainerUtil.concat(updatedFileTypes, addedFileTypes)) {
      while (versionToFileType.containsKey(counter)) --counter;
      registerStamp(fileType, counter);
    }

    if (!addedFileTypes.isEmpty() || !updatedFileTypes.isEmpty() || !removedFileTypes.isEmpty()) {
      if (!addedFileTypes.isEmpty()) {
        StubUpdatingIndex.LOG.info("Following new file types will be indexed:" + StringUtil.join(addedFileTypes, FileType::getName, ","));
      }

      if (!updatedFileTypes.isEmpty()) {
        StubUpdatingIndex.LOG.info("Stub version was changed for " + StringUtil.join(updatedFileTypes, FileType::getName, ","));
      }

      if (!removedFileTypes.isEmpty()) {
        StubUpdatingIndex.LOG.info("Following file types will not be indexed:" + StringUtil.join(removedFileTypes, ","));
      }

      StringBuilder allFileTypes = new StringBuilder();
      allFileTypes.append(currentStubIndexStamp).append(LINE_SEPARATOR);

      for (FileType fileType : fileTypeToVersionOwner.keySet()) {
        Object owner = fileTypeToVersionOwner.get(fileType);
        long timestamp = fileTypeToVersion.get(fileType);
        allFileTypes.append(fileType.getName()).append(RECORD_SEPARATOR).append(typeAndVersion(owner)).append(RECORD_SEPARATOR)
          .append(timestamp).append(LINE_SEPARATOR);
      }
      FileUtil.writeToFile(allIndexedFiles, allFileTypes.toString().getBytes(ourEncoding));
    }

    myStubIndexStamp = currentStubIndexStamp;
  }

  private void registerStamp(FileType fileTypeByName, long stamp) {
    fileTypeToVersion.put(fileTypeByName, stamp);
    FileType previousType = versionToFileType.put(stamp, fileTypeByName);
    if (previousType != null) {
      assert false;
    }
  }

  private static Object getVersionOwner(FileType fileType) {
    Object owner = null;
    if (fileType instanceof LanguageFileType) {
      Language l = ((LanguageFileType)fileType).getLanguage();
      ParserDefinition parserDefinition = LanguageParserDefinitions.INSTANCE.forLanguage(l);
      if (parserDefinition != null) {
        final IFileElementType type = parserDefinition.getFileNodeType();
        if (type instanceof IStubFileElementType) {
          owner = type;
        }
      }
    }

    BinaryFileStubBuilder builder = BinaryFileStubBuilders.INSTANCE.forFileType(fileType);
    if (builder != null) {
      owner = builder;
    }
    return owner;
  }

  public long getStamp(FileType type) {
    return fileTypeToVersion.get(type);
  }

  void clear() {
    fileTypeToVersion.clear();
    versionToFileType.clear();
    try {
      updateState();
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  @NotNull
  private static File allIndexedFilesRegistryFile() {
    return new File(new File(IndexInfrastructure.getIndexRootDir(StubUpdatingIndex.INDEX_ID), ".fileTypes"), INDEXED_FILETYPES);
  }

  @NotNull
  private static String typeAndVersion(Object owner) {
    return info(owner) + "," + version(owner);
  }

  private static String info(Object owner) {
    if (owner instanceof IStubFileElementType) {
      return "stub:" + owner.getClass().getName();
    } else {
      return "binary stub builder:" + owner.getClass().getName();
    }
  }

  private static int version(Object owner) {
    if (owner instanceof IStubFileElementType) {
      IStubFileElementType elementType = (IStubFileElementType)owner;
      if (elementType.getLanguage() instanceof TemplateLanguage &&
          elementType.getStubVersion() < IStubFileElementType.getTemplateStubVersion()) {
        PluginException.logPluginError(LOG, elementType.getLanguage() + " stub version should call super.getStubVersion()",
                                       null, elementType.getClass());
      }
      return elementType.getStubVersion();
    } else {
      return ((BinaryFileStubBuilder)owner).getStubVersion();
    }
  }

  private int getIndexingTimestampDiffForFileType(FileType type) {
    return (int)(myStubIndexStamp - fileTypeToVersion.get(type));
  }

  @Nullable
  private FileType getFileTypeByIndexingTimestampDiff(int diff) {
    return versionToFileType.get(myStubIndexStamp - diff);
  }

  private static final FileAttribute VERSION_STAMP = new FileAttribute("stubIndex.versionStamp", 2, true);
  public void persistIndexedState(int fileId, @NotNull VirtualFile file) throws IOException {
    try (DataOutputStream stream = FSRecords.writeAttribute(fileId, VERSION_STAMP)) {
      FileType[] type = {null};
      ProgressManager.getInstance().executeNonCancelableSection(() -> {
        type[0] = file.getFileType();
      });
      DataInputOutputUtil.writeINT(stream, getIndexingTimestampDiffForFileType(type[0]));
    }
  }

  public boolean isIndexed(int fileId, @NotNull VirtualFile file) throws IOException {
    DataInputStream stream = FSRecords.readAttributeWithLock(fileId, VERSION_STAMP);
    int diff = stream != null ? DataInputOutputUtil.readINT(stream) : 0;
    if (diff == 0) return false;
    FileType fileType = getFileTypeByIndexingTimestampDiff(diff);
    return fileType != null && getStamp(file.getFileType()) == getStamp(fileType);
  }
}