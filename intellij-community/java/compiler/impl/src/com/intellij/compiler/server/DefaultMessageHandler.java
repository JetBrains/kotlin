// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.compiler.server;

import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.lang.java.JavaLanguage;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.search.*;
import com.intellij.util.SmartList;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.intellij.util.concurrency.SequentialTaskExecutor;
import com.intellij.util.containers.ContainerUtil;
import io.netty.channel.Channel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.concurrency.CancellablePromise;
import org.jetbrains.jps.api.CmdlineProtoUtil;
import org.jetbrains.jps.api.CmdlineRemoteProto;
import org.jetbrains.org.objectweb.asm.Opcodes;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author Eugene Zhuravlev
 */
public abstract class DefaultMessageHandler implements BuilderMessageHandler {
  private static final Logger LOG = Logger.getInstance(DefaultMessageHandler.class);
  private final Project myProject;
  private final ExecutorService myTaskExecutor = SequentialTaskExecutor.createSequentialApplicationPoolExecutor(
    "DefaultMessageHandler Pool");

  protected DefaultMessageHandler(Project project) {
    myProject = project;
  }

  @Override
  public void buildStarted(@NotNull UUID sessionId) {
  }

  @Override
  public final void handleBuildMessage(final Channel channel, final UUID sessionId, final CmdlineRemoteProto.Message.BuilderMessage msg) {
    //noinspection EnumSwitchStatementWhichMissesCases
    switch (msg.getType()) {
      case BUILD_EVENT:
        final CmdlineRemoteProto.Message.BuilderMessage.BuildEvent event = msg.getBuildEvent();
        if (event.getEventType() == CmdlineRemoteProto.Message.BuilderMessage.BuildEvent.Type.CUSTOM_BUILDER_MESSAGE && event.hasCustomBuilderMessage()) {
          final CmdlineRemoteProto.Message.BuilderMessage.BuildEvent.CustomBuilderMessage message = event.getCustomBuilderMessage();
          if (!myProject.isDisposed()) {
            myProject.getMessageBus().syncPublisher(CustomBuilderMessageHandler.TOPIC).messageReceived(
              message.getBuilderId(), message.getMessageType(), message.getMessageText()
            );
          }
        }
        handleBuildEvent(sessionId, event);
        break;
      case COMPILE_MESSAGE:
        CmdlineRemoteProto.Message.BuilderMessage.CompileMessage compileMessage = msg.getCompileMessage();
        handleCompileMessage(sessionId, compileMessage);
        if (compileMessage.getKind() == CmdlineRemoteProto.Message.BuilderMessage.CompileMessage.Kind.INTERNAL_BUILDER_ERROR) {
          LOG.error("Internal build error:\n" + compileMessage.getText());
        }
        break;
      case CONSTANT_SEARCH_TASK:
        final CmdlineRemoteProto.Message.BuilderMessage.ConstantSearchTask task = msg.getConstantSearchTask();
        handleConstantSearchTask(channel, sessionId, task);
        break;
    }
  }

  protected abstract void handleCompileMessage(UUID sessionId, CmdlineRemoteProto.Message.BuilderMessage.CompileMessage message);

  protected abstract void handleBuildEvent(UUID sessionId, CmdlineRemoteProto.Message.BuilderMessage.BuildEvent event);

  private void handleConstantSearchTask(final Channel channel, final UUID sessionId, final CmdlineRemoteProto.Message.BuilderMessage.ConstantSearchTask task) {
    if (ReadAction.compute(() -> !myProject.isDisposed() && DumbService.getInstance(myProject).isSuspendedDumbMode())) {
      LOG.debug("Constant search wasn't performed because of suspended dumb mode, so waiting for it to finish within a timeout makes little sense");
      notifyConstantSearchFinished(channel, sessionId, task.getOwnerClassName(), task.getFieldName(), null);
      return;
    }

    CancellablePromise<Set<String>> search = ReadAction
      .nonBlocking(() -> doHandleConstantSearchTask(sessionId, task))
      .inSmartMode(myProject)
      .submit(myTaskExecutor);

    ScheduledFuture<?> cancelByTimeout = AppExecutorUtil.getAppScheduledExecutorService().schedule(() -> {
      LOG.debug("Total constant search time exceeded time limit for this build session");
      search.cancel();
    }, 1, TimeUnit.MINUTES);

    search.onProcessed(affectedPaths -> {
      cancelByTimeout.cancel(false);
      notifyConstantSearchFinished(channel, sessionId, task.getOwnerClassName(), task.getFieldName(), affectedPaths);
    });
  }

