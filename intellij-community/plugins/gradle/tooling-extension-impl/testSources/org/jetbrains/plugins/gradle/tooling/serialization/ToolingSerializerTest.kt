// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.tooling.serialization

import org.assertj.core.api.Assertions.assertThat
import org.gradle.api.artifacts.Dependency
import org.jeasy.random.EasyRandom
import org.jeasy.random.EasyRandomParameters
import org.jeasy.random.FieldPredicates.*
import org.jeasy.random.api.Randomizer
import org.jetbrains.plugins.gradle.model.DefaultExternalProject
import org.jetbrains.plugins.gradle.model.DefaultExternalProjectDependency
import org.jetbrains.plugins.gradle.model.DefaultGradleExtensions
import org.jetbrains.plugins.gradle.model.ExternalTask
import org.jetbrains.plugins.gradle.model.tests.DefaultExternalTestsModel
import org.jetbrains.plugins.gradle.tooling.internal.BuildScriptClasspathModelImpl
import org.jetbrains.plugins.gradle.tooling.internal.RepositoriesModelImpl
import org.junit.Before
import org.junit.Test
import java.io.File
import java.io.IOException
import java.util.*
import java.util.function.Consumer

/**
 * @author Vladislav.Soroka
 */
class ToolingSerializerTest {
  private lateinit var myRandom: EasyRandom
  private lateinit var myRandomParameters: EasyRandomParameters

  @Before
  fun setUp() {
    myRandomParameters = EasyRandomParameters()
    myRandom = EasyRandom(myRandomParameters)
    myRandomParameters
      .collectionSizeRange(0, 3)
      .objectPoolSize(5)
      .overrideDefaultInitialization(true)
      .scanClasspathForConcreteTypes(true)
      .randomize(File::class.java) { File(myRandom.nextObject(String::class.java)) }
  }


  @Test
  @Throws(Exception::class)
  fun `external project serialization test`() {
    myRandomParameters
      .randomize(named("externalSystemId").and(ofType(String::class.java)).and(inClass(DefaultExternalProject::class.java)),
                 Randomizer { "GRADLE" })
      .randomize(named("configurationName").and(ofType(String::class.java)).and(inClass(DefaultExternalProjectDependency::class.java)),
                 Randomizer { Dependency.DEFAULT_CONFIGURATION })
    doTest(DefaultExternalProject::class.java, Consumer { fixMapsKeys(it) })
  }

  @Test
  @Throws(Exception::class)
  fun `build script classpath serialization test`() {
    doTest(BuildScriptClasspathModelImpl::class.java)
  }

  @Test
  @Throws(Exception::class)
  fun `external test model serialization test`() {
    doTest(DefaultExternalTestsModel::class.java)
  }

  @Test
  @Throws(Exception::class)
  fun `gradle extensions serialization test`() {
    // tasks are not serialized, it's assumed to be populated from ExternalProject model at BaseGradleProjectResolverExtension.populateModuleExtraModels
    myRandomParameters.excludeField(named("tasks").and(ofType(ArrayList::class.java)).and(inClass(DefaultGradleExtensions::class.java)))
    doTest(DefaultGradleExtensions::class.java)
  }

  @Test
  @Throws(Exception::class)
  fun `repositories model serialization test`() {
    doTest(RepositoriesModelImpl::class.java)
  }

  @Throws(IOException::class)
  private fun <T> doTest(modelClazz: Class<T>) {
    doTest(modelClazz, null)
  }

  @Throws(IOException::class)
  private fun <T> doTest(modelClazz: Class<T>, generatedObjectPatcher: Consumer<T>?) {
    val generatedObject = myRandom.nextObject(modelClazz)
    generatedObjectPatcher?.accept(generatedObject)
    val serializer = ToolingSerializer()
    val bytes = serializer.write(generatedObject as Any, modelClazz)
    val deserializedObject = serializer.read(bytes, modelClazz)
    assertThat(deserializedObject).isEqualToComparingFieldByFieldRecursively(generatedObject)
  }

  private fun fixMapsKeys(externalProject: DefaultExternalProject) {
    fixChildProjectsMapsKeys(externalProject, Collections.newSetFromMap(IdentityHashMap()))
  }

  private fun fixChildProjectsMapsKeys(externalProject: DefaultExternalProject, processed: MutableSet<DefaultExternalProject>) {
    if (!processed.add(externalProject)) return
    val sourceSets = externalProject.sourceSets
    for (setsKey in sourceSets.keys.toList()) {
      val sourceSet = sourceSets.remove(setsKey)
      sourceSets[sourceSet!!.name] = sourceSet
    }

    @Suppress("UNCHECKED_CAST")
    val tasks = externalProject.tasks as HashMap<String, ExternalTask>
    for (key in tasks.keys.toList()) {
      val task = tasks.remove(key) as ExternalTask
      tasks[task.name] = task
    }

    @Suppress("UNCHECKED_CAST")
    val projectMap = externalProject.childProjects as TreeMap<String, DefaultExternalProject>
    for (key in projectMap.keys.toList()) {
      val childProject = projectMap.remove(key)
      projectMap[childProject!!.name] = childProject
      fixChildProjectsMapsKeys(childProject, processed)
    }
  }
}