// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.codeInsight.template.postfix.settings;

import com.intellij.codeInsight.intention.impl.config.BeforeAfterActionMetaData;
import com.intellij.codeInsight.intention.impl.config.BeforeAfterMetaData;
import com.intellij.codeInsight.intention.impl.config.TextDescriptor;
import com.intellij.codeInsight.template.postfix.templates.PostfixTemplate;
import com.intellij.codeInsight.template.postfix.templates.editable.EditablePostfixTemplate;
import com.intellij.codeInsight.template.postfix.templates.editable.PostfixTemplateWrapper;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.lang.UrlClassLoader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

public final class PostfixTemplateMetaData extends BeforeAfterActionMetaData {

  public static final String KEY = "$key";

  public static final PostfixTemplateMetaData EMPTY_METADATA = new PostfixTemplateMetaData();
  private static final Logger LOG = Logger.getInstance("#com.intellij.codeInsight.template.postfix.settings.PostfixTemplateMetaData");
  private static final String DESCRIPTION_FOLDER = "postfixTemplates";

  @NotNull
  public static BeforeAfterMetaData createMetaData(@Nullable PostfixTemplate template) {
    if (template == null) return EMPTY_METADATA;
    if (template instanceof PostfixTemplateWrapper) {
      return new PostfixTemplateWrapperMetaData((PostfixTemplateWrapper)template);
    }
    if (template instanceof EditablePostfixTemplate && !template.isBuiltin()) {
      return new EditablePostfixTemplateMetaData((EditablePostfixTemplate)template);
    }
    return new PostfixTemplateMetaData(template);
  }

  private URL urlDir = null;
  private PostfixTemplate myTemplate;

  public PostfixTemplateMetaData(@NotNull PostfixTemplate template) {
    super(template.getClass().getClassLoader(), template.getClass().getSimpleName());
    myTemplate = template;
  }

  PostfixTemplateMetaData() {
    super(EMPTY_DESCRIPTION, EMPTY_EXAMPLE, EMPTY_EXAMPLE);
  }

  @NotNull
  @Override
  public TextDescriptor[] getExampleUsagesBefore() {
    return decorateTextDescriptor(getRawExampleUsagesBefore());
  }

  @NotNull
  TextDescriptor[] getRawExampleUsagesBefore() {
    return super.getExampleUsagesBefore();
  }

  @NotNull
  private TextDescriptor[] decorateTextDescriptor(TextDescriptor[] before) {
    String key = myTemplate.getKey();
    return decorateTextDescriptorWithKey(before, key);
  }

  @NotNull
  static TextDescriptor[] decorateTextDescriptorWithKey(TextDescriptor[] before, @NotNull String key) {
    List<TextDescriptor> list = ContainerUtil.newArrayListWithCapacity(before.length);
    for (final TextDescriptor descriptor : before) {
      list.add(new TextDescriptor() {
        @Override
        public String getText() throws IOException {
          return StringUtil.replace(descriptor.getText(), KEY, key);
        }

        @Override
        public String getFileName() {
          return descriptor.getFileName();
        }
      });
    }
    return list.toArray(new TextDescriptor[0]);
  }

  @NotNull
  @Override
  public TextDescriptor[] getExampleUsagesAfter() {
    return decorateTextDescriptor(getRawExampleUsagesAfter());
  }

  @NotNull
  TextDescriptor[] getRawExampleUsagesAfter() {
    return super.getExampleUsagesAfter();
  }

  @NotNull
  @Override
  protected URL getDirURL() {
    if (urlDir != null) {
      return urlDir;
    }

    final URL pageURL = myLoader.getResource(DESCRIPTION_FOLDER + "/" + myDescriptionDirectoryName + "/" + DESCRIPTION_FILE_NAME);
    if (LOG.isDebugEnabled()) {
      LOG.debug("Path:" + DESCRIPTION_FOLDER + "/" + myDescriptionDirectoryName);
      LOG.debug("URL:" + pageURL);
    }
    if (pageURL != null) {
      try {
        final String url = pageURL.toExternalForm();
        urlDir = UrlClassLoader.internProtocol(new URL(url.substring(0, url.lastIndexOf('/'))));
        return urlDir;
      }
      catch (MalformedURLException e) {
        LOG.error(e);
      }
    }
    return null;
  }
}
