// INSPECTION_CLASS: com.android.tools.idea.lint.AndroidLintCommitTransactionInspection

@file:Suppress("UNUSED_VARIABLE")

import android.app.Activity
import android.app.FragmentTransaction
import android.app.FragmentManager
import android.os.Bundle

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //OK
        val transaction = fragmentManager.beginTransaction()
        val transaction2: FragmentTransaction
        transaction2 = fragmentManager.beginTransaction()
        transaction.commit()
        transaction2.commit()

        //WARNING
        val transaction3 = fragmentManager.<warning descr="This transaction should be completed with a `commit()` call">beginTransaction</warning>()

        //OK
        fragmentManager.beginTransaction().commit()
        fragmentManager.beginTransaction().add(null, "A").commit()

        //OK KT-14470
        Runnable {
            val a = fragmentManager.beginTransaction()
            a.commit()
        }
    }

    // KT-14780: Kotlin Lint: "Missing commit() calls" false positive when the result of `commit()` is assigned or used as receiver
    fun testResultOfCommit(fm: FragmentManager) {
        val r1 = fm.beginTransaction().hide(fm.findFragmentByTag("aTag")).commit()
        val r2 = fm.<warning descr="This transaction should be completed with a `commit()` call">beginTransaction</warning>().hide(fm.findFragmentByTag("aTag")).commit().toString()
    }
}
