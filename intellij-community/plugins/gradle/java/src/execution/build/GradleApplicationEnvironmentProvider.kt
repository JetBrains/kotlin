// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.execution.build

import com.intellij.codeInsight.daemon.impl.analysis.JavaModuleGraphUtil
import com.intellij.compiler.options.CompileStepBeforeRun
import com.intellij.execution.CantRunException
import com.intellij.execution.ExecutionBundle
import com.intellij.execution.Executor
import com.intellij.execution.ShortenCommandLine
import com.intellij.execution.application.ApplicationConfiguration
import com.intellij.execution.configurations.JavaParameters
import com.intellij.execution.configurations.JavaRunConfigurationModule
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.impl.RunManagerImpl
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.util.ExecutionErrorDialog
import com.intellij.execution.util.JavaParametersUtil
import com.intellij.execution.util.ProgramParametersUtil
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.externalSystem.model.execution.ExternalSystemTaskExecutionSettings
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemRunConfiguration
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.JavaSdkType
import com.intellij.openapi.projectRoots.JavaSdkVersion
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.projectRoots.ex.JavaSdkUtil
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiJavaModule
import com.intellij.task.ExecuteRunConfigurationTask
import com.intellij.util.PathUtil
import gnu.trove.THashMap
import org.jetbrains.plugins.gradle.execution.GradleRunnerUtil
import org.jetbrains.plugins.gradle.service.project.GradleProjectResolverUtil
import org.jetbrains.plugins.gradle.service.task.GradleTaskManager
import org.jetbrains.plugins.gradle.util.GradleConstants

/**
 * @author Vladislav.Soroka
 */
class GradleApplicationEnvironmentProvider : GradleExecutionEnvironmentProvider {

  override fun isApplicable(task: ExecuteRunConfigurationTask): Boolean = task.runProfile is ApplicationConfiguration

  override fun createExecutionEnvironment(project: Project,
                                          executeRunConfigurationTask: ExecuteRunConfigurationTask,
                                          executor: Executor?): ExecutionEnvironment? {
    if (!isApplicable(executeRunConfigurationTask)) return null

    val applicationConfiguration = executeRunConfigurationTask.runProfile as ApplicationConfiguration
    val mainClass = applicationConfiguration.mainClass ?: return null

    val virtualFile = mainClass.containingFile.virtualFile
    val module = ProjectFileIndex.SERVICE.getInstance(project).getModuleForFile(virtualFile) ?: return null

    val params = JavaParameters().apply {
      JavaParametersUtil.configureConfiguration(this, applicationConfiguration)
      this.vmParametersList.addParametersString(applicationConfiguration.vmParameters)
    }

    val javaModuleName: String?
    val javaExePath: String
    try {
      val jdk = JavaParametersUtil.createProjectJdk(project, applicationConfiguration.alternativeJrePath)
                ?: throw RuntimeException(ExecutionBundle.message("run.configuration.error.no.jdk.specified"))
      val type = jdk.sdkType
      if (type !is JavaSdkType) throw RuntimeException(ExecutionBundle.message("run.configuration.error.no.jdk.specified"))
      javaExePath = (type as JavaSdkType).getVMExecutablePath(jdk)?.let {
        FileUtil.toSystemIndependentName(it)
      } ?: throw RuntimeException(ExecutionBundle.message("run.configuration.cannot.find.vm.executable"))
      javaModuleName = findJavaModuleName(jdk, applicationConfiguration.configurationModule, mainClass)
    }
    catch (e: CantRunException) {
      ExecutionErrorDialog.show(e, "Cannot use specified JRE", project)
      throw RuntimeException(ExecutionBundle.message("run.configuration.cannot.find.vm.executable"))
    }

    val taskSettings = ExternalSystemTaskExecutionSettings()
    taskSettings.isPassParentEnvs = params.isPassParentEnvs
    taskSettings.env = if (params.env.isEmpty()) emptyMap() else THashMap(params.env)
    taskSettings.externalSystemIdString = GradleConstants.SYSTEM_ID.id
    val projectPath = GradleRunnerUtil.resolveProjectPath(module)
    taskSettings.externalProjectPath = projectPath
    val runAppTaskName = mainClass.name!! + ".main()"
    taskSettings.taskNames = listOf(runAppTaskName)

    val executorId = executor?.id ?: DefaultRunExecutor.EXECUTOR_ID
    val environment = ExternalSystemUtil.createExecutionEnvironment(project, GradleConstants.SYSTEM_ID, taskSettings, executorId)
                      ?: return null
    val runnerAndConfigurationSettings = environment.runnerAndConfigurationSettings!!
    val gradleRunConfiguration = runnerAndConfigurationSettings.configuration as ExternalSystemRunConfiguration

    val gradlePath = GradleProjectResolverUtil.getGradlePath(module) ?: return null
    val sourceSetName = when {
                          GradleConstants.GRADLE_SOURCE_SET_MODULE_TYPE_KEY == ExternalSystemApiUtil.getExternalModuleType(
                            module) -> GradleProjectResolverUtil.getSourceSetName(module)
                          ModuleRootManager.getInstance(module).fileIndex.isInTestSourceContent(virtualFile) -> "test"
                          else -> "main"
                        } ?: return null

    val initScript = generateInitScript(applicationConfiguration, project, module, params, gradlePath,
                                        runAppTaskName, mainClass, javaExePath, sourceSetName, javaModuleName)
    gradleRunConfiguration.putUserData<String>(GradleTaskManager.INIT_SCRIPT_KEY, initScript)
    gradleRunConfiguration.putUserData<String>(GradleTaskManager.INIT_SCRIPT_PREFIX_KEY, runAppTaskName)

    // reuse all before tasks except 'Make' as it doesn't make sense for delegated run
    gradleRunConfiguration.beforeRunTasks = RunManagerImpl.getInstanceImpl(project).getBeforeRunTasks(applicationConfiguration)
      .filter { it.providerId !== CompileStepBeforeRun.ID }
    return environment
  }

