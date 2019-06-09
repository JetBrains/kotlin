// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.externalSystem.model.createCacheReadConfiguration
import com.intellij.openapi.externalSystem.model.internal.InternalExternalProjectInfo
import com.intellij.openapi.externalSystem.service.project.manage.ExternalProjectsDataStorage
import com.intellij.serialization.VersionedFile
import com.intellij.testFramework.ProjectRule
import org.junit.Test
import java.nio.file.Paths

class CacheFileLoadTest {
  @JvmField
  val projectRule = ProjectRule()

  // open https://github.com/apereo/cas project in IDEA and then copy project.dat from system cache to somewhere
  @Test
  fun testLoad() {
    projectRule.project

    //var versionedFile = VersionedFile(Paths.get("/Volumes/data/Library/Preferences/IJ/system/external_build_system/circlet.28a36c4d/project.dat-text.ion"), ExternalProjectsDataStorage.STORAGE_VERSION)
    var versionedFile = VersionedFile(Paths.get("/Volumes/data/Library/Preferences/IJ/system/external_build_system/quickstart.30660b39/project.dat-text.ion"), ExternalProjectsDataStorage.STORAGE_VERSION)
    var start = System.currentTimeMillis()
    val data = versionedFile.readList(InternalExternalProjectInfo::class.java, createCacheReadConfiguration(logger<CacheFileLoadTest>()), renameToCorruptedOnError = false)!!
    println("Read in ${System.currentTimeMillis() - start}")

    //for (info in data) {
    //  info.externalProjectStructure?.visit {
    //    try {
    //      it.data
    //    }
    //    catch (e: IllegalStateException) {
    //      if (e.cause !is ClassNotFoundException) {
    //        throw e
    //      }
    //    }
    //  }
    //}
    //
    //versionedFile = VersionedFile(Paths.get("/Volumes/data/big-ion2.ion"), ExternalProjectsDataStorage.STORAGE_VERSION, isCompressed = true)
    //
    //start = System.currentTimeMillis()
    //versionedFile.writeList(data, InternalExternalProjectInfo::class.java)
    //println("Write in ${System.currentTimeMillis() - start}")
    //
    //start = System.currentTimeMillis()
    //versionedFile.writeList(data, InternalExternalProjectInfo::class.java)
    //println("Second write in ${System.currentTimeMillis() - start}")
  }
}