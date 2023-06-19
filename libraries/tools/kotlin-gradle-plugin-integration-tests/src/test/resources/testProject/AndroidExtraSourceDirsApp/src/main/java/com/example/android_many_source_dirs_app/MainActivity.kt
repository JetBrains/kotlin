package com.example.android_many_source_dirs_app

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android_many_source_dirs_app.NotesApplication

public class MainActivity : AppCompatActivity() {

    private lateinit var notesApp: NotesApplication

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }
}