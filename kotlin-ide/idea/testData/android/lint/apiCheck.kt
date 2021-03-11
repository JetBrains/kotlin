
// INSPECTION_CLASS: com.android.tools.idea.lint.AndroidLintNewApiInspection
// INSPECTION_CLASS2: com.android.tools.idea.lint.AndroidLintInlinedApiInspection
// INSPECTION_CLASS3: com.android.tools.idea.lint.AndroidLintOverrideInspection

import android.animation.RectEvaluator
import android.annotation.SuppressLint
import android.annotation.TargetApi
import org.w3c.dom.DOMError
import org.w3c.dom.DOMErrorHandler
import org.w3c.dom.DOMLocator

import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.app.Activity
import android.app.ApplicationErrorReport
import android.graphics.drawable.VectorDrawable
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.Rect
import android.os.Build
import android.widget.*
import dalvik.bytecode.OpcodeInfo

import android.os.Build.VERSION
import <warning descr="Field requires API level 4 (current min is 1): `android.os.Build.VERSION#SDK_INT`">android.os.Build.VERSION.SDK_INT</warning>
import android.os.Build.VERSION_CODES
import android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH
import android.os.Build.VERSION_CODES.JELLY_BEAN
import android.os.Bundle
import android.os.Parcelable
import android.system.ErrnoException
import android.widget.TextView

@Suppress("SENSELESS_COMPARISON", "UNUSED_EXPRESSION", "UsePropertyAccessSyntax", "UNUSED_VARIABLE", "unused", "UNUSED_PARAMETER", "DEPRECATION", "USELESS_CAST")
class ApiCallTest: Activity() {

