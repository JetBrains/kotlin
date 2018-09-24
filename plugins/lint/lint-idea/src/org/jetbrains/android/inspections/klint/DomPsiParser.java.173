/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetbrains.android.inspections.klint;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.tools.klint.client.api.LintClient;
import com.android.tools.klint.client.api.XmlParser;
import com.android.tools.klint.detector.api.DefaultPosition;
import com.android.tools.klint.detector.api.Location;
import com.android.tools.klint.detector.api.Position;
import com.android.tools.klint.detector.api.XmlContext;
import com.intellij.openapi.application.AccessToken;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiFile;
import com.intellij.psi.xml.XmlFile;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import java.io.File;

/**
 * Lint parser which reads in a DOM from a given file, by mapping to the underlying XML PSI structure
 */
class DomPsiParser extends XmlParser {
  private final LintClient myClient;
  private AccessToken myReadLock;

  public DomPsiParser(LintClient client) {
    myClient = client;
  }

  @Override
  public void dispose(@NonNull XmlContext context, @NonNull Document document) {
    if (context.document != null) {
      myReadLock.finish();
      myReadLock = null;
      context.document = null;
    }
  }

  @Override
  public int getNodeStartOffset(@NonNull XmlContext context, @NonNull Node node) {
    TextRange textRange = DomPsiConverter.getTextRange(node);
    return textRange.getStartOffset();
  }

  @Override
  public int getNodeEndOffset(@NonNull XmlContext context, @NonNull Node node) {
    TextRange textRange = DomPsiConverter.getTextRange(node);
    return textRange.getEndOffset();
  }

  @Nullable
  @Override
  public Document parseXml(@NonNull final XmlContext context) {
    assert myReadLock == null;
    myReadLock = ApplicationManager.getApplication().acquireReadActionLock();
    Document document = parse(context);
    if (document == null) {
      myReadLock.finish();
      myReadLock = null;
    }
    return document;
  }

  @Nullable
  private Document parse(XmlContext context) {
    // Should only be called from read thread
    assert ApplicationManager.getApplication().isReadAccessAllowed();

    final PsiFile psiFile = IntellijLintUtils.getPsiFile(context);
    if (!(psiFile instanceof XmlFile)) {
      return null;
    }
    XmlFile xmlFile = (XmlFile)psiFile;

    try {
      return DomPsiConverter.convert(xmlFile);
    } catch (Throwable t) {
      myClient.log(t, "Failed converting PSI parse tree to DOM for file %1$s",
                   context.file.getPath());
      return null;
    }
  }

  @NonNull
  @Override
  public Location getLocation(@NonNull XmlContext context, @NonNull Node node) {
    TextRange textRange = DomPsiConverter.getTextRange(node);
    Position start = new DefaultPosition(-1, -1, textRange.getStartOffset());
    Position end = new DefaultPosition(-1, -1, textRange.getEndOffset());
    return Location.create(context.file, start, end);
  }

  @NonNull
  @Override
  public Location getLocation(@NonNull XmlContext context, @NonNull Node node, int startDelta, int endDelta) {
    TextRange textRange = DomPsiConverter.getTextRange(node);
    Position start = new DefaultPosition(-1, -1, textRange.getStartOffset() + startDelta);
    Position end = new DefaultPosition(-1, -1, textRange.getStartOffset() + endDelta);
    return Location.create(context.file, start, end);
  }

  @NonNull
  @Override
  public Location getNameLocation(@NonNull XmlContext context, @NonNull Node node) {
    TextRange textRange = DomPsiConverter.getTextNameRange(node);
    Position start = new DefaultPosition(-1, -1, textRange.getStartOffset());
    Position end = new DefaultPosition(-1, -1, textRange.getEndOffset());
    return Location.create(context.file, start, end);
  }

  @NonNull
  @Override
  public Location getValueLocation(@NonNull XmlContext context, @NonNull Attr node) {
    TextRange textRange = DomPsiConverter.getTextValueRange(node);
    Position start = new DefaultPosition(-1, -1, textRange.getStartOffset());
    Position end = new DefaultPosition(-1, -1, textRange.getEndOffset());
    return Location.create(context.file, start, end);
  }

  @NonNull
  @Override
  public Location.Handle createLocationHandle(@NonNull XmlContext context, @NonNull Node node) {
    return new LocationHandle(context.file, node);
  }

  private static class LocationHandle implements Location.Handle {
    private final File myFile;
    private final Node myNode;
    private Object myClientData;

    public LocationHandle(File file, Node node) {
      myFile = file;
      myNode = node;
    }

    @NonNull
    @Override
    public Location resolve() {
      if (!ApplicationManager.getApplication().isReadAccessAllowed()) {
        return ApplicationManager.getApplication().runReadAction(new Computable<Location>() {
          @Override
          public Location compute() {
            return resolve();
          }
        });
      }
      TextRange textRange = DomPsiConverter.getTextRange(myNode);
      Position start = new DefaultPosition(-1, -1, textRange.getStartOffset());
      Position end = new DefaultPosition(-1, -1, textRange.getEndOffset());
      return Location.create(myFile, start, end);
    }

    @Override
    public void setClientData(@Nullable Object clientData) {
      myClientData = clientData;
    }

    @Override
    @Nullable
    public Object getClientData() {
      return myClientData;
    }
  }
}
