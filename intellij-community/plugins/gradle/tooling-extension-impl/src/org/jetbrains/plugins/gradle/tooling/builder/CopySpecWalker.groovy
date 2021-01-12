/*
 * Copyright 2000-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.plugins.gradle.tooling.builder

import org.gradle.api.Action
import org.gradle.api.file.CopySpec
import org.gradle.api.file.FileVisitDetails
import org.gradle.api.file.FileVisitor
import org.gradle.util.GradleVersion

/**
 * @author Vladislav.Soroka
 */
class CopySpecWalker {

  static interface Visitor {
    void visitSourcePath(String relativePath, String path)

    void visitDir(String relativePath, FileVisitDetails dirDetails)

    void visitFile(String relativePath, FileVisitDetails fileDetails)
  }

  @SuppressWarnings("GrUnresolvedAccess")
  static void walk(CopySpec copySpec, Visitor visitor) {
    copySpec.setIncludeEmptyDirs(true)
    if (!(copySpec.metaClass.respondsTo(copySpec, 'walk', Action))) {
      return
    }

    copySpec.walk({ resolver ->
      // def resolver ->
      //      in Gradle v1.x - org.gradle.api.internal.file.copy.CopySpecInternal
      //      in Gradle v2.x - org.gradle.api.internal.file.copy.CopySpecResolver

      if (resolver.metaClass.respondsTo(resolver, 'setIncludeEmptyDirs', boolean)) {
        resolver.setIncludeEmptyDirs(true)
      }
      if (!resolver.metaClass.respondsTo(resolver, 'getDestPath') ||
          !resolver.metaClass.respondsTo(resolver, 'getSource')) {
        throw new RuntimeException("${GradleVersion.current()} is not supported by JEE artifact importer")
      }

      final String relativePath = resolver.destPath.pathString
      def sourcePaths

      if (resolver.metaClass.respondsTo(resolver, 'getSourcePaths')) {
        sourcePaths = resolver.getSourcePaths()
      }
      else if (resolver.hasProperty('sourcePaths')) {
        sourcePaths = resolver.sourcePaths
      }
      else if (resolver.hasProperty('this$0') && resolver.this$0.metaClass.respondsTo(resolver, 'getSourcePaths')) {
        sourcePaths = resolver.this$0.getSourcePaths()
      }
      else if (resolver.hasProperty('this$0') && resolver.this$0.hasProperty('sourcePaths')) {
        sourcePaths = resolver.this$0.sourcePaths
      }

      if (sourcePaths) {
        (sourcePaths.flatten() as List).each { path ->
          if (path instanceof String) {
            visitor.visitSourcePath(relativePath, path)
          }
        }
      }

      resolver.source.visit(new FileVisitor() {
        @Override
        void visitDir(FileVisitDetails dirDetails) {
          try {
            visitor.visitDir(relativePath, dirDetails)
          }
          catch (Exception ignore) {
          }
        }

        @Override
        void visitFile(FileVisitDetails fileDetails) {
          try {
            visitor.visitFile(relativePath, fileDetails)
          }
          catch (Exception ignore) {
          }
        }
      })
    })
  }
}