    fun method(chronometer: Chronometer, locator: DOMLocator) {
        chronometer.<error descr="Call requires API level 16 (current min is 1): android.view.View#setBackground">setBackground</error>(null)

        // Ok
        Bundle().getInt("")

        <warning descr="Field requires API level 16 (current min is 1): `android.view.View#SYSTEM_UI_FLAG_FULLSCREEN`">View.SYSTEM_UI_FLAG_FULLSCREEN</warning>

        // Virtual call
        <error descr="Call requires API level 11 (current min is 1): android.app.Activity#getActionBar">getActionBar</error>() // API 11
        <error descr="Call requires API level 11 (current min is 1): android.app.Activity#getActionBar">actionBar</error> // API 11

        // Class references (no call or field access)
        val error: DOMError? = null // API 8
        val clz = DOMErrorHandler::class // API 8

        // Method call
        chronometer.<error descr="Call requires API level 3 (current min is 1): android.widget.Chronometer#getOnChronometerTickListener">onChronometerTickListener</error> // API 3

        // Inherited method call (from TextView
        chronometer.<error descr="Call requires API level 11 (current min is 1): android.widget.TextView#setTextIsSelectable">setTextIsSelectable</error>(true) // API 11

        GridLayout::class

        // Field access
        val field = <warning descr="Field requires API level 11 (current min is 1): `dalvik.bytecode.OpcodeInfo#MAXIMUM_VALUE`">OpcodeInfo.MAXIMUM_VALUE</warning> // API 11


        val fillParent = LayoutParams.FILL_PARENT // API 1
        // This is a final int, which means it gets inlined
        val matchParent = LayoutParams.MATCH_PARENT // API 8
        // Field access: non final
        val batteryInfo = <error descr="Field requires API level 14 (current min is 1): `android.app.ApplicationErrorReport#batteryInfo`">report!!.batteryInfo</error>

        // Enum access
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP) {
            val mode = <error descr="Field requires API level 11 (current min is 1): `android.graphics.PorterDuff.Mode#OVERLAY`">PorterDuff.Mode.OVERLAY</error> // API 11
        }
    }

    fun test(rect: Rect) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            RectEvaluator(rect); // OK
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (rect != null) {
                RectEvaluator(rect); // OK
            }
        }
    }

    fun test2(rect: Rect) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            RectEvaluator(rect); // OK
        }
    }

    fun test3(rect: Rect) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            <error descr="Call requires API level 18 (current min is 1): new android.animation.RectEvaluator">RectEvaluator()</error>; // ERROR
        }
    }

    fun test4(rect: Rect) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            System.out.println("Something");
            RectEvaluator(rect); // OK
        } else {
            <error descr="Call requires API level 21 (current min is 1): new android.animation.RectEvaluator">RectEvaluator(rect)</error>; // ERROR
        }
    }

    fun test5(rect: Rect) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.CUPCAKE) {
            <error descr="Call requires API level 21 (current min is 1): new android.animation.RectEvaluator">RectEvaluator(rect)</error>; // ERROR
        } else {
            <error descr="Call requires API level 21 (current min is 1): new android.animation.RectEvaluator">RectEvaluator(rect)</error>; // ERROR
        }
    }

    fun test(priority: Boolean, layout: ViewGroup) {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            GridLayout(null).getOrientation(); // Not flagged
        } else {
            <error descr="Call requires API level 14 (current min is 1): new android.widget.GridLayout">GridLayout(null)</error>.<error descr="Call requires API level 14 (current min is 1): android.widget.GridLayout#getOrientation">getOrientation</error>(); // Flagged
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            GridLayout(null).getOrientation(); // Not flagged
        } else {
            <error descr="Call requires API level 14 (current min is 1): new android.widget.GridLayout">GridLayout(null)</error>.<error descr="Call requires API level 14 (current min is 1): android.widget.GridLayout#getOrientation">getOrientation</error>(); // Flagged
        }

        if (SDK_INT >= ICE_CREAM_SANDWICH) {
            GridLayout(null).getOrientation(); // Not flagged
        } else {
            <error descr="Call requires API level 14 (current min is 1): new android.widget.GridLayout">GridLayout(null)</error>.<error descr="Call requires API level 14 (current min is 1): android.widget.GridLayout#getOrientation">getOrientation</error>(); // Flagged
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            GridLayout(null).getOrientation(); // Not flagged
        } else {
            <error descr="Call requires API level 14 (current min is 1): new android.widget.GridLayout">GridLayout(null)</error>.<error descr="Call requires API level 14 (current min is 1): android.widget.GridLayout#getOrientation">getOrientation</error>(); // Flagged
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            <error descr="Call requires API level 14 (current min is 1): new android.widget.GridLayout">GridLayout(null)</error>.<error descr="Call requires API level 14 (current min is 1): android.widget.GridLayout#getOrientation">getOrientation</error>(); // Flagged
        } else {
            GridLayout(null).getOrientation(); // Not flagged
        }

        if (Build.VERSION.SDK_INT >= 14) {
            GridLayout(null).getOrientation(); // Not flagged
        } else {
            <error descr="Call requires API level 14 (current min is 1): new android.widget.GridLayout">GridLayout(null)</error>.<error descr="Call requires API level 14 (current min is 1): android.widget.GridLayout#getOrientation">getOrientation</error>(); // Flagged
        }

        if (VERSION.SDK_INT >= VERSION_CODES.ICE_CREAM_SANDWICH) {
            GridLayout(null).getOrientation(); // Not flagged
        } else {
            <error descr="Call requires API level 14 (current min is 1): new android.widget.GridLayout">GridLayout(null)</error>.<error descr="Call requires API level 14 (current min is 1): android.widget.GridLayout#getOrientation">getOrientation</error>(); // Flagged
        }

        // Nested conditionals
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            if (priority) {
                <error descr="Call requires API level 14 (current min is 1): new android.widget.GridLayout">GridLayout(null)</error>.<error descr="Call requires API level 14 (current min is 1): android.widget.GridLayout#getOrientation">getOrientation</error>(); // Flagged
            } else {
                <error descr="Call requires API level 14 (current min is 1): new android.widget.GridLayout">GridLayout(null)</error>.<error descr="Call requires API level 14 (current min is 1): android.widget.GridLayout#getOrientation">getOrientation</error>(); // Flagged
            }
        } else {
            <error descr="Call requires API level 14 (current min is 1): new android.widget.GridLayout">GridLayout(null)</error>.<error descr="Call requires API level 14 (current min is 1): android.widget.GridLayout#getOrientation">getOrientation</error>(); // Flagged
        }

        // Nested conditionals 2
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            if (priority) {
                GridLayout(null).getOrientation(); // Not flagged
            } else {
                GridLayout(null).getOrientation(); // Not flagged
            }
        } else {
            <error descr="Call requires API level 14 (current min is 1): new android.widget.GridLayout">GridLayout(null)</error>; // Flagged
        }
    }

    fun test2(priority: Boolean) {
        if (android.os.Build.VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN) {
            GridLayout(null).getOrientation(); // Not flagged
        } else {
            <error descr="Call requires API level 14 (current min is 1): new android.widget.GridLayout">GridLayout(null)</error>; // Flagged
        }

        if (android.os.Build.VERSION.SDK_INT >= 16) {
            GridLayout(null).getOrientation(); // Not flagged
        } else {
            <error descr="Call requires API level 14 (current min is 1): new android.widget.GridLayout">GridLayout(null)</error>; // Flagged
        }

        if (android.os.Build.VERSION.SDK_INT >= 13) {
            <error descr="Call requires API level 14 (current min is 1): new android.widget.GridLayout">GridLayout(null)</error>.<error descr="Call requires API level 14 (current min is 1): android.widget.GridLayout#getOrientation">getOrientation</error>(); // Flagged
        } else {
            <error descr="Call requires API level 14 (current min is 1): new android.widget.GridLayout">GridLayout(null)</error>; // Flagged
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            GridLayout(null).getOrientation(); // Not flagged
        } else {
            <error descr="Call requires API level 14 (current min is 1): new android.widget.GridLayout">GridLayout(null)</error>; // Flagged
        }

        if (SDK_INT >= JELLY_BEAN) {
            GridLayout(null).getOrientation(); // Not flagged
        } else {
            <error descr="Call requires API level 14 (current min is 1): new android.widget.GridLayout">GridLayout(null)</error>; // Flagged
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            GridLayout(null).getOrientation(); // Not flagged
        } else {
            <error descr="Call requires API level 14 (current min is 1): new android.widget.GridLayout">GridLayout(null)</error>; // Flagged
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            <error descr="Call requires API level 14 (current min is 1): new android.widget.GridLayout">GridLayout(null)</error>; // Flagged
        } else {
            GridLayout(null).getOrientation(); // Not flagged
        }

        if (Build.VERSION.SDK_INT >= 16) {
            GridLayout(null).getOrientation(); // Not flagged
        } else {
            <error descr="Call requires API level 14 (current min is 1): new android.widget.GridLayout">GridLayout(null)</error>; // Flagged
        }

        if (VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN) {
            GridLayout(null).getOrientation(); // Not flagged
        } else {
            <error descr="Call requires API level 14 (current min is 1): new android.widget.GridLayout">GridLayout(null)</error>; // Flagged
        }
    }

    fun test(textView: TextView) {
        if (textView.<error descr="Call requires API level 14 (current min is 1): android.widget.TextView#isSuggestionsEnabled">isSuggestionsEnabled</error>()) {
            //ERROR
        }
        if (textView.<error descr="Call requires API level 14 (current min is 1): android.widget.TextView#isSuggestionsEnabled">isSuggestionsEnabled</error>) {
            //ERROR
        }

        if (SDK_INT >= JELLY_BEAN && textView.isSuggestionsEnabled()) {
            //NO ERROR
        }

        if (SDK_INT >= JELLY_BEAN && textView.isSuggestionsEnabled) {
            //NO ERROR
        }

        if (SDK_INT >= JELLY_BEAN && (textView.text != "" || textView.isSuggestionsEnabled)) {
            //NO ERROR
        }

        if (SDK_INT < JELLY_BEAN && (textView.text != "" || textView.<error descr="Call requires API level 14 (current min is 1): android.widget.TextView#isSuggestionsEnabled">isSuggestionsEnabled</error>)) {
            //ERROR
        }

        if (SDK_INT < JELLY_BEAN && textView.<error descr="Call requires API level 14 (current min is 1): android.widget.TextView#isSuggestionsEnabled">isSuggestionsEnabled</error>()) {
            //ERROR
        }

        if (SDK_INT < JELLY_BEAN && textView.<error descr="Call requires API level 14 (current min is 1): android.widget.TextView#isSuggestionsEnabled">isSuggestionsEnabled</error>) {
            //ERROR
        }

        if (SDK_INT < JELLY_BEAN || textView.isSuggestionsEnabled) {
            //NO ERROR
        }

        if (SDK_INT > JELLY_BEAN || textView.<error descr="Call requires API level 14 (current min is 1): android.widget.TextView#isSuggestionsEnabled">isSuggestionsEnabled</error>) {
            //ERROR
        }


        // getActionBar() API 11
        if (SDK_INT <= 10 || getActionBar() == null) {
            //NO ERROR
        }

        if (SDK_INT < 10 || <error descr="Call requires API level 11 (current min is 1): android.app.Activity#getActionBar">getActionBar</error>() == null) {
            //ERROR
        }

        if (SDK_INT < 11 || getActionBar() == null) {
            //NO ERROR
        }

        if (SDK_INT != 11 || <error descr="Call requires API level 11 (current min is 1): android.app.Activity#getActionBar">getActionBar</error>() == null) {
            //NO ERROR
        }

        if (SDK_INT != 12 || <error descr="Call requires API level 11 (current min is 1): android.app.Activity#getActionBar">getActionBar</error>() == null) {
            //ERROR
        }

        if (SDK_INT <= 11 || getActionBar() == null) {
            //NO ERROR
        }

        if (SDK_INT < 12 || getActionBar() == null) {
            //NO ERROR
        }

        if (SDK_INT <= 12 || getActionBar() == null) {
            //NO ERROR
        }

        if (SDK_INT < 9 || <error descr="Call requires API level 11 (current min is 1): android.app.Activity#getActionBar">getActionBar</error>() == null) {
            //ERROR
        }

        if (SDK_INT <= 9 || <error descr="Call requires API level 11 (current min is 1): android.app.Activity#getActionBar">getActionBar</error>() == null) {
            //ERROR
        }
    }

    fun testReturn() {
        if (SDK_INT < 11) {
            return
        }

        // No Error
        val actionBar = getActionBar()
    }

    fun testThrow() {
        if (SDK_INT < 11) {
            throw IllegalStateException()
        }

        // No Error
        val actionBar = <error descr="Call requires API level 11 (current min is 1): android.app.Activity#getActionBar">getActionBar</error>()
    }

    fun testError() {
        if (SDK_INT < 11) {
            error("Api")
        }

        // No Error
        val actionBar = <error descr="Call requires API level 11 (current min is 1): android.app.Activity#getActionBar">getActionBar</error>()
    }

    fun testWithoutAnnotation(textView: TextView) {
        if (textView.<error descr="Call requires API level 14 (current min is 1): android.widget.TextView#isSuggestionsEnabled">isSuggestionsEnabled</error>()) {

        }

        if (textView.<error descr="Call requires API level 14 (current min is 1): android.widget.TextView#isSuggestionsEnabled">isSuggestionsEnabled</error>) {

        }
    }

    @TargetApi(JELLY_BEAN)
    fun testWithTargetApiAnnotation(textView: TextView) {
        if (textView.isSuggestionsEnabled()) {
            //NO ERROR, annotation
        }

        if (textView.isSuggestionsEnabled) {
            //NO ERROR, annotation
        }
    }

    @SuppressLint("NewApi")
    fun testWithSuppressLintAnnotation(textView: TextView) {
        if (textView.isSuggestionsEnabled()) {
            //NO ERROR, annotation
        }

        if (textView.isSuggestionsEnabled) {
            //NO ERROR, annotation
        }
    }

    fun testCatch() {
        try {

        } catch (e: <error descr="Class requires API level 21 (current min is 1): android.system.ErrnoException">ErrnoException</error>) {

        }
    }

    fun testOverload() {
        // this overloaded addOval available only on API Level 21
        Path().<error descr="Call requires API level 21 (current min is 1): android.graphics.Path#addOval">addOval</error>(0f, 0f, 0f, 0f, Path.Direction.CW)
    }

    // KT-14737 False error with short-circuit evaluation
    fun testShortCircuitEvaluation() {
        <error descr="Call requires API level 21 (current min is 1): android.content.Context#getDrawable">getDrawable</error>(0) // error here as expected
        if(Build.VERSION.SDK_INT >= 23
           && null == getDrawable(0)) // error here should not occur
        {
            getDrawable(0) // no error here as expected
        }
    }

    // KT-1482 Kotlin Lint: "Calling new methods on older versions" does not report call on receiver in extension function
    private fun Bundle.caseE1a() { <error descr="Call requires API level 18 (current min is 1): android.os.Bundle#getBinder">getBinder</error>("") }

    private fun Bundle.caseE1c() { this.<error descr="Call requires API level 18 (current min is 1): android.os.Bundle#getBinder">getBinder</error>("") }

    private fun caseE1b(bundle: Bundle) { bundle.<error descr="Call requires API level 18 (current min is 1): android.os.Bundle#getBinder">getBinder</error>("") }

    // KT-12023 Kotlin Lint: Cast doesn't trigger minSdk error
    fun testCast(layout: ViewGroup) {
        if (layout is LinearLayout) {}  // OK API 1
        layout as? LinearLayout         // OK API 1
        layout as LinearLayout          // OK API 1

        if (layout !is <error descr="Class requires API level 14 (current min is 1): android.widget.GridLayout">GridLayout</error>) {}
        layout as? <error descr="Class requires API level 14 (current min is 1): android.widget.GridLayout">GridLayout</error>
        layout as <error descr="Class requires API level 14 (current min is 1): android.widget.GridLayout">GridLayout</error>

        val grid = layout as? <error descr="Class requires API level 14 (current min is 1): android.widget.GridLayout">GridLayout</error>
        val linear = layout as LinearLayout // OK API 1
    }

    abstract class ErrorVectorDravable : <error descr="Call requires API level 21 (current min is 1): new android.graphics.drawable.VectorDrawable"><error descr="Class requires API level 21 (current min is 1): android.graphics.drawable.VectorDrawable">VectorDrawable</error>()</error>, Parcelable

    @TargetApi(21)
    class MyVectorDravable : VectorDrawable()

    fun testTypes() {
        <error descr="Call requires API level 14 (current min is 1): new android.widget.GridLayout">GridLayout(this)</error>
        val c = VectorDrawable::class.java
    }

    fun testCallWithApiAnnotation(textView: TextView) {
        MyVectorDravable()
        testWithTargetApiAnnotation(textView)
    }

    companion object : Activity() {
        fun test() {
            <error descr="Call requires API level 21 (current min is 1): android.content.Context#getDrawable">getDrawable</error>(0)
        }
    }

    // Return type
    internal // API 14
    val gridLayout: GridLayout?
        get() = null

    private val report: ApplicationErrorReport?
        get() = null
}

object O: Activity() {
    fun test() {
        <error descr="Call requires API level 21 (current min is 1): android.content.Context#getDrawable">getDrawable</error>(0)
    }
}

fun testJava8() {
    // Error, Api 24, Java8
    mutableListOf(1, 2, 3).<error descr="Call requires API level 24 (current min is 1): java.util.Collection#removeIf">removeIf</error> {
        true
    }

    // Ok, Kotlin
    mutableListOf(1, 2, 3).removeAll {
        true
    }

    // Error, Api 24, Java8
    mapOf(1 to 2).forEach { key, value -> key + value }

    // Ok, Kotlin
    mapOf(1 to 2).forEach { (key, value) -> key + value }
}

interface WithDefault {
    // Should be ok
    fun methodWithBody() {
        return
    }
}