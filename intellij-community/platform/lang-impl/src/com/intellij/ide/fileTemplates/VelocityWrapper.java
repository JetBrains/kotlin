// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.fileTemplates;

import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.apache.commons.collections.ExtendedProperties;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.context.Context;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.RuntimeServices;
import org.apache.velocity.runtime.RuntimeSingleton;
import org.apache.velocity.runtime.log.LogSystem;
import org.apache.velocity.runtime.parser.ParseException;
import org.apache.velocity.runtime.parser.node.SimpleNode;
import org.apache.velocity.runtime.resource.Resource;
import org.apache.velocity.runtime.resource.ResourceManager;
import org.apache.velocity.runtime.resource.ResourceManagerImpl;
import org.apache.velocity.runtime.resource.loader.ResourceLoader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;

/**
 * Initializes Velocity when it's actually needed. All interaction with Velocity should go through this class.
 *
 * @author peter
 */
final class VelocityWrapper {
  private static final Logger LOG = Logger.getInstance(VelocityWrapper.class);

  static {
    try{
      final Class<?>[] interfaces = ResourceManagerImpl.class.getInterfaces();
      if (interfaces.length != 1 || !interfaces[0].equals(ResourceManager.class)) {
        throw new IllegalStateException("Incorrect velocity version in the classpath" +
                                        ", ResourceManager in " + PathManager.getJarPathForClass(ResourceManager.class) +
                                        ", ResourceManagerImpl in " + PathManager.getJarPathForClass(ResourceManagerImpl.class));
      }

      LogSystem emptyLogSystem = new LogSystem() {
        @Override
        public void init(RuntimeServices runtimeServices) throws Exception {
        }

        @Override
        public void logVelocityMessage(int i, String s) {
          //todo[myakovlev] log somethere?
        }
      };
      Velocity.setProperty(RuntimeConstants.RUNTIME_LOG_LOGSYSTEM, emptyLogSystem);
      Velocity.setProperty(RuntimeConstants.INPUT_ENCODING, FileTemplate.ourEncoding);
      Velocity.setProperty(RuntimeConstants.PARSER_POOL_SIZE, 3);
      Velocity.setProperty(RuntimeConstants.RESOURCE_LOADER, "includes");
      Velocity.setProperty("includes.resource.loader.instance", new ResourceLoader() {
        @Override
        public void init(ExtendedProperties configuration) {
        }

        @Override
        public InputStream getResourceStream(String resourceName) throws ResourceNotFoundException {
          FileTemplateManager templateManager = VelocityTemplateContext.getFromContext();
          final FileTemplate include = templateManager.getPattern(resourceName);
          if (include == null) {
            throw new ResourceNotFoundException("Template not found: " + resourceName);
          }
          final String text = include.getText();
          try {
            return new ByteArrayInputStream(text.getBytes(FileTemplate.ourEncoding));
          }
          catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
          }
        }

        @Override
        public boolean isSourceModified(Resource resource) {
          return true;
        }

        @Override
        public long getLastModified(Resource resource) {
          return 0L;
        }
      });

      Velocity.init();
    }
    catch (Exception e){
      LOG.error("Unable to init Velocity", e);
    }
  }

  @NotNull
  static SimpleNode parse(@NotNull Reader reader, @NotNull String templateName) throws ParseException {
    return RuntimeSingleton.parse(reader, templateName);
  }

  static boolean evaluate(@Nullable Project project, Context context, @NotNull Writer writer, String templateContent)
    throws ParseErrorException, MethodInvocationException, ResourceNotFoundException {
    return VelocityTemplateContext.withContext(project, () -> Velocity.evaluate(context, writer, "", templateContent));
  }

}
