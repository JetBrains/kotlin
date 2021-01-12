// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.jps.gradle.compiler;

import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.io.StreamUtil;
import org.apache.tools.ant.util.ReaderInputStream;
import org.jetbrains.jps.gradle.model.impl.GradleModuleResourceConfiguration;
import org.jetbrains.jps.gradle.model.impl.GradleProjectConfiguration;
import org.jetbrains.jps.gradle.model.impl.ResourceRootConfiguration;
import org.jetbrains.jps.gradle.model.impl.ResourceRootFilter;
import org.jetbrains.jps.incremental.CompileContext;
import org.jetbrains.jps.incremental.FSOperations;
import org.jetbrains.jps.incremental.messages.BuildMessage;
import org.jetbrains.jps.incremental.messages.CompilerMessage;
import org.jetbrains.jps.model.JpsEncodingConfigurationService;
import org.jetbrains.jps.model.JpsEncodingProjectConfiguration;
import org.jetbrains.jps.model.JpsProject;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * @author Vladislav.Soroka
 */
public class GradleResourceFileProcessor {
  private static final int FILTERING_SIZE_LIMIT = 10 * 1024 * 1024 /*10 mb*/;
  protected final JpsEncodingProjectConfiguration myEncodingConfig;
  protected final GradleProjectConfiguration myProjectConfig;
  protected final GradleModuleResourceConfiguration myModuleConfiguration;

  public GradleResourceFileProcessor(GradleProjectConfiguration projectConfiguration, JpsProject project,
                                     GradleModuleResourceConfiguration moduleConfiguration) {
    myProjectConfig = projectConfiguration;
    myEncodingConfig = JpsEncodingConfigurationService.getInstance().getEncodingConfiguration(project);
    myModuleConfiguration = moduleConfiguration;
  }

  public void copyFile(File file, Ref<File> targetFileRef, ResourceRootConfiguration rootConfiguration, CompileContext context,
                       FileFilter filteringFilter) throws IOException {
    boolean shouldFilter = rootConfiguration.isFiltered && !rootConfiguration.filters.isEmpty() && filteringFilter.accept(file);
    if (shouldFilter && file.length() > FILTERING_SIZE_LIMIT) {
      context.processMessage(new CompilerMessage(
        GradleResourcesBuilder.BUILDER_NAME, BuildMessage.Kind.WARNING,
        "File is too big to be filtered. Most likely it is a binary file and should be excluded from filtering", file.getPath())
      );
      shouldFilter = false;
    }
    if (shouldFilter) {
      copyWithFiltering(file, targetFileRef, rootConfiguration.filters, context);
    }
    else {
      FSOperations.copy(file, targetFileRef.get());
    }
  }

  private static void copyWithFiltering(File file, Ref<File> outputFileRef, List<ResourceRootFilter> filters, CompileContext context)
    throws IOException {
    final FileInputStream originalInputStream = new FileInputStream(file);
    try {
      final InputStream inputStream = transform(filters, originalInputStream, outputFileRef, context);
      try {
        final File outputFile = outputFileRef.get();
        FileUtil.createIfDoesntExist(outputFile);
        FileOutputStream outputStream = new FileOutputStream(outputFileRef.get());
        try {
          FileUtil.copy(inputStream, outputStream);
        }
        finally {
          StreamUtil.closeStream(outputStream);
        }
      }
      finally {
        StreamUtil.closeStream(inputStream);
      }
    }
    finally {
      StreamUtil.closeStream(originalInputStream);
    }
  }

  private static InputStream transform(List<ResourceRootFilter> filters, FileInputStream original, Ref<File> outputFileRef, CompileContext context) {
    final InputStreamReader streamReader = new InputStreamReader(original, StandardCharsets.UTF_8);
    final Reader newReader = new ChainingFilterTransformer(context, filters, outputFileRef).transform(streamReader);
    return streamReader == newReader ? original : new ReaderInputStream(newReader);
  }
}