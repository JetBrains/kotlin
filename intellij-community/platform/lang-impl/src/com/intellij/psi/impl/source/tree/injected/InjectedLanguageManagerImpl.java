// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.psi.impl.source.tree.injected;

import com.intellij.ide.plugins.DynamicPluginListener;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.injected.editor.DocumentWindow;
import com.intellij.injected.editor.VirtualFileWindow;
import com.intellij.lang.injection.InjectedLanguageManager;
import com.intellij.lang.injection.MultiHostInjector;
import com.intellij.lang.injection.MultiHostRegistrar;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.ex.DocumentEx;
import com.intellij.openapi.extensions.ExtensionPointListener;
import com.intellij.openapi.extensions.PluginDescriptor;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.Segment;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.impl.PsiDocumentManagerBase;
import com.intellij.psi.impl.source.resolve.FileContextUtil;
import com.intellij.util.ArrayUtil;
import com.intellij.util.Processor;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.util.*;

/**
 * @author cdr
 */
@SuppressWarnings("deprecation")
public class InjectedLanguageManagerImpl extends InjectedLanguageManager implements Disposable {
  private static final Logger LOG = Logger.getInstance(InjectedLanguageManagerImpl.class);
  @SuppressWarnings("RedundantStringConstructorCall")
  static final Object ourInjectionPsiLock = new String("injectionPsiLock");
  private final Project myProject;
  private final DumbService myDumbService;
  private final PsiDocumentManager myDocManager;

  public static InjectedLanguageManagerImpl getInstanceImpl(Project project) {
    return (InjectedLanguageManagerImpl)InjectedLanguageManager.getInstance(project);
  }

  public InjectedLanguageManagerImpl(Project project) {
    myProject = project;
    myDumbService = DumbService.getInstance(myProject);
    myDocManager = PsiDocumentManager.getInstance(project);

    MultiHostInjector.MULTIHOST_INJECTOR_EP_NAME.getPoint(project).addExtensionPointListener(new ExtensionPointListener<MultiHostInjector>() {
      @Override
      public void extensionAdded(@NotNull MultiHostInjector injector, @NotNull PluginDescriptor pluginDescriptor) {
        clearInjectorCache();
      }

      @Override
      public void extensionRemoved(@NotNull MultiHostInjector injector, @NotNull PluginDescriptor pluginDescriptor) {
        clearInjectorCache();
      }
    }, false, this);

    LanguageInjector.EXTENSION_POINT_NAME.addExtensionPointListener(new ExtensionPointListener<LanguageInjector>() {
      @Override
      public void extensionAdded(@NotNull LanguageInjector extension, @NotNull PluginDescriptor pluginDescriptor) {
        clearInjectorCache();
      }

      @Override
      public void extensionRemoved(@NotNull LanguageInjector extension, @NotNull PluginDescriptor pluginDescriptor) {
        clearInjectorCache();
      }
    }, this);

    project.getMessageBus().connect(this).subscribe(DynamicPluginListener.TOPIC, new DynamicPluginListener() {
      @Override
      public void beforePluginUnload(@NotNull IdeaPluginDescriptor pluginDescriptor) {
        // When a language plugin is unloaded, make sure we don't have references to any injection host PSI from this language
        // in the injector cache
        clearInjectorCache();
      }
    });
  }

  @Override
  public void dispose() {
    disposeInvalidEditors();
  }

  public static void disposeInvalidEditors() {
    EditorWindowImpl.disposeInvalidEditors();
  }

  @Override
  public PsiLanguageInjectionHost getInjectionHost(@NotNull FileViewProvider injectedProvider) {
    if (!(injectedProvider instanceof InjectedFileViewProvider)) return null;
    return ((InjectedFileViewProvider)injectedProvider).getShreds().getHostPointer().getElement();
  }

  @Override
  public PsiLanguageInjectionHost getInjectionHost(@NotNull PsiElement injectedElement) {
    final PsiFile file = injectedElement.getContainingFile();
    final VirtualFile virtualFile = file == null ? null : file.getVirtualFile();
    if (virtualFile instanceof VirtualFileWindow) {
      PsiElement host = FileContextUtil.getFileContext(file); // use utility method in case the file's overridden getContext()
      if (host instanceof PsiLanguageInjectionHost) {
        return (PsiLanguageInjectionHost)host;
      }
    }
    return null;
  }

  @Override
  @NotNull
  public TextRange injectedToHost(@NotNull PsiElement injectedContext, @NotNull TextRange injectedTextRange) {
    DocumentWindow documentWindow = getDocumentWindow(injectedContext);
    return documentWindow == null ? injectedTextRange : documentWindow.injectedToHost(injectedTextRange);
  }

