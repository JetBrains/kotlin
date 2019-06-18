// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.compiler.backwardRefs;

import com.intellij.compiler.server.BuildManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileWithId;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.Queue;
import com.intellij.util.indexing.InvertedIndexUtil;
import com.intellij.util.indexing.StorageException;
import gnu.trove.THashSet;
import gnu.trove.TIntHashSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.backwardRefs.CompilerRef;
import org.jetbrains.jps.backwardRefs.JavaCompilerBackwardReferenceIndex;
import org.jetbrains.jps.backwardRefs.SignatureData;
import org.jetbrains.jps.backwardRefs.index.CompilerReferenceIndex;
import org.jetbrains.jps.backwardRefs.index.JavaCompilerIndices;
import org.jetbrains.jps.incremental.relativizer.PathRelativizerService;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class JavaBackwardReferenceIndexReaderFactory implements CompilerReferenceReaderFactory<JavaBackwardReferenceIndexReaderFactory.BackwardReferenceReader> {
  public static final JavaBackwardReferenceIndexReaderFactory INSTANCE = new JavaBackwardReferenceIndexReaderFactory();

  private static final Logger LOG = Logger.getInstance(JavaBackwardReferenceIndexReaderFactory.class);

  @Override
  public int expectedIndexVersion() {
    return JavaCompilerIndices.VERSION;
  }

  @Override
  @Nullable
  public BackwardReferenceReader create(Project project) {
    File buildDir = BuildManager.getInstance().getProjectSystemDirectory(project);

    if (buildDir == null
        || !CompilerReferenceIndex.exists(buildDir)
        || CompilerReferenceIndex.versionDiffers(buildDir, expectedIndexVersion())) {
      return null;
    }

    try {
      return new BackwardReferenceReader(project, buildDir);
    }
    catch (RuntimeException e) {
      LOG.error("An exception while initialization of compiler reference index.", e);
      return null;
    }
  }

  public static class BackwardReferenceReader extends CompilerReferenceReader<JavaCompilerBackwardReferenceIndex> {
    protected BackwardReferenceReader(Project project, File buildDir) {
      super(buildDir, new JavaCompilerBackwardReferenceIndex(buildDir, new PathRelativizerService(project.getBasePath(), buildDir.getPath()), true));
    }

    @Override
    @Nullable
    public TIntHashSet findReferentFileIds(@NotNull CompilerRef ref, boolean checkBaseClassAmbiguity) throws StorageException {
      CompilerRef.NamedCompilerRef[] hierarchy;
      if (ref instanceof CompilerRef.CompilerClassHierarchyElementDef) {
        hierarchy = new CompilerRef.NamedCompilerRef[]{(CompilerRef.NamedCompilerRef)ref};
      }
      else {
        CompilerRef.CompilerClassHierarchyElementDef hierarchyElement = ((CompilerRef.CompilerMember)ref).getOwner();
        hierarchy = getHierarchy(hierarchyElement, checkBaseClassAmbiguity, false, -1);
      }
      if (hierarchy == null) return null;
      TIntHashSet set = new TIntHashSet();
      for (CompilerRef.NamedCompilerRef aClass : hierarchy) {
        final CompilerRef overriderUsage = ref.override(aClass.getName());
        addUsages(overriderUsage, set);
      }
      return set;
    }

    @Override
    @Nullable
    public TIntHashSet findFileIdsWithImplicitToString(@NotNull CompilerRef ref) throws StorageException {
      TIntHashSet result = new TIntHashSet();
      myIndex.get(JavaCompilerIndices.IMPLICIT_TO_STRING).getData(ref).forEach(
        (id, value) -> {
          final VirtualFile file = findFile(id);
          if (file != null) {
            result.add(((VirtualFileWithId)file).getId());
          }
          return true;
        });
      return result;
    }

    /**
     * @return two maps of classes grouped per file
     * <p>
     * 1st map: inheritors. Can be used without explicit inheritance verification
     * 2nd map: candidates. One need to check that these classes are really direct inheritors
     */
    @Override
    @NotNull
    public Map<VirtualFile, SearchId[]> getDirectInheritors(@NotNull CompilerRef searchElement,
                                                            @NotNull GlobalSearchScope searchScope,
                                                            @NotNull GlobalSearchScope dirtyScope,
                                                            @NotNull FileType fileType,
                                                            @NotNull CompilerHierarchySearchType searchType) throws StorageException {
      GlobalSearchScope effectiveSearchScope = GlobalSearchScope.notScope(dirtyScope).intersectWith(searchScope);
      LanguageCompilerRefAdapter adapter = LanguageCompilerRefAdapter.findAdapter(fileType);
      LOG.assertTrue(adapter != null, "adapter is null for file type: " + fileType);
      Class<? extends CompilerRef> requiredCompilerRefClass = searchType.getRequiredClass(adapter);

      Map<VirtualFile, SearchId[]> candidatesPerFile = new HashMap<>();
      myIndex.get(JavaCompilerIndices.BACK_HIERARCHY).getData(searchElement).forEach((fileId, defs) -> {
        final List<CompilerRef> requiredCandidates = ContainerUtil.filter(defs, requiredCompilerRefClass::isInstance);
        if (requiredCandidates.isEmpty()) return true;
        final VirtualFile file = findFile(fileId);
        if (file != null && effectiveSearchScope.contains(file)) {
          candidatesPerFile.put(file, searchType.convertToIds(requiredCandidates, myIndex.getByteSeqEum()));
        }
        return true;
      });
      return candidatesPerFile.isEmpty() ? Collections.emptyMap() : candidatesPerFile;
    }

    @Override
    @Nullable
    public Integer getAnonymousCount(@NotNull CompilerRef.CompilerClassHierarchyElementDef classDef, boolean checkDefinitions) {
      try {
        if (checkDefinitions && getDefinitionCount(classDef) != DefCount.ONE) {
          return null;
        }
        final int[] count = {0};
        myIndex.get(JavaCompilerIndices.BACK_HIERARCHY).getData(classDef).forEach((id, value) -> {
          count[0] += value.size();
          return true;
        });
        return count[0];
      }
      catch (StorageException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public int getOccurrenceCount(@NotNull CompilerRef element) {
      try {
        int[] result = new int[]{0};
        myIndex.get(JavaCompilerIndices.BACK_USAGES).getData(element).forEach(
          (id, value) -> {
            result[0] += value;
            return true;
          });
        return result[0];
      }
      catch (StorageException e) {
        throw new RuntimeException(e);
      }
    }

    @NotNull
    List<CompilerRef> getMembersFor(@NotNull SignatureData data) {
      try {
        List<CompilerRef> result = new ArrayList<>();
        myIndex.get(JavaCompilerIndices.BACK_MEMBER_SIGN).getData(data).forEach((id, refs) -> {
          result.addAll(refs);
          return true;
        });
        return result;
      }
      catch (StorageException e) {
        throw new RuntimeException(e);
      }
    }

    @NotNull
    TIntHashSet getAllContainingFileIds(@NotNull CompilerRef ref) throws StorageException {
      return InvertedIndexUtil
        .collectInputIdsContainingAllKeys(myIndex.get(JavaCompilerIndices.BACK_USAGES), Collections.singletonList(ref), null, null, null);
    }

    @NotNull
    OccurrenceCounter<CompilerRef> getTypeCastOperands(@NotNull CompilerRef.CompilerClassHierarchyElementDef castType, @Nullable TIntHashSet fileIds)
      throws StorageException {
      OccurrenceCounter<CompilerRef> result = new OccurrenceCounter<>();
      myIndex.get(JavaCompilerIndices.BACK_CAST).getData(castType).forEach((id, values) -> {
        if (fileIds != null && !fileIds.contains(id)) return true;
        for (CompilerRef ref : values) {
          result.add(ref);
        }
        return true;
      });
      return result;
    }

    private void addUsages(CompilerRef usage, TIntHashSet sink) throws StorageException {
      myIndex.get(JavaCompilerIndices.BACK_USAGES).getData(usage).forEach(
        (id, value) -> {
          final VirtualFile file = findFile(id);
          if (file != null) {
            sink.add(((VirtualFileWithId)file).getId());
          }
          return true;
        });
    }

    private VirtualFile findFile(int id) {
      try {
        String path = myIndex.getFilePathEnumerator().valueOf(id);
        assert path != null;
        return VfsUtil.findFileByIoFile(new File(path), false);
      }
      catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    @Nullable("return null if the class hierarchy contains ambiguous qualified names")
    public CompilerRef.CompilerClassHierarchyElementDef[] getHierarchy(CompilerRef.CompilerClassHierarchyElementDef hierarchyElement,
                                                                       boolean checkBaseClassAmbiguity,
                                                                       boolean includeAnonymous,
                                                                       int interruptNumber) {
      try {
        Set<CompilerRef.CompilerClassHierarchyElementDef> result = new THashSet<>();
        Queue<CompilerRef.CompilerClassHierarchyElementDef> q = new Queue<>(10);
        q.addLast(hierarchyElement);
        while (!q.isEmpty()) {
          CompilerRef.CompilerClassHierarchyElementDef curClass = q.pullFirst();
          if (interruptNumber != -1 && result.size() > interruptNumber) {
            break;
          }
          if (result.add(curClass)) {
            if (result.size() % 100 == 0) {
              ProgressManager.checkCanceled();
            }

            if (!(curClass instanceof CompilerRef.CompilerAnonymousClassDef) && (checkBaseClassAmbiguity || curClass != hierarchyElement)) {
              if (hasMultipleDefinitions(curClass)) {
                return null;
              }
            }
            myIndex.get(JavaCompilerIndices.BACK_HIERARCHY).getData(curClass).forEach((id, children) -> {
              for (CompilerRef child : children) {
                if (child instanceof CompilerRef.CompilerClassHierarchyElementDef &&
                    (includeAnonymous || !(child instanceof CompilerRef.CompilerAnonymousClassDef))) {
                  q.addLast((CompilerRef.CompilerClassHierarchyElementDef)child);
                }
              }
              return true;
            });
          }
        }
        return result.toArray(CompilerRef.CompilerClassHierarchyElementDef.EMPTY_ARRAY);
      }
      catch (StorageException e) {
        throw new RuntimeException(e);
      }
    }

    @NotNull
    CompilerRef.CompilerClassHierarchyElementDef[] getDirectInheritors(CompilerRef.CompilerClassHierarchyElementDef hierarchyElement)
      throws StorageException {
      Set<CompilerRef.CompilerClassHierarchyElementDef> result = new THashSet<>();
      myIndex.get(JavaCompilerIndices.BACK_HIERARCHY).getData(hierarchyElement).forEach((id, children) -> {
        for (CompilerRef child : children) {
          if (child instanceof CompilerRef.CompilerClassHierarchyElementDef && !(child instanceof CompilerRef.CompilerAnonymousClassDef)) {
            result.add((CompilerRef.CompilerClassHierarchyElementDef)child);
          }
        }
        return true;
      });
      return result.toArray(CompilerRef.CompilerClassHierarchyElementDef.EMPTY_ARRAY);
    }

    private enum DefCount {NONE, ONE, MANY}

    private boolean hasMultipleDefinitions(CompilerRef.NamedCompilerRef def) throws StorageException {
      DefCount count = getDefinitionCount(def);
      if (count == DefCount.NONE) {
        //diagnostic
        String name =
          def instanceof CompilerRef.CompilerAnonymousClassDef ? String.valueOf(def.getName()) : getNameEnumerator().getName(def.getName());
        LOG.error("Can't get definition files for: " + name + ", class: " + def.getClass());
      }
      return count == DefCount.MANY;
    }

    @NotNull
    private DefCount getDefinitionCount(CompilerRef.NamedCompilerRef def) throws StorageException {
      DefCount[] result = new DefCount[]{DefCount.NONE};
      myIndex.get(JavaCompilerIndices.BACK_CLASS_DEF).getData(def).forEach((id, value) -> {
        if (result[0] == DefCount.NONE) {
          result[0] = DefCount.ONE;
          return true;
        }
        if (result[0] == DefCount.ONE) {
          result[0] = DefCount.MANY;
          return true;
        }
        return false;
      });
      return result[0];
    }
  }
}
