// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.tooling.serialization

import org.assertj.core.api.Assertions.assertThat
import org.gradle.api.artifacts.Dependency
import org.gradle.util.GradleVersion
import org.jeasy.random.EasyRandom
import org.jeasy.random.EasyRandomParameters
import org.jeasy.random.FieldPredicates.*
import org.jeasy.random.api.Randomizer
import org.jetbrains.plugins.gradle.model.DefaultExternalProject
import org.jetbrains.plugins.gradle.model.DefaultExternalProjectDependency
import org.jetbrains.plugins.gradle.model.DefaultGradleExtensions
import org.jetbrains.plugins.gradle.model.ExternalTask
import org.jetbrains.plugins.gradle.model.tests.DefaultExternalTestsModel
import org.jetbrains.plugins.gradle.tooling.internal.AnnotationProcessingModelImpl
import org.jetbrains.plugins.gradle.tooling.internal.BuildScriptClasspathModelImpl
import org.jetbrains.plugins.gradle.tooling.internal.RepositoriesModelImpl
import org.jetbrains.plugins.gradle.tooling.serialization.internal.IdeaProjectSerializationService
import org.jetbrains.plugins.gradle.tooling.serialization.internal.adapter.*
import org.jetbrains.plugins.gradle.tooling.util.GradleVersionComparator
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
      .randomize(
        named("externalSystemId").and(ofType(String::class.java)).and(inClass(DefaultExternalProject::class.java)),
        Randomizer { "GRADLE" }
      )
      .randomize(
        named("configurationName").and(ofType(String::class.java)).and(inClass(DefaultExternalProjectDependency::class.java)),
        Randomizer { Dependency.DEFAULT_CONFIGURATION }
      )
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

  @Test
  @Throws(Exception::class)
  fun `IDEA project serialization test`() {
    val gradleVersion = GradleVersion.version("5.5")
    myRandomParameters
      .randomize(
        ofType(GradleVersionComparator::class.java).and(inClass(InternalIdeaContentRoot::class.java)),
        Randomizer { GradleVersionComparator(gradleVersion) }
      )
      .randomize(
        ofType(InternalProjectIdentifier::class.java),
        Randomizer { InternalProjectIdentifier(InternalBuildIdentifier(File("")), "") }
      )
      .excludeField(named("parent").and(ofType(InternalIdeaProject::class.java)).and(inClass(InternalIdeaModule::class.java)))
      .excludeField(named("parent").and(ofType(InternalGradleProject::class.java)).and(inClass(InternalGradleProject::class.java)))
      // relax InternalBuildIdentifier.rootDir absolute path assertion
      .randomize(File::class.java) { File(myRandom.nextObject(String::class.java)).absoluteFile }
      .excludeField(named("gradleProject").and(inClass(InternalGradleTask::class.java)))

    val serializer = ToolingSerializer()
    serializer.register(IdeaProjectSerializationService(gradleVersion))
    doTest(InternalIdeaProject::class.java, Consumer { ideaProject ->
      val buildIdentifier = InternalBuildIdentifier(myRandom.nextObject(File::class.java))
      ideaProject.children.forEach { ideaModule ->
        ideaModule.parent = ideaProject
        ideaModule.dependencies
          .filter { it?.scope?.scope == null }
          .forEach { it.scope = InternalIdeaDependencyScope.getInstance("Compile") }

        val seenProjects = Collections.newSetFromMap(IdentityHashMap<InternalGradleProject, Boolean>())
        fixGradleProjects(null, ideaModule.gradleProject, seenProjects, buildIdentifier)
      }
    }, serializer)
  }

  @Test
  @Throws(Exception::class)
  fun `annotation processing model serialization test`() {
    doTest(AnnotationProcessingModelImpl::class.java)
  }

  @Throws(IOException::class)
  private fun <T> doTest(modelClazz: Class<T>) {
    doTest(modelClazz, null)
  }

  @Throws(IOException::class)
  private fun <T> doTest(modelClazz: Class<T>, generatedObjectPatcher: Consumer<T>?) {
    doTest(modelClazz, generatedObjectPatcher, ToolingSerializer())
  }

  @Throws(IOException::class)
  private fun <T> doTest(modelClazz: Class<T>,
                         generatedObjectPatcher: Consumer<T>?,
                         serializer: ToolingSerializer) {
    val generatedObject = myRandom.nextObject(modelClazz)
    generatedObjectPatcher?.accept(generatedObject)
    val bytes = serializer.write(generatedObject as Any, modelClazz)
    val deserializedObject = serializer.read(bytes, modelClazz)
    assertThat(deserializedObject).usingRecursiveComparison().isEqualTo(generatedObject)
  }

  companion object {
    private fun fixGradleProjects(parentGradleProject: InternalGradleProject?,
                                  gradleProject: InternalGradleProject,
                                  seenProjects: MutableSet<InternalGradleProject>,
                                  buildIdentifier: InternalBuildIdentifier) {
      if (!seenProjects.add(gradleProject)) return

      gradleProject.projectIdentifier = InternalProjectIdentifier(buildIdentifier, ":" + gradleProject.name)
      gradleProject.tasks.forEach {
        it.setGradleProject(gradleProject)
        if (it.path == null) {
          it.path = ""
        }
      }

      // workaround StackOverflowError for the test assertion
      if (parentGradleProject == null) {
        gradleProject.setChildren(gradleProject.children.take(2))
        gradleProject.children.forEach {
          fixGradleProjects(gradleProject, it, seenProjects, buildIdentifier)
        }
      }
      else {
        gradleProject.parent = parentGradleProject
        gradleProject.setChildren(emptyList())
      }
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

    private fun fixMapsKeys(externalProject: DefaultExternalProject) {
      fixChildProjectsMapsKeys(externalProject, Collections.newSetFromMap(IdentityHashMap()))
    }
  }
}