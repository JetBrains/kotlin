// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.intention.impl.config;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileTypes.*;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.ArrayUtil;
import com.intellij.util.ObjectUtils;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public abstract class BeforeAfterActionMetaData implements BeforeAfterMetaData {
  private static final Logger LOG = Logger.getInstance("#com.intellij.codeInsight.intention.impl.config.BeforeAfterActionMetaData");

  protected static final TextDescriptor[] EMPTY_EXAMPLE = new TextDescriptor[0];
  protected static final TextDescriptor EMPTY_DESCRIPTION = new PlainTextDescriptor("", "");

  @NonNls protected static final String DESCRIPTION_FILE_NAME = "description.html";
  @NonNls static final String EXAMPLE_USAGE_URL_SUFFIX = ".template";
  @NonNls private static final String BEFORE_TEMPLATE_PREFIX = "before";
  @NonNls private static final String AFTER_TEMPLATE_PREFIX = "after";
  protected final ClassLoader myLoader;
  protected final String myDescriptionDirectoryName;
  private TextDescriptor[] myExampleUsagesBefore;
  private TextDescriptor[] myExampleUsagesAfter;
  protected TextDescriptor myDescription;

  public BeforeAfterActionMetaData(@Nullable ClassLoader loader, @NotNull String descriptionDirectoryName) {
    myLoader = loader;
    myDescriptionDirectoryName = descriptionDirectoryName;
  }

  public BeforeAfterActionMetaData(@NotNull TextDescriptor description,
                                   @NotNull TextDescriptor[] exampleUsagesBefore,
                                   @NotNull TextDescriptor[] exampleUsagesAfter) {
    myLoader = null;
    myDescriptionDirectoryName = null;

    myExampleUsagesBefore = exampleUsagesBefore;
    myExampleUsagesAfter = exampleUsagesAfter;
    myDescription = description;
  }

  @NotNull
  private static TextDescriptor[] retrieveURLs(@NotNull URL descriptionDirectory, @NotNull String prefix, @NotNull String suffix)
    throws MalformedURLException {
    List<TextDescriptor> urls = new ArrayList<>();
    final FileType[] fileTypes = FileTypeManager.getInstance().getRegisteredFileTypes();
    for (FileType fileType : fileTypes) {
      final List<FileNameMatcher> matchers = FileTypeManager.getInstance().getAssociations(fileType);
      for (final FileNameMatcher matcher : matchers) {
        if (matcher instanceof ExactFileNameMatcher) {
          final ExactFileNameMatcher exactFileNameMatcher = (ExactFileNameMatcher)matcher;
          final String fileName = StringUtil.trimStart(exactFileNameMatcher.getFileName(), ".");
          URL url = new URL(descriptionDirectory.toExternalForm() + "/" + prefix + "." + fileName + suffix);

          if (checkUrl(url, urls))
            break;
        }
        else if (matcher instanceof ExtensionFileNameMatcher) {
          final ExtensionFileNameMatcher extensionFileNameMatcher = (ExtensionFileNameMatcher)matcher;
          final String extension = extensionFileNameMatcher.getExtension();
          for (int i = 0; ; i++) {
            URL url = new URL(descriptionDirectory.toExternalForm() + "/"
                              + prefix + "." + extension + (i == 0 ? "" : Integer.toString(i))
                              + suffix);
            if (!checkUrl(url, urls))
              break;
          }
        }
      }
    }
    if (urls.isEmpty()) {
      String[] children;
      Exception cause = null;
      try {
        URI uri = descriptionDirectory.toURI();
        children = uri.isOpaque() ? null : ObjectUtils.notNull(new File(uri).list(), ArrayUtil.EMPTY_STRING_ARRAY);
      }
      catch (URISyntaxException | IllegalArgumentException e) {
        cause = e;
        children = null;
      }
      LOG.error("URLs not found for available file types and prefix: '" +
                prefix +
                "', suffix: '" +
                suffix +
                "';" +
                " in directory: '" +
                descriptionDirectory +
                "'" +
                (children == null ? "" : "; directory contents: " + Arrays.asList(children)), cause);
      return EMPTY_EXAMPLE;
    }
    return urls.toArray(new TextDescriptor[0]);
  }

  private static boolean checkUrl(URL url, List<? super TextDescriptor> urls) {
    try (InputStream ignored = url.openStream()) {
      urls.add(new ResourceTextDescriptor(url));
      return true;
    }
    catch (IOException e) {
      return false;
    }
  }

  @Override
  @NotNull
  public TextDescriptor[] getExampleUsagesBefore() {
    if (myExampleUsagesBefore == null) {
      try {
        myExampleUsagesBefore = retrieveURLs(getDirURL(), BEFORE_TEMPLATE_PREFIX, EXAMPLE_USAGE_URL_SUFFIX);
      }
      catch (MalformedURLException e) {
        LOG.error(e);
        return EMPTY_EXAMPLE;
      }
    }
    return myExampleUsagesBefore;
  }

  @Override
  @NotNull
  public TextDescriptor[] getExampleUsagesAfter() {
    if (myExampleUsagesAfter == null) {
      try {
        myExampleUsagesAfter = retrieveURLs(getDirURL(), AFTER_TEMPLATE_PREFIX, EXAMPLE_USAGE_URL_SUFFIX);
      }
      catch (MalformedURLException e) {
        LOG.error(e);
        return EMPTY_EXAMPLE;
      }
    }
    return myExampleUsagesAfter;
  }

  @Override
  @NotNull
  public TextDescriptor getDescription() {
    if (myDescription == null) {
      try {
        final URL dirURL = getDirURL();
        URL descriptionURL = new URL(dirURL.toExternalForm() + "/" + DESCRIPTION_FILE_NAME);
        myDescription = new ResourceTextDescriptor(descriptionURL);
      }
      catch (MalformedURLException e) {
        LOG.error(e);
        return EMPTY_DESCRIPTION;
      }
    }
    return myDescription;
  }

  @NotNull
  protected abstract URL getDirURL();
}
