// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.jps.gradle.compiler;

import com.intellij.openapi.util.Ref;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.filters.ExpandProperties;
import org.gradle.api.Transformer;
import org.gradle.util.ConfigureUtil;
import org.jetbrains.jps.gradle.model.impl.ResourceRootFilter;
import org.jetbrains.jps.incremental.CompileContext;
import org.jetbrains.jps.incremental.messages.BuildMessage;
import org.jetbrains.jps.incremental.messages.CompilerMessage;

import java.io.File;
import java.io.FilterReader;
import java.io.Reader;
import java.lang.reflect.Constructor;
import java.util.*;
import java.util.regex.Matcher;

/**
 * @author Vladislav.Soroka
 */
public class ChainingFilterTransformer implements Transformer<Reader, Reader> {
  private final Collection<ResourceRootFilter> myFilters = new ArrayList<>();
  private final CompileContext myContext;
  private final Ref<File> myOutputFileRef;


  public ChainingFilterTransformer(CompileContext context, Collection<? extends ResourceRootFilter> filters, Ref<File> outputFileRef) {
    myContext = context;
    myOutputFileRef = outputFileRef;
    myFilters.addAll(filters);
  }

  public ChainingFilterTransformer(CompileContext context, Collection<? extends ResourceRootFilter> filters) {
    this(context, filters, null);
  }

  public void addAll(Collection<ResourceRootFilter> filters) {
    myFilters.addAll(filters);
  }

  public void add(ResourceRootFilter... filters) {
    Collections.addAll(myFilters, filters);
  }

  @Override
  public Reader transform(Reader original) {
    Reader value = original;
    for (ResourceRootFilter filter : myFilters) {
      value = doTransform(filter, value);
    }
    return value;
  }

  private Reader doTransform(ResourceRootFilter filter, Reader original) {
    if ("RenamingCopyFilter".equals(filter.filterType)) {
      final Matcher matcher = (Matcher)filter.getProperties().get("matcher");
      final String replacement = (String)filter.getProperties().get("replacement");
      if (matcher == null || replacement == null) return original;

      matcher.reset(myOutputFileRef.get().getName());
      if (matcher.find()) {
        final String newFileName = matcher.replaceFirst(replacement);
        myOutputFileRef.set(new File(myOutputFileRef.get().getParentFile(), newFileName));
      }
      return original;
    }
    try {
      Class<?> clazz = Class.forName(filter.filterType);
      if (!FilterReader.class.isAssignableFrom(clazz)) {
        myContext.processMessage(
          new CompilerMessage(
            GradleResourcesBuilder.BUILDER_NAME, BuildMessage.Kind.WARNING,
            String.format("Error - Invalid filter specification for %s. It should extend java.io.FilterReader.", filter.filterType), null)
        );
      }
      Constructor constructor = clazz.getConstructor(Reader.class);
      FilterReader result = (FilterReader)constructor.newInstance(original);
      final Map<Object, Object> properties = filter.getProperties();
      if (!properties.isEmpty()) {
        if (ExpandProperties.class.getName().equals(filter.filterType)) {
          final Map<Object, Object> antProps = new HashMap<>(properties);
          final Project project = new Project();
          for (Map.Entry<Object, Object> entry : antProps.entrySet()) {
            project.setProperty(entry.getKey().toString(), entry.getValue().toString());
          }
          properties.clear();
          properties.put("project", project);
        }
        ConfigureUtil.configureByMap(properties, result);
      }
      return result;
    }
    catch (Throwable th) {
      myContext.processMessage(new CompilerMessage(
                                 GradleResourcesBuilder.BUILDER_NAME, BuildMessage.Kind.WARNING,
                                 String.format("Error - Failed to apply filter(%s): %s", filter.filterType, th.getMessage()), null)
      );
    }
    return original;
  }
}