  @Override
  public int injectedToHost(@NotNull PsiElement element, int offset) {
    DocumentWindow documentWindow = getDocumentWindow(element);
    return documentWindow == null ? offset : documentWindow.injectedToHost(offset);
  }

  @Override
  public int injectedToHost(@NotNull PsiElement injectedContext, int injectedOffset, boolean minHostOffset) {
    DocumentWindow documentWindow = getDocumentWindow(injectedContext);
    return documentWindow == null ? injectedOffset : documentWindow.injectedToHost(injectedOffset, minHostOffset);
  }

  private static DocumentWindow getDocumentWindow(@NotNull PsiElement element) {
    PsiFile file = element.getContainingFile();
    if (file == null) return null;
    Document document = PsiDocumentManager.getInstance(file.getProject()).getCachedDocument(file);
    return !(document instanceof DocumentWindow) ? null : (DocumentWindow)document;
  }

  // used only from tests => no need for complex synchronization
  private final Set<MultiHostInjector> myManualInjectors = Collections.synchronizedSet(new LinkedHashSet<>());
  private volatile ClassMapCachingNulls<MultiHostInjector> cachedInjectors;

  public void processInjectableElements(@NotNull Collection<? extends PsiElement> in, @NotNull Processor<? super PsiElement> processor) {
    ClassMapCachingNulls<MultiHostInjector> map = getInjectorMap();
    for (PsiElement element : in) {
      if (map.get(element.getClass()) != null) {
        processor.process(element);
      }
    }
  }

  @NotNull
  private ClassMapCachingNulls<MultiHostInjector> getInjectorMap() {
    ClassMapCachingNulls<MultiHostInjector> cached = cachedInjectors;
    if (cached != null) {
      return cached;
    }

    ClassMapCachingNulls<MultiHostInjector> result = calcInjectorMap();
    cachedInjectors = result;
    return result;
  }

  @NotNull
  private ClassMapCachingNulls<MultiHostInjector> calcInjectorMap() {
    Map<Class<?>, MultiHostInjector[]> injectors = new HashMap<>();

    List<MultiHostInjector> allInjectors = new ArrayList<>(myManualInjectors);
    Collections.addAll(allInjectors, MultiHostInjector.MULTIHOST_INJECTOR_EP_NAME.getExtensions(myProject));
    if (LanguageInjector.EXTENSION_POINT_NAME.hasAnyExtensions()) {
      allInjectors.add(PsiManagerRegisteredInjectorsAdapter.INSTANCE);
    }

    for (MultiHostInjector injector : allInjectors) {
      for (Class<? extends PsiElement> place : injector.elementsToInjectIn()) {
        LOG.assertTrue(place != null, injector);
        MultiHostInjector[] existing = injectors.get(place);
        injectors.put(place, existing == null ? new MultiHostInjector[]{injector} : ArrayUtil.append(existing, injector));
      }
    }

    return new ClassMapCachingNulls<>(injectors, new MultiHostInjector[0], allInjectors);
  }

  private void clearInjectorCache() {
    cachedInjectors = null;
  }

  @Override
  public void registerMultiHostInjector(@NotNull MultiHostInjector injector) {
    myManualInjectors.add(injector);
    clearInjectorCache();
  }

  @Override
  public void registerMultiHostInjector(@NotNull MultiHostInjector injector, @NotNull Disposable parentDisposable) {
    registerMultiHostInjector(injector);
    Disposer.register(parentDisposable, () -> unregisterMultiHostInjector(injector));
  }

  private void unregisterMultiHostInjector(@NotNull MultiHostInjector injector) {
    try {
      myManualInjectors.remove(injector);
    }
    finally {
      clearInjectorCache();
    }
  }


  @NotNull
  @Override
  public String getUnescapedText(@NotNull final PsiElement injectedNode) {
    final String leafText = InjectedLanguageUtil.getUnescapedLeafText(injectedNode, false);
    if (leafText != null) {
      return leafText; // optimization
    }
    final StringBuilder text = new StringBuilder(injectedNode.getTextLength());
    // gather text from (patched) leaves
    injectedNode.accept(new PsiRecursiveElementWalkingVisitor() {
      @Override
      public void visitElement(PsiElement element) {
        String leafText = InjectedLanguageUtil.getUnescapedLeafText(element, false);
        if (leafText != null) {
          text.append(leafText);
          return;
        }
        super.visitElement(element);
      }
    });
    return text.toString();
  }

