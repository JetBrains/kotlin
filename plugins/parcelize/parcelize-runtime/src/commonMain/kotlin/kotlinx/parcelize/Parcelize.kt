/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kotlinx.parcelize

/**
 * Instructs the Kotlin compiler to generate `writeToParcel()`, `describeContents()` [android.os.Parcelable] methods,
 * as well as a `CREATOR` factory class automatically.
 *
 * The annotation is applicable only to classes that implements [android.os.Parcelable] (directly or indirectly).
 * Note that only the primary constructor properties will be serialized.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class Parcelize