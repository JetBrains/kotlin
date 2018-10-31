package org.maw.library

import android.support.test.annotation.UiThreadTest
import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4
import android.view.ViewGroup
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see [Testing documentation](http://d.android.com/tools/testing)
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {

    @get:Rule
    var activityTestRule = ActivityTestRule<BlankActivity>(BlankActivity::class.java)


    @UiThreadTest
    @Test
    fun createAndAddView() {

        val container = activityTestRule.activity.findViewById<ViewGroup>(android.R.id.content)
        val testView = TestView(activityTestRule.activity)

        container.addView(testView)

    }
}