  @Nullable("when no reliable search can be performed via PSI")
  private Set<String> doHandleConstantSearchTask(UUID sessionId, CmdlineRemoteProto.Message.BuilderMessage.ConstantSearchTask task) {
    final String ownerClassName = task.getOwnerClassName();
    final String fieldName = task.getFieldName();
    final int accessFlags = task.getAccessFlags();
    final boolean accessChanged = task.getIsAccessChanged();
    final boolean isRemoved = task.getIsFieldRemoved();
    final Set<String> affectedPaths = Collections.synchronizedSet(new HashSet<>()); // PsiSearchHelper runs multiple threads
    final String qualifiedName = ownerClassName.replace('$', '.');

    handleCompileMessage(sessionId, CmdlineProtoUtil.createCompileProgressMessageResponse(
      "Searching for usages of changed/removed constants for class " + qualifiedName
    ).getCompileMessage());

    PsiClass[] classes = JavaPsiFacade.getInstance(myProject).findClasses(qualifiedName, GlobalSearchScope.allScope(myProject));

    try {
      if (isRemoved) {
        if (classes.length > 0) {
          for (PsiClass aClass : classes) {
            if (!performRemovedConstantSearch(aClass, fieldName, accessFlags, affectedPaths)) {
              return null;
            }
          }
        }
        else if (!performRemovedConstantSearch(null, fieldName, accessFlags, affectedPaths)) {
          return null;
        }
      }
      else {
        if (classes.length > 0) {
          List<PsiField> changedFields = ContainerUtil.mapNotNull(classes, aClass -> aClass.findFieldByName(fieldName, false));
          if (changedFields.isEmpty()) {
            LOG.debug("Constant search task: field " + fieldName + " not found in classes " + qualifiedName);
            return null;
          }
          else {
            for (PsiField changedField : changedFields) {
              if (!accessChanged && isPrivate(accessFlags)) {
                // optimization: don't need to search, because it may be used only in this class
                continue;
              }
              if (!affectDirectUsages(changedField, accessChanged, affectedPaths)) {
                return null;
              }
            }
          }
        }
        else {
          LOG.debug("Constant search task: class " + qualifiedName + " not found");
          return null;
        }
      }
    }
    catch (Throwable e) {
      LOG.debug("Constant search task: failed with message " + e.getMessage());
      return null;
    }
    return affectedPaths;
  }

  private static void notifyConstantSearchFinished(Channel channel,
                                                   UUID sessionId,
                                                   String ownerClassName,
                                                   String fieldName,
                                                   @Nullable Set<String> affectedPaths) {
    final CmdlineRemoteProto.Message.ControllerMessage.ConstantSearchResult.Builder builder =
      CmdlineRemoteProto.Message.ControllerMessage.ConstantSearchResult.newBuilder();
    builder.setOwnerClassName(ownerClassName);
    builder.setFieldName(fieldName);
    builder.setIsSuccess(affectedPaths != null);
    if (affectedPaths != null) {
      builder.addAllPath(affectedPaths);
      LOG.debug("Constant search task: " + affectedPaths.size() + " affected files found");
    }
    else {
      LOG.debug("Constant search task: unsuccessful");
    }
    channel.writeAndFlush(CmdlineProtoUtil.toMessage(sessionId, CmdlineRemoteProto.Message.ControllerMessage.newBuilder().setType(
      CmdlineRemoteProto.Message.ControllerMessage.Type.CONSTANT_SEARCH_RESULT).setConstantSearchResult(builder.build()).build()
    ));
  }

  private boolean performRemovedConstantSearch(@Nullable final PsiClass aClass, String fieldName, int fieldAccessFlags, final Set<? super String> affectedPaths) {
    final PsiSearchHelper psiSearchHelper = PsiSearchHelper.getInstance(myProject);

    final Ref<Boolean> result = new Ref<>(Boolean.TRUE);
    final PsiFile fieldContainingFile = aClass != null? aClass.getContainingFile() : null;

    SearchScope searchScope = getSearchScope(aClass, fieldAccessFlags);
    if (containsUnloadedModules(searchScope)) {
      LOG.debug("Constant search tasks: there may be usages of " + (aClass!= null ? aClass.getQualifiedName() + "::": "") + fieldName + " in unloaded modules");
      return false;
    }
    processIdentifiers(psiSearchHelper, new PsiElementProcessor<PsiIdentifier>() {
      @Override
      public boolean execute(@NotNull PsiIdentifier identifier) {
        try {
          final PsiElement parent = identifier.getParent();
          if (parent instanceof PsiReferenceExpression) {
            final PsiClass ownerClass = getOwnerClass(parent);
            if (ownerClass != null && ownerClass.getQualifiedName() != null) {
              final PsiFile usageFile = ownerClass.getContainingFile();
              if (usageFile != null && !usageFile.equals(fieldContainingFile)) {
                final VirtualFile vFile = usageFile.getOriginalFile().getVirtualFile();
                if (vFile != null) {
                  affectedPaths.add(vFile.getPath());
                }
              }
            }
          }
          return true;
        }
        catch (PsiInvalidElementAccessException ignored) {
          result.set(Boolean.FALSE);
          LOG.debug("Constant search task: PIEAE thrown while searching of usages of removed constant");
          return false;
        }
      }
    }, fieldName, searchScope);

    return result.get();
  }

