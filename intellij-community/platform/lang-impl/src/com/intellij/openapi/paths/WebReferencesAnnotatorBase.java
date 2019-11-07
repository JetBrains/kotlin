// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.paths;

import com.intellij.codeHighlighting.HighlightDisplayLevel;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.lang.annotation.Annotation;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.ExternalAnnotator;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiAnchor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import com.intellij.util.io.HttpRequests;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Eugene.Kudelevsky
 */
public abstract class WebReferencesAnnotatorBase extends ExternalAnnotator<WebReferencesAnnotatorBase.MyInfo[], WebReferencesAnnotatorBase.MyInfo[]> {
  private static final Logger LOG = Logger.getInstance(WebReferencesAnnotatorBase.class);

  private final Map<String, MyFetchCacheEntry> myFetchCache = new HashMap<>();
  private final Object myFetchCacheLock = new Object();
  private static final long FETCH_CACHE_TIMEOUT = 10000;

  protected static final WebReference[] EMPTY_ARRAY = new WebReference[0];

  @NotNull
  protected abstract WebReference[] collectWebReferences(@NotNull PsiFile file);

  @Nullable
  protected static WebReference lookForWebReference(@NotNull PsiElement element) {
    return lookForWebReference(Arrays.asList(element.getReferences()));
  }

  @SuppressWarnings("unchecked")
  @Nullable
  private static WebReference lookForWebReference(Collection<PsiReference> references) {
    for (PsiReference reference : references) {
      if (reference instanceof WebReference) {
        return (WebReference)reference;
      }
      else if (reference instanceof PsiDynaReference) {
        final WebReference webReference = lookForWebReference(((PsiDynaReference)reference).getReferences());
        if (webReference != null) {
          return webReference;
        }
      }
    }
    return null;
  }

  @Override
  public MyInfo[] collectInformation(@NotNull PsiFile file) {
    final WebReference[] references = collectWebReferences(file);
    final MyInfo[] infos = new MyInfo[references.length];

    for (int i = 0; i < infos.length; i++) {
      final WebReference reference = references[i];
      infos[i] = new MyInfo(PsiAnchor.create(reference.getElement()), reference.getRangeInElement(), reference.getValue());
    }
    return infos;
  }

  @Override
  public MyInfo[] doAnnotate(MyInfo[] infos) {
    final MyFetchResult[] fetchResults = new MyFetchResult[infos.length];
    for (int i = 0; i < fetchResults.length; i++) {
      fetchResults[i] = checkUrl(infos[i].myUrl);
    }

    boolean containsAvailableHosts = false;
    
    for (MyFetchResult fetchResult : fetchResults) {
      if (fetchResult != MyFetchResult.UNKNOWN_HOST) {
        containsAvailableHosts = true;
        break;
      }
    }

    for (int i = 0; i < fetchResults.length; i++) {
      final MyFetchResult result = fetchResults[i];

      // if all hosts are not available, internet connection may be disabled, so it's better to not report warnings for unknown hosts
      if (result == MyFetchResult.OK || (!containsAvailableHosts && result == MyFetchResult.UNKNOWN_HOST)) {
        infos[i].myResult = true;
      }
    }

    return infos;
  }

  @Override
  public void apply(@NotNull PsiFile file, MyInfo[] infos, @NotNull AnnotationHolder holder) {
    if (infos == null || infos.length == 0) {
      return;
    }

    final HighlightDisplayLevel displayLevel = getHighlightDisplayLevel(file);

    for (MyInfo info : infos) {
      if (!info.myResult) {
        final PsiElement element = info.myAnchor.retrieve();
        if (element != null) {
          final int start = element.getTextRange().getStartOffset();
          final TextRange range = new TextRange(start + info.myRangeInElement.getStartOffset(),
                                                start + info.myRangeInElement.getEndOffset());
          final String message = getErrorMessage(info.myUrl);

          final Annotation annotation;

          if (displayLevel == HighlightDisplayLevel.ERROR) {
            annotation = holder.createErrorAnnotation(range, message);
          }
          else if (displayLevel == HighlightDisplayLevel.WARNING) {
            annotation = holder.createWarningAnnotation(range, message);
          }
          else if (displayLevel == HighlightDisplayLevel.WEAK_WARNING) {
            annotation = holder.createInfoAnnotation(range, message);
          }
          else {
            annotation = holder.createWarningAnnotation(range, message);
          }

          for (IntentionAction action : getQuickFixes()) {
            annotation.registerFix(action);
          }
        }
      }
    }
  }
  
  @NotNull
  protected abstract String getErrorMessage(@NotNull String url);

  @NotNull
  protected IntentionAction[] getQuickFixes() {
    return IntentionAction.EMPTY_ARRAY;
  }
  
  @NotNull
  protected abstract HighlightDisplayLevel getHighlightDisplayLevel(@NotNull PsiElement context);

  @NotNull
  private MyFetchResult checkUrl(String url) {
    synchronized (myFetchCacheLock) {
      final MyFetchCacheEntry entry = myFetchCache.get(url);
      final long currentTime = System.currentTimeMillis();

      if (entry != null && currentTime - entry.getTime() < FETCH_CACHE_TIMEOUT) {
        return entry.getFetchResult();
      }

      final MyFetchResult fetchResult = doCheckUrl(url);
      myFetchCache.put(url, new MyFetchCacheEntry(currentTime, fetchResult));
      return fetchResult;
    }
  }

  private static MyFetchResult doCheckUrl(@NotNull String url) {
    if (url.startsWith("mailto")) {
      return MyFetchResult.OK;
    }

    try {
      HttpRequests.request(url).connectTimeout(3000).readTimeout(3000).tryConnect();
    }
    catch (UnknownHostException e) {
      LOG.info(e);
      return MyFetchResult.UNKNOWN_HOST;
    }
    catch (HttpRequests.HttpStatusException e) {
      LOG.info(e);
      return MyFetchResult.NONEXISTENCE;
    }
    catch (IOException e) {
      LOG.info(e);
    }
    catch (IllegalArgumentException e) {
      LOG.debug(e);
    }
    return MyFetchResult.OK;
  }

  private static class MyFetchCacheEntry {
    private final long myTime;
    private final MyFetchResult myFetchResult;

    private MyFetchCacheEntry(long time, @NotNull MyFetchResult fetchResult) {
      myTime = time;
      myFetchResult = fetchResult;
    }

    public long getTime() {
      return myTime;
    }

    @NotNull
    public MyFetchResult getFetchResult() {
      return myFetchResult;
    }
  }
  
  private enum MyFetchResult {
    OK, UNKNOWN_HOST, NONEXISTENCE
  }

  protected static class MyInfo {
    final PsiAnchor myAnchor;
    final String myUrl;
    final TextRange myRangeInElement;

    volatile boolean myResult;

    private MyInfo(PsiAnchor anchor, TextRange rangeInElement, String url) {
      myAnchor = anchor;
      myRangeInElement = rangeInElement;
      myUrl = url;
    }
  }
}
