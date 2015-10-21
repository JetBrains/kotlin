package com.myapp

import android.app.Activity
import android.app.Fragment
import android.os.Bundle
import java.io.File
import kotlinx.android.synthetic.main.layout.*

public class MyActivity : Activity() {
    init { fragm }
}

public class MyFragment : Fragment() {
    init { fragm }
}

// 1 INVOKEVIRTUAL android/app/Activity\.getFragmentManager
// 1 INVOKEVIRTUAL android/app/Fragment\.getFragmentManager
// 2 GETSTATIC com/myapp/R\$id\.fragm
// 2 INVOKEVIRTUAL android/app/FragmentManager\.findFragmentById
// 2 CHECKCAST android/app/Fragment