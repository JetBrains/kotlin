// INSPECTION_CLASS: org.jetbrains.android.inspections.klint.AndroidLintInspectionToolProvider$AndroidKLintShortAlarmInspection

import android.app.AlarmManager

@Suppress("UsePropertyAccessSyntax", "UNUSED_VARIABLE", "unused", "UNUSED_PARAMETER", "DEPRECATION")
class TestAlarm {
    fun test(alarmManager: AlarmManager) {
        alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME, 5000, 60000, null); // OK
        alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME, 6000, 70000, null); // OK
        alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME, <warning descr="Value will be forced up to 5000 as of Android 5.1; don't rely on this to be exact">50</warning>, <warning descr="Value will be forced up to 60000 as of Android 5.1; don't rely on this to be exact">10</warning>, null); // ERROR

        alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME, 5000,  // ERROR
                                  <warning descr="Value will be forced up to 60000 as of Android 5.1; don't rely on this to be exact">OtherClass.MY_INTERVAL</warning>, null);                          // ERROR

        val interval = 10;
        val interval2 = 2L * interval;
        alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME, 5000, <warning descr="Value will be forced up to 60000 as of Android 5.1; don't rely on this to be exact">interval2</warning>, null); // ERROR
    }

    private object OtherClass {
        val MY_INTERVAL = 1000L;
    }
}