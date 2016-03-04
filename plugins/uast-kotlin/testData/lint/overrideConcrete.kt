// INSPECTION_CLASS: org.jetbrains.android.inspections.klint.AndroidLintInspectionToolProvider$AndroidKLintOverrideAbstractInspection

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
@Suppress("UsePropertyAccessSyntax", "UNUSED_VARIABLE", "unused", "UNUSED_PARAMETER", "DEPRECATION")
class OverrideConcreteTest2 {
    // OK: This one specifies both methods
    private open class MyNotificationListenerService1 : NotificationListenerService() {
        override fun onNotificationPosted(statusBarNotification: StatusBarNotification) {
        }

        override fun onNotificationRemoved(statusBarNotification: StatusBarNotification) {
        }
    }

    // Error: Misses onNotificationPosted
    private class <error descr="Must override `android.service.notification.NotificationListenerService.onNotificationPosted(android.service.notification.StatusBarNotification)`: Method was abstract until 21, and your `minSdkVersion` is 18">MyNotificationListenerService2</error> : NotificationListenerService() {
        override fun onNotificationRemoved(statusBarNotification: StatusBarNotification) {
        }
    }

    // Error: Misses onNotificationRemoved
    private open class <error descr="Must override `android.service.notification.NotificationListenerService.onNotificationRemoved(android.service.notification.StatusBarNotification)`: Method was abstract until 21, and your `minSdkVersion` is 18">MyNotificationListenerService3</error> : NotificationListenerService() {
        override fun onNotificationPosted(statusBarNotification: StatusBarNotification) {
        }
    }

    // Error: Missing both; wrong signatures (first has wrong arg count, second has wrong type)
    private class <error descr="Must override `android.service.notification.NotificationListenerService.onNotificationPosted(android.service.notification.StatusBarNotification)`: Method was abstract until 21, and your `minSdkVersion` is 18">MyNotificationListenerService4</error> : NotificationListenerService() {
        fun onNotificationPosted(statusBarNotification: StatusBarNotification, flags: Int) {
        }

        fun onNotificationRemoved(statusBarNotification: Int) {
        }
    }

    // OK: Inherits from a class which define both
    private class MyNotificationListenerService5 : MyNotificationListenerService1()

    // OK: Inherits from a class which defines only one, but the other one is defined here
    private class MyNotificationListenerService6 : MyNotificationListenerService3() {
        override fun onNotificationRemoved(statusBarNotification: StatusBarNotification) {
        }
    }

    // Error: Inheriting from a class which only defines one
    private class <error descr="Must override `android.service.notification.NotificationListenerService.onNotificationRemoved(android.service.notification.StatusBarNotification)`: Method was abstract until 21, and your `minSdkVersion` is 18">MyNotificationListenerService7</error> : MyNotificationListenerService3()

    // OK: Has target api setting a local version that is high enough
    @TargetApi(21)
    private class MyNotificationListenerService8 : NotificationListenerService()

    // OK: Suppressed
    @SuppressLint("OverrideAbstract")
    private class MyNotificationListenerService9 : MyNotificationListenerService1()
}