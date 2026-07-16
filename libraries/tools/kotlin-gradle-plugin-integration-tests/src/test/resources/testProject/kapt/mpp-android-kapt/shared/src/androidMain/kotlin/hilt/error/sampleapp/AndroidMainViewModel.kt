package hilt.error.sampleapp

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/***
 * This is just a standard HiltViewModel. This works as intended
 */
@HiltViewModel
class AndroidMainViewModel @Inject constructor(
    dependency: AndroidDependency
) : ViewModel() {

    val text: String = dependency.text
}

class AndroidDependency @Inject constructor() {
    val text = "Hello AndroidMain"
}