// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.semantic;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.extensions.ExtensionNotApplicableException;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.RecursionGuard;
import com.intellij.openapi.util.RecursionManager;
import com.intellij.openapi.util.UserDataHolder;
import com.intellij.patterns.ElementPattern;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiModificationTracker;
import com.intellij.util.NullableFunction;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.IntObjectMap;
import com.intellij.util.containers.MultiMap;
import gnu.trove.THashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * @author peter
 */
@SuppressWarnings({"unchecked"})
public final class SemServiceImpl extends SemService {
  private static final Logger LOG = Logger.getInstance(SemServiceImpl.class);

  private volatile MultiMap<SemKey<?>, NullableFunction<PsiElement, Collection<? extends SemElement>>> myProducers;
  private final Project myProject;
  private final CachedValuesManager myCVManager;

  public SemServiceImpl(Project project) {
    myProject = project;
    myCVManager = CachedValuesManager.getManager(myProject);
  }

  private MultiMap<SemKey<?>, NullableFunction<PsiElement, Collection<? extends SemElement>>> collectProducers() {
    MultiMap<SemKey<?>, NullableFunction<PsiElement, Collection<? extends SemElement>>> map = MultiMap.createSmart();

    final SemRegistrar registrar = new SemRegistrar() {
      @Override
      public <T extends SemElement, V extends PsiElement> void registerSemElementProvider(SemKey<T> key,
                                                                                          final ElementPattern<? extends V> place,
                                                                                          final NullableFunction<? super V, ? extends T> provider) {
        map.putValue(key, element -> {
          if (place.accepts(element)) {
            return Collections.singleton(provider.fun((V)element));
          }
          return null;
        });
      }

      @Override
      public <T extends SemElement, V extends PsiElement> void registerRepeatableSemElementProvider(SemKey<T> key,
                                                                                                    ElementPattern<? extends V> place,
                                                                                                    NullableFunction<? super V, ? extends Collection<T>> provider) {
        map.putValue(key, element -> {
          if (place.accepts(element)) {
            return provider.fun((V)element);
          }
          return null;
        });
      }
    };

    for (SemContributorEP contributor : SemContributor.EP_NAME.getExtensionList()) {
      SemContributor semContributor;
      try {
        semContributor = myProject.instantiateExtensionWithPicoContainerOnlyIfNeeded(contributor.implementation, contributor.getPluginDescriptor());
      }
      catch (ProcessCanceledException e) {
        throw e;
      }
      catch (ExtensionNotApplicableException e) {
        continue;
      }
      catch (Exception e) {
        LOG.error(e);
        continue;
      }
      semContributor.registerSemProviders(registrar, myProject);
    }

    return map;
  }

  @Override
  public <T extends SemElement> List<T> getSemElements(@NotNull SemKey<T> key, @NotNull final PsiElement psi) {
    SemCacheChunk chunk = myCVManager.getCachedValue((UserDataHolder)psi, () ->
      CachedValueProvider.Result.create(new SemCacheChunk(), PsiModificationTracker.MODIFICATION_COUNT));
    List<T> cached = findCached(key, chunk);
    return cached != null ? cached : createSemElements(key, psi, chunk);
  }

  @NotNull
  private <T extends SemElement> List<T> createSemElements(@NotNull SemKey<T> key,
                                                           @NotNull PsiElement psi,
                                                           SemCacheChunk chunk) {
    ensureInitialized();

    RecursionGuard.StackStamp stamp = RecursionManager.markStack();

    LinkedHashSet<T> result = new LinkedHashSet<>();
    Map<SemKey<?>, List<SemElement>> map = new THashMap<>();
    for (SemKey<?> each : key.getInheritors()) {
      List<SemElement> list = createSemElements(each, psi);
      map.put(each, list);
      result.addAll((List<T>)list);
    }

    if (stamp.mayCacheNow()) {
      for (SemKey<?> semKey : map.keySet()) {
        chunk.putSemElements(semKey, map.get(semKey));
      }
    }

    return new ArrayList<>(result);
  }

  private void ensureInitialized() {
    if (myProducers == null) {
      myProducers = collectProducers();
    }
  }

  @NotNull
  private List<SemElement> createSemElements(SemKey<?> key, PsiElement psi) {
    List<SemElement> result = null;
    Collection<NullableFunction<PsiElement, Collection<? extends SemElement>>> functions = myProducers.get(key);
    if (!functions.isEmpty()) {
      for (final NullableFunction<PsiElement, Collection<? extends SemElement>> producer : functions) {
        Collection<? extends SemElement> elements = producer.fun(psi);
        if (elements != null) {
          if (result == null) result = new SmartList<>();
          ContainerUtil.addAllNotNull(result, elements);
        }
      }
    }
    return result == null ? Collections.emptyList() : Collections.unmodifiableList(result);
  }

  @Nullable
  private static <T extends SemElement> List<T> findCached(SemKey<T> key, SemCacheChunk chunk) {
    List<T> singleList = null;
    LinkedHashSet<T> result = null;
    List<SemKey<?>> inheritors = key.getInheritors();
    //noinspection ForLoopReplaceableByForEach
    for (int i = 0; i < inheritors.size(); i++) {
      List<T> cached = (List<T>)chunk.getSemElements(inheritors.get(i));
      if (cached == null) {
        return null;
      }

      if (cached != Collections.<T>emptyList()) {
        if (singleList == null) {
          singleList = cached;
          continue;
        }

        if (result == null) {
          result = new LinkedHashSet<>(singleList);
        }
        result.addAll(cached);
      }
    }


    if (result == null) {
      if (singleList != null) {
        return singleList;
      }

      return Collections.emptyList();
    }

    return new ArrayList<>(result);
  }

  private static class SemCacheChunk {
    private final IntObjectMap<List<SemElement>> map = ContainerUtil.createConcurrentIntObjectMap();

    List<SemElement> getSemElements(SemKey<?> key) {
      return map.get(key.getUniqueId());
    }

    void putSemElements(SemKey<?> key, List<SemElement> elements) {
      map.put(key.getUniqueId(), elements);
    }

  }

}
