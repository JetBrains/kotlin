package org.example.manyvariants

import android.app.Activity
import kotlinx.android.synthetic.full.activity_full.*
import kotlinx.android.synthetic.fullRelease.activity_full_release.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.release.activity_release.*

fun Activity.fullRelease() {
    viewMain
    viewFull
    viewRelease
    viewFullRelease
}
