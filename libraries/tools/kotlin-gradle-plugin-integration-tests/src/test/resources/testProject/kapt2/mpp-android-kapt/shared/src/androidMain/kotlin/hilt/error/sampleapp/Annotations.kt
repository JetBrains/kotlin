@file:OptIn(ExperimentalMultiplatform::class)

package hilt.error.sampleapp

/**
 * Issue: prior to 1.9 this would be fine. Now, Hilt cannot find viewmodels marked with this typealias
 */
actual typealias HiltViewModel = dagger.hilt.android.lifecycle.HiltViewModel

actual typealias ViewModel = androidx.lifecycle.ViewModel

actual typealias Inject = javax.inject.Inject

actual typealias AutoCloseable = java.lang.AutoCloseable
actual typealias Closeable = java.io.Closeable