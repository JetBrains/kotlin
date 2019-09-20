/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package com.intellij.configurationStore

import com.intellij.configurationStore.xml.XmlElementStorageTest
import org.junit.runner.RunWith
import org.junit.runners.Suite

// All in package is very slow, so, we have to use Suite to speedup
@RunWith(Suite::class)
@Suite.SuiteClasses(
  ApplicationStoreTest::class,
  ProjectStoreTest::class, DefaultProjectStoreTest::class,
  ModuleStoreTest::class, ModuleStoreRenameTest::class,
  StorageManagerTest::class,
  SchemeManagerTest::class,
  XmlElementStorageTest::class,
  DirectoryBasedStorageTest::class
)
class StoreTestSuite