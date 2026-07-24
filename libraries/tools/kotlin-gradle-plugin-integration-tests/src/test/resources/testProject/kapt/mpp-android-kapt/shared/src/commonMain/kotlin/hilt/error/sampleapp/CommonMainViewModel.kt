package hilt.error.sampleapp

/**
 * This is a shared view model. It is marked with a typealias to a HiltViewModel
 * But does not get added to Hilts view model map (from kotlin 1.90 onwards)
 */
@HiltViewModel
class CommonMainViewModel @Inject constructor(
    dependency: CommonMainDependency
) : ViewModel() {

    val text = dependency.text
}

class CommonMainDependency @Inject constructor() {
    val text = "Hello CommonMain"
}