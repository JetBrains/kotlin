import groovy.xml.MarkupBuilder
import org.gradle.api.internal.tasks.testing.TestDescriptorInternal

class KotlinMppTestLogger {
    static def configureTestEventLogging(def task) {
        task.beforeSuite { descriptor -> logTestEvent("beforeSuite", descriptor, null, null) }
        task.afterSuite { descriptor, result -> logTestEvent("afterSuite", descriptor, null, result) }

        task.beforeTest { descriptor -> logTestEvent("beforeTest", descriptor, null, null) }
        task.onOutput { descriptor, event -> logTestEvent("onOutput", descriptor, event, null) }
        task.afterTest { descriptor, result -> logTestEvent("afterTest", descriptor, null, result) }
    }

    static def logTestEvent(testEventType, TestDescriptorInternal testDescriptor, testEvent, testResult) {
        def writer = new StringWriter()
        def xml = new MarkupBuilder(writer)
        xml.event(type: testEventType) {
            test(id: testDescriptor.id, parentId: testDescriptor.parent?.id ?: '') {
                if (testDescriptor) {
                    descriptor(name: testDescriptor.name ?: '', className: testDescriptor.className ?: '')
                }
                if (testEvent) {
                    def message = escapeCdata(testEvent.message)
                    event(destination: testEvent.destination) {
                        xml.mkp.yieldUnescaped("$message")
                    }
                }
                if (testResult) {
                    def errorMsg = escapeCdata(testResult.exception?.message ?: '')
                    def stackTrace = escapeCdata(getStackTrace(testResult.exception))
                    result(resultType: testResult.resultType ?: '', startTime: testResult.startTime, endTime: testResult.endTime) {
                        def exception = testResult.exception
                        if (exception?.message?.trim()) xml.mkp.yieldUnescaped("<errorMsg>$errorMsg</errorMsg>")
                        if (exception) xml.mkp.yieldUnescaped("<stackTrace>$stackTrace</stackTrace>")

                        if ('kotlin.AssertionError'.equals(exception?.class?.name) || exception instanceof AssertionError) {
                            failureType('assertionFailed')
                            return
                        }

                        failureType('error')
                    }
                }
            }
        }

        writeLog(writer.toString())
    }

    static String escapeCdata(String s) {
        return "<![CDATA[" + s?.getBytes("UTF-8")?.encodeBase64()?.toString() + "]]>";
    }

    static def wrap(String s) {
        if(!s) return s;
        s.replaceAll("\r\n|\n\r|\n|\r","<ijLogEol/>\n")
    }

    static def writeLog(s) {
        println String.format("\n<ijLog>%s</ijLog>", wrap(s))
    }

    static def logTestReportLocation(def report) {
        if(!report) return
        def writer = new StringWriter()
        def xml = new MarkupBuilder(writer)
        xml.event(type: 'reportLocation', testReport: report)
        writeLog(writer.toString());
    }

    static def logConfigurationError(aTitle, aMessage, boolean openSettings) {
        def writer = new StringWriter()
        def xml = new MarkupBuilder(writer)
        xml.event(type: 'configurationError', openSettings: openSettings) {
            title(aTitle)
            message(aMessage)
        }
        writeLog(writer.toString());
    }

    static def getStackTrace(Throwable t) {
        if(!t) return ''
        StringWriter sw = new StringWriter()
        t.printStackTrace(new PrintWriter(sw))
        sw.toString()
    }
}

gradle.taskGraph.beforeTask { task ->
    def taskSuperClass = task.class
    while (taskSuperClass != Object.class) {
        if (taskSuperClass.canonicalName == "org.jetbrains.kotlin.gradle.tasks.KotlinTest") {
            try {
                KotlinMppTestLogger.logTestReportLocation(task.reports?.html?.entryPoint?.path)
                KotlinMppTestLogger.configureTestEventLogging(task)
                task.testLogging.showStandardStreams = false
            }
            catch (all) {
                logger.error("", all)
            }
            return
        } else {
            taskSuperClass = taskSuperClass.superclass
        }
    }
}