  companion object {
    private val LOG = Logger.getInstance(GradleApplicationEnvironmentProvider::class.java)

    private fun createEscapedParameters(parameters: List<String>, prefix: String): String {
      val result = StringBuilder()
      for (parameter in parameters) {
        if (StringUtil.isEmpty(parameter)) continue
        val escaped = StringUtil.escapeChars(parameter, '\\', '"', '\'')
        result.append(prefix).append(" '").append(escaped).append("'\n")
      }
      return result.toString()
    }

    private fun findJavaModuleName(sdk: Sdk, module: JavaRunConfigurationModule, mainClass: PsiClass): String? {
      return if (JavaSdkUtil.isJdkAtLeast(sdk, JavaSdkVersion.JDK_1_9)) {
        DumbService.getInstance(module.project).computeWithAlternativeResolveEnabled<PsiJavaModule, RuntimeException> {
          JavaModuleGraphUtil.findDescriptorByElement(module.findClass(mainClass.qualifiedName))
        }?.name ?: return null
      }
      else null
    }

    private fun generateInitScript(applicationConfiguration: ApplicationConfiguration,
                                   project: Project,
                                   module: Module,
                                   params: JavaParameters,
                                   gradlePath: String,
                                   runAppTaskName: String,
                                   mainClass: PsiClass,
                                   javaExePath: String,
                                   sourceSetName: String,
                                   javaModuleName: String?): String {
      val workingDir = ProgramParametersUtil.getWorkingDir(applicationConfiguration, project, module)?.let {
        FileUtil.toSystemIndependentName(it)
      }

      val argsString = createEscapedParameters(params.programParametersList.parameters, "args") +
                       createEscapedParameters(params.vmParametersList.parameters, "jvmArgs")

      val useManifestJar = applicationConfiguration.shortenCommandLine === ShortenCommandLine.MANIFEST
      val useArgsFile = applicationConfiguration.shortenCommandLine === ShortenCommandLine.ARGS_FILE
      var useClasspathFile = applicationConfiguration.shortenCommandLine === ShortenCommandLine.CLASSPATH_FILE
      var intelliJRtPath: String? = null
      if (useClasspathFile) {
        try {
          intelliJRtPath = PathUtil.getCanonicalPath(
            PathManager.getJarPathForClass(Class.forName("com.intellij.rt.execution.CommandLineWrapper")))
        }
        catch (t: Throwable) {
          LOG.warn("Unable to use classpath file", t)
          useClasspathFile = false
        }

      }

      // @formatter:off
      @Suppress("UnnecessaryVariable")
//      @Language("Groovy")
      val initScript = """
    def gradlePath = '$gradlePath'
    def runAppTaskName = '$runAppTaskName'
    def mainClass = '${mainClass.qualifiedName}'
    def javaExePath = '$javaExePath'
    def _workingDir = ${if (workingDir.isNullOrEmpty()) "null\n" else "'$workingDir'\n"}
    def sourceSetName = '$sourceSetName'
    def javaModuleName = ${if (javaModuleName == null) "null\n" else "'$javaModuleName'\n"}
    ${if (useManifestJar) "gradle.addListener(new ManifestTaskActionListener(runAppTaskName))\n" else ""}
    ${if (useArgsFile) "gradle.addListener(new ArgFileTaskActionListener(runAppTaskName))\n" else ""}
    ${if (useClasspathFile && intelliJRtPath != null) "gradle.addListener(new ClasspathFileTaskActionListener(runAppTaskName, mainClass, '$intelliJRtPath'))\n " else ""}

    allprojects {
      afterEvaluate { project ->
        if(project.path == gradlePath && project?.convention?.findPlugin(JavaPluginConvention)) {
          def overwrite = project.tasks.findByName(runAppTaskName) != null
          project.tasks.create(name: runAppTaskName, overwrite: overwrite, type: JavaExec) {
            if (javaExePath) executable = javaExePath
            classpath = project.sourceSets[sourceSetName].runtimeClasspath
            main = mainClass
            $argsString
            if(_workingDir) workingDir = _workingDir
            standardInput = System.in
            if(javaModuleName) {
              inputs.property('moduleName', javaModuleName)
              doFirst {
                jvmArgs += [
                  '--module-path', classpath.asPath,
                  '--module', javaModuleName + '/' + mainClass
                ]
                classpath = files()
              }
            }
          }
        }
      }
    }
    """ + (if (useManifestJar || useArgsFile || useClasspathFile) """
    import org.gradle.api.execution.TaskActionListener
    import org.gradle.api.Task
    import org.gradle.api.tasks.JavaExec
    abstract class RunAppTaskActionListener implements TaskActionListener {
      String myTaskName
      File myClasspathFile
      RunAppTaskActionListener(String taskName) {
        myTaskName = taskName
      }
      void beforeActions(Task task) {
        if(!(task instanceof JavaExec) || task.name != myTaskName) return
        myClasspathFile = patchTaskClasspath(task)
      }
      void afterActions(Task task) {
        if(myClasspathFile != null) { myClasspathFile.delete() }
      }
      abstract File patchTaskClasspath(JavaExec task)
    }
    """ else "") + (if (useManifestJar) """

    import org.gradle.api.tasks.JavaExec
    import java.util.jar.Attributes
    import java.util.jar.JarOutputStream
    import java.util.jar.Manifest
    import java.util.zip.ZipEntry
    class ManifestTaskActionListener extends RunAppTaskActionListener {
      ManifestTaskActionListener(String taskName) {
         super(taskName)
      }
      File patchTaskClasspath(JavaExec task) {
        Manifest manifest = new Manifest()
        Attributes attributes = manifest.getMainAttributes()
        attributes.put(Attributes.Name.MANIFEST_VERSION, '1.0')
        attributes.putValue('Class-Path', task.classpath.files.collect {it.toURI().toURL().toString()}.join(' '))
        File file = File.createTempFile('generated-', '-manifest')
        def oStream = new JarOutputStream(new FileOutputStream(file), manifest)
        oStream.putNextEntry(new ZipEntry('META-INF/'))
        oStream.close()
        task.classpath = task.project.files(file.getAbsolutePath())
        return file
      }
    }
    """ else "") + (if (useArgsFile) """

    import org.gradle.api.tasks.JavaExec
    import org.gradle.process.CommandLineArgumentProvider
    class ArgFileTaskActionListener extends RunAppTaskActionListener {
      ArgFileTaskActionListener(String taskName) {
         super(taskName)
      }
      File patchTaskClasspath(JavaExec task) {
        File file = File.createTempFile('generated-', '-argFile')
        def writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), 'UTF-8'))
        def lineSep = System.getProperty('line.separator')
        writer.print('-classpath' + lineSep)
        writer.print(quoteArg(task.classpath.asPath))
        writer.print(lineSep)
        writer.close()
        task.jvmArgs('@' + file.absolutePath)
        task.classpath = task.project.files([])
        return file
      }
      private static String quoteArg(String arg) {
        String specials = ' #\'\"\n\r\t\f'
        if (specials.find { arg.indexOf(it) != -1 } == null) return arg
        StringBuilder sb = new StringBuilder(arg.length() * 2)
        for (int i = 0; i < arg.length(); i++) {
          char c = arg.charAt(i)
          if (c == ' ' as char || c == '#' as char || c == '\'' as char) sb.append('"').append(c).append('"')
          else if (c == '"' as char) sb.append("\"\\\"\"")
          else if (c == '\n' as char) sb.append("\"\\n\"")
          else if (c == '\r' as char) sb.append("\"\\r\"")
          else if (c == '\t' as char) sb.append("\"\\t\"")
          else if (c == '\f' as char) sb.append("\"\\f\"")
          else sb.append(c)
        }
        return sb.toString()
      }}
    """ else "") + if (useClasspathFile) """

    import org.gradle.api.tasks.JavaExec
    import org.gradle.process.CommandLineArgumentProvider
    class ClasspathFileTaskActionListener extends RunAppTaskActionListener {
      String myMainClass
      String myIntelliJRtPath
      ClasspathFileTaskActionListener(String taskName, String mainClass, String intelliJRtPath) {
         super(taskName)
         myMainClass = mainClass
         myIntelliJRtPath = intelliJRtPath
      }
      File patchTaskClasspath(JavaExec task) {
        File file = File.createTempFile('generated-', '-classpathFile')
        def writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), 'UTF-8'))
        task.classpath.files.each { writer.println(it.path) }
        writer.close()
        List args = [file.absolutePath, myMainClass] as List
        args.addAll(task.args)
        task.args = []
        task.argumentProviders.add({ return args } as CommandLineArgumentProvider)
        task.main = 'com.intellij.rt.execution.CommandLineWrapper'
        task.classpath = task.project.files([myIntelliJRtPath])
        return file
      }
    }
    """ else ""
      // @formatter:on
      return initScript
    }
  }
}
