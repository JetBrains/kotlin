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
 * Allows serializing the annotated type even if it is not supported by Parcelize: the parts of the type which cannot be
 * serialized otherwise are written with [android.os.Parcel.writeValue]. For example, a `@RawValue Any` property is written
 * with [android.os.Parcel.writeValue], while a `@RawValue String` property is still written with
 * [android.os.Parcel.writeString], as if it was not annotated.
 *
 * Serialization may fail at runtime, depending on the actual value type.
 */
@Target(AnnotationTarget.TYPE)
@Retention(AnnotationRetention.BINARY)
annotation class RawValue