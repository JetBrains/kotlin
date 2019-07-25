import org.gradle.api.Task
import org.gradle.api.tasks.testing.Test

gradle.taskGraph.beforeTask { Task task ->
  if (task instanceof Test) {
    try {
      try {
        def urls = task.classpath.files.findAll{
          it.name == 'idea_rt.jar' || it.name.startsWith('junit')
        }.collect { it.toURI().toURL()}
        def classLoader = Class.forName("org.gradle.launcher.daemon.bootstrap.DaemonMain").getClassLoader()
        if(classLoader instanceof URLClassLoader) {
          for (URL url : urls) {
            classLoader.addURL(url)
          }
        } else {
          logger.error("unable to enhance gradle daemon classloader with idea_rt.jar")
        }
      }
      catch (all) {
        logger.error("unable to enhance gradle daemon classloader with idea_rt.jar", all)
      }

      IJTestEventLogger.logTestReportLocation(task.reports?.html?.entryPoint?.path)
      IJTestEventLogger.configureTestEventLogging(task)
      task.testLogging.showStandardStreams = false
    }
    catch (all) {
      logger.error("", all)
    }
  }
}