  /**
   *  intersection may spread over several injected fragments
   *  @param rangeToEdit range in encoded(raw) PSI
   *  @return list of ranges in encoded (raw) PSI
   */
  @SuppressWarnings("ConstantConditions")
  @Override
  @NotNull
  public List<TextRange> intersectWithAllEditableFragments(@NotNull PsiFile injectedPsi, @NotNull TextRange rangeToEdit) {
    Place shreds = InjectedLanguageUtil.getShreds(injectedPsi);
    if (shreds == null) return Collections.emptyList();
    Object result = null; // optimization: TextRange or ArrayList
    int count = 0;
    int offset = 0;
    for (PsiLanguageInjectionHost.Shred shred : shreds) {
      TextRange encodedRange = TextRange.from(offset + shred.getPrefix().length(), shred.getRangeInsideHost().getLength());
      TextRange intersection = encodedRange.intersection(rangeToEdit);
      if (intersection != null) {
        count++;
        if (count == 1) {
          result = intersection;
        }
        else if (count == 2) {
          TextRange range = (TextRange)result;
          if (range.isEmpty()) {
            result = intersection;
            count = 1;
          }
          else if (intersection.isEmpty()) {
            count = 1;
          }
          else {
            List<TextRange> list = new ArrayList<>();
            list.add(range);
            list.add(intersection);
            result = list;
          }
        }
        else if (intersection.isEmpty()) {
          count--;
        }
        else {
          //noinspection unchecked
          ((List<TextRange>)result).add(intersection);
        }
      }
      offset += shred.getPrefix().length() + shred.getRangeInsideHost().getLength() + shred.getSuffix().length();
    }
    //noinspection unchecked
    return count == 0 ? Collections.emptyList() : count == 1 ? Collections.singletonList((TextRange)result) : (List<TextRange>)result;
  }

  @Override
  public boolean isInjectedFragment(@NotNull final PsiFile injectedFile) {
    return injectedFile.getViewProvider() instanceof InjectedFileViewProvider;
  }

  @Override
  public PsiElement findInjectedElementAt(@NotNull PsiFile hostFile, int hostDocumentOffset) {
    return InjectedLanguageUtil.findInjectedElementNoCommit(hostFile, hostDocumentOffset);
  }

  @Override
  public void dropFileCaches(@NotNull PsiFile file) {
    InjectedLanguageUtil.clearCachedInjectedFragmentsForFile(file);
  }

  @Override
  public PsiFile getTopLevelFile(@NotNull PsiElement element) {
    return InjectedLanguageUtil.getTopLevelFile(element);
  }

  @NotNull
  @Override
  public List<DocumentWindow> getCachedInjectedDocumentsInRange(@NotNull PsiFile hostPsiFile, @NotNull TextRange range) {
    return InjectedLanguageUtil.getCachedInjectedDocumentsInRange(hostPsiFile, range);
  }

  @Override
  public void enumerate(@NotNull PsiElement host, @NotNull PsiLanguageInjectionHost.InjectedPsiVisitor visitor) {
    InjectedLanguageUtil.enumerate(host, visitor);
  }

  @Override
  public void enumerateEx(@NotNull PsiElement host,
                          @NotNull PsiFile containingFile,
                          boolean probeUp,
                          @NotNull PsiLanguageInjectionHost.InjectedPsiVisitor visitor) {
    InjectedLanguageUtil.enumerate(host, containingFile, probeUp, visitor);
  }

  @NotNull
  @Override
  public List<TextRange> getNonEditableFragments(@NotNull DocumentWindow window) {
    List<TextRange> result = new ArrayList<>();
    int offset = 0;
    for (PsiLanguageInjectionHost.Shred shred : ((DocumentWindowImpl)window).getShreds()) {
      Segment hostRange = shred.getHostRangeMarker();
      if (hostRange == null) continue;

      offset = appendRange(result, offset, shred.getPrefix().length());
      offset += hostRange.getEndOffset() - hostRange.getStartOffset();
      offset = appendRange(result, offset, shred.getSuffix().length());
    }

    return result;
  }

  @Override
  public boolean mightHaveInjectedFragmentAtOffset(@NotNull Document hostDocument, int hostOffset) {
    return InjectedLanguageUtil.mightHaveInjectedFragmentAtCaret(myProject, hostDocument, hostOffset);
  }

  @NotNull
  @Override
  public DocumentWindow freezeWindow(@NotNull DocumentWindow document) {
    Place shreds = ((DocumentWindowImpl)document).getShreds();
    Project project = shreds.getHostPointer().getProject();
    DocumentEx delegate = ((PsiDocumentManagerBase)PsiDocumentManager.getInstance(project)).getLastCommittedDocument(document.getDelegate());
    Place place = new Place();
    place.addAll(ContainerUtil.map(shreds, shred -> ((ShredImpl)shred).withPsiRange()));
    return new DocumentWindowImpl(delegate, place);
  }

  private static int appendRange(@NotNull List<TextRange> result, int start, int length) {
    if (length > 0) {
      int lastIndex = result.size() - 1;
      TextRange lastRange = lastIndex >= 0 ? result.get(lastIndex) : null;
      if (lastRange != null && lastRange.getEndOffset() == start) {
        result.set(lastIndex, lastRange.grown(length));
      }
      else {
        result.add(TextRange.from(start, length));
      }
    }
    return start + length;
  }

  private final Map<Class<?>, MultiHostInjector[]> myInjectorsClone = new HashMap<>();
  @TestOnly
  public static void pushInjectors(@NotNull Project project) {
    InjectedLanguageManagerImpl cachedManager = (InjectedLanguageManagerImpl)project.getUserData(INSTANCE_CACHE);
    if (cachedManager == null) return;
    try {
      assert cachedManager.myInjectorsClone.isEmpty() : cachedManager.myInjectorsClone;
    }
    finally {
      cachedManager.myInjectorsClone.clear();
    }
    cachedManager.myInjectorsClone.putAll(cachedManager.getInjectorMap().getBackingMap());
  }

  @TestOnly
  public static void checkInjectorsAreDisposed(@Nullable Project project) {
    InjectedLanguageManagerImpl cachedManager =
      project == null ? null : (InjectedLanguageManagerImpl)project.getUserData(INSTANCE_CACHE);
    if (cachedManager == null) return;

    try {
      ClassMapCachingNulls<MultiHostInjector> cached = cachedManager.cachedInjectors;
      if (cached == null) return;
      for (Map.Entry<Class<?>, MultiHostInjector[]> entry : cached.getBackingMap().entrySet()) {
        Class<?> key = entry.getKey();
        if (cachedManager.myInjectorsClone.isEmpty()) return;
        MultiHostInjector[] oldInjectors = cachedManager.myInjectorsClone.get(key);
        for (MultiHostInjector injector : entry.getValue()) {
          if (ArrayUtil.indexOf(oldInjectors, injector) == -1) {
            throw new AssertionError("Injector was not disposed: " + key + " -> " + injector);
          }
        }
      }
    }
    finally {
      cachedManager.myInjectorsClone.clear();
    }
  }

  InjectionResult processInPlaceInjectorsFor(@NotNull PsiFile hostPsiFile, @NotNull PsiElement element) {
    MultiHostInjector[] infos = getInjectorMap().get(element.getClass());
    if (infos == null || infos.length == 0) {
      return null;
    }
    final boolean dumb = myDumbService.isDumb();
    InjectionRegistrarImpl hostRegistrar = new InjectionRegistrarImpl(myProject, hostPsiFile, element, myDocManager);
    for (MultiHostInjector injector : infos) {
      if (dumb && !DumbService.isDumbAware(injector)) {
        continue;
      }

      injector.getLanguagesToInject(hostRegistrar, element);
      InjectionResult result = hostRegistrar.getInjectedResult();
      if (result != null) return result;
    }
    return null;
  }

  @Override
  @Nullable
  public List<Pair<PsiElement, TextRange>> getInjectedPsiFiles(@NotNull final PsiElement host) {
    if (!(host instanceof PsiLanguageInjectionHost) || !((PsiLanguageInjectionHost) host).isValidHost()) {
      return null;
    }
    final PsiElement inTree = InjectedLanguageUtil.loadTree(host, host.getContainingFile());
    final List<Pair<PsiElement, TextRange>> result = new SmartList<>();
    enumerate(inTree, (injectedPsi, places) -> {
      for (PsiLanguageInjectionHost.Shred place : places) {
        if (place.getHost() == inTree) {
          result.add(new Pair<>(injectedPsi, place.getRangeInsideHost()));
        }
      }
    });
    return result.isEmpty() ? null : result;
  }

  private static class PsiManagerRegisteredInjectorsAdapter implements MultiHostInjector {
    public static final PsiManagerRegisteredInjectorsAdapter INSTANCE = new PsiManagerRegisteredInjectorsAdapter();
    @Override
    public void getLanguagesToInject(@NotNull final MultiHostRegistrar injectionPlacesRegistrar, @NotNull PsiElement context) {
      final PsiLanguageInjectionHost host = (PsiLanguageInjectionHost)context;
      InjectedLanguagePlaces placesRegistrar = (language, rangeInsideHost, prefix, suffix) -> injectionPlacesRegistrar
        .startInjecting(language)
        .addPlace(prefix, suffix, host, rangeInsideHost)
        .doneInjecting();
      for (LanguageInjector injector : LanguageInjector.EXTENSION_POINT_NAME.getExtensionList()) {
        injector.getLanguagesToInject(host, placesRegistrar);
      }
    }

    @Override
    @NotNull
    public List<Class<? extends PsiElement>> elementsToInjectIn() {
      return Collections.singletonList(PsiLanguageInjectionHost.class);
    }
  }
}