  private SearchScope getSearchScope(PsiClass aClass, int fieldAccessFlags) {
    SearchScope searchScope = GlobalSearchScope.projectScope(myProject);
    if (aClass != null && isPackageLocal(fieldAccessFlags)) {
      final PsiFile containingFile = aClass.getContainingFile();
      if (containingFile instanceof PsiJavaFile) {
        final String packageName = ((PsiJavaFile)containingFile).getPackageName();
        final PsiPackage aPackage = JavaPsiFacade.getInstance(myProject).findPackage(packageName);
        if (aPackage != null) {
          searchScope = PackageScope.packageScope(aPackage, false);
          searchScope = searchScope.intersectWith(aClass.getUseScope());
        }
      }
    }
    return searchScope;
  }

  private static void processIdentifiers(PsiSearchHelper helper,
                                         @NotNull final PsiElementProcessor<? super PsiIdentifier> processor,
                                         @NotNull final String identifier,
                                         @NotNull SearchScope searchScope) {
    TextOccurenceProcessor processor1 =
      (element, offsetInElement) -> !(element instanceof PsiIdentifier) || processor.execute((PsiIdentifier)element);
    SearchScope javaScope = searchScope instanceof GlobalSearchScope
                            ? GlobalSearchScope.getScopeRestrictedByFileTypes((GlobalSearchScope)searchScope, JavaFileType.INSTANCE)
                            : searchScope;
    helper.processElementsWithWord(processor1, javaScope, identifier, UsageSearchContext.IN_CODE, true, false);
  }

  private boolean affectDirectUsages(final PsiField psiField,
                                     final boolean ignoreAccessScope,
                                     final Set<? super String> affectedPaths) throws ProcessCanceledException {
    final PsiFile fieldContainingFile = psiField.getContainingFile();
    final Set<PsiFile> processedFiles = new HashSet<>();
    if (fieldContainingFile != null) {
      processedFiles.add(fieldContainingFile);
    }
    // if field is invalid, the file might be changed, so next time it is compiled,
    // the constant value change, if any, will be processed
    final Collection<PsiReferenceExpression> references = doFindReferences(psiField, ignoreAccessScope);
    if (references == null) {
      return false;
    }

    for (final PsiReferenceExpression ref : references) {
      final PsiElement usage = ref.getElement();
      final PsiFile containingPsi = usage.getContainingFile();
      if (containingPsi != null && processedFiles.add(containingPsi)) {
        final VirtualFile vFile = containingPsi.getOriginalFile().getVirtualFile();
        if (vFile != null) {
          affectedPaths.add(vFile.getPath());
        }
      }
    }
    return true;
  }

  @Nullable("returns null if search failed")
  private Collection<PsiReferenceExpression> doFindReferences(final PsiField psiField, boolean ignoreAccessScope) {
    final SmartList<PsiReferenceExpression> result = new SmartList<>();

    final SearchScope searchScope = (ignoreAccessScope? psiField.getContainingFile() : psiField).getUseScope();
    if (containsUnloadedModules(searchScope)) {
      PsiClass aClass = psiField.getContainingClass();
      LOG.debug("Constant search tasks: there may be usages of " + (aClass != null ? aClass.getQualifiedName() + "::" : "") + psiField.getName() + " in unloaded modules");
      return null;
    }

    processIdentifiers(PsiSearchHelper.getInstance(myProject), new PsiElementProcessor<PsiIdentifier>() {
      @Override
      public boolean execute(@NotNull PsiIdentifier identifier) {
        final PsiElement parent = identifier.getParent();
        if (parent instanceof PsiReferenceExpression) {
          final PsiReferenceExpression refExpression = (PsiReferenceExpression)parent;
          if (refExpression.isReferenceTo(psiField)) {
            synchronized (result) {
              // processor's code may be invoked from multiple threads
              result.add(refExpression);
            }
          }
        }
        return true;
      }
    }, psiField.getName(), searchScope);

    return result;
  }

  @Nullable
  private static PsiClass getOwnerClass(PsiElement element) {
    while (!(element instanceof PsiFile)) {
      if (element instanceof PsiClass && element.getParent() instanceof PsiJavaFile) { // top-level class
        final PsiClass psiClass = (PsiClass)element;
        if (JspPsiUtil.isInJspFile(psiClass)) {
          return null;
        }
        final PsiFile containingFile = psiClass.getContainingFile();
        if (containingFile == null) {
          return null;
        }
        return JavaLanguage.INSTANCE.equals(containingFile.getLanguage())? psiClass : null;
      }
      element = element.getParent();
    }
    return null;
  }

  private static boolean containsUnloadedModules(SearchScope scope) {
    if (scope instanceof LocalSearchScope) {
      return false;
    }
    else if (scope instanceof GlobalSearchScope) {
      return !((GlobalSearchScope)scope).getUnloadedModulesBelongingToScope().isEmpty();
    }
    else {
      //cannot happen now, every SearchScope's implementation extends either LocalSearchScope or GlobalSearchScope
      return true;
    }
  }

  private static boolean isPackageLocal(int flags) {
    return (Opcodes.ACC_PUBLIC & flags) == 0 && (Opcodes.ACC_PROTECTED & flags) == 0 && (Opcodes.ACC_PRIVATE & flags) == 0;
  }

  private static boolean isPrivate(int flags) {
    return (Opcodes.ACC_PRIVATE & flags) != 0;
  }
}
