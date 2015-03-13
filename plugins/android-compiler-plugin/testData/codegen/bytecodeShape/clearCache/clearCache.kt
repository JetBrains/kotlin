package com.myapp

import android.app.Activity
import android.os.Bundle
import java.io.File
import kotlinx.android.synthetic.*

public class MyActivity : Activity() {
    {clearFindViewByIdCache()}
}

// 5 INVOKEVIRTUAL
// 1 CHECKCAST
// 2  _\$_clearFindViewByIdCache