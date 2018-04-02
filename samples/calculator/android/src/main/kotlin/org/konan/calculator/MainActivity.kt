package org.konan.calculator

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.EditText
import android.widget.TextView
import org.konan.arithmeticparser.parseAndCompute

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val resultView = findViewById<TextView>(R.id.computed_result)

        val input = findViewById<EditText>(R.id.input)
        input.setOnEditorActionListener { input, _, _ ->
            val inputText = input.text.toString()
            val result = parseAndCompute(inputText).expression
            with(resultView) {
                text = if (result != null) inputText + " = " + result.toString() else "Unable to parse " + inputText
            }
            true
        }
    }

}