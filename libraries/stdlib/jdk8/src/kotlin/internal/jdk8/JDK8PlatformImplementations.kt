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

@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER", "CANNOT_OVERRIDE_INVISIBLE_MEMBER")
package kotlin.internal.jdk8

import java.util.regex.MatchResult
import java.util.regex.Matcher
import kotlin.internal.jdk7.JDK7PlatformImplementations
import kotlin.random.Random
import kotlin.random.jdk8.PlatformThreadLocalRandom
import kotlin.time.*

internal open class JDK8PlatformImplementations : JDK7PlatformImplementations() {

    // the same SDK version check as in the base class is duplicated here,
    // to avoid having a non-public cross-module dependency
    private object ReflectSdkVersion {
        @JvmField
        public val sdkVersion: Int? = try {
            Class.forName("android.os.Build\$VERSION").getField("SDK_INT").get(null) as? Int
        } catch (e: Throwable) {
            null
        }?.takeIf { it > 0 }
    }

    private fun sdkIsNullOrAtLeast(version: Int): Boolean = ReflectSdkVersion.sdkVersion == null || ReflectSdkVersion.sdkVersion >= version

    override fun getMatchResultNamedGroup(matchResult: MatchResult, name: String): MatchGroup? {
        val matcher = matchResult as? Matcher ?: throw UnsupportedOperationException("Retrieving groups by name is not supported on this platform.")

        val range = matcher.start(name)..matcher.end(name) - 1
        return if (range.start >= 0)
            MatchGroup(matcher.group(name), range)
        else
            null
    }

    override fun defaultPlatformRandom(): Random =
        // while ThreadLocalRandom is available since SDK 21 (as documented), it has bugs in the implementation,
        // so we don't use it for the same reasons as why we don't use it in JDK7. ThreadLocalRandom worked on
        // SDK 24, but starting SDK 25 it had bugs in seeding so that it would return the same sequence of values
        // for all application starts. That will be fixed in SDK 34. Therefore, do not use ThreadLocalRandom until
        // then.
        if (sdkIsNullOrAtLeast(34)) PlatformThreadLocalRandom() else super.defaultPlatformRandom()

    @ExperimentalTime
    override fun getSystemClock(): Clock {
        // java.time.Instant is available since SDK 26
        return if (sdkIsNullOrAtLeast(26)) object : Clock {
            override fun now(): Instant =
                java.time.Instant.now().toKotlinInstant()
        } else object : Clock {
            override fun now(): Instant =
                // After experimenting in Android Studio, it seems like on Android with API < 26, only millisecond precision
                // is available in `Instant.now()` with core library desugaring enabled. Because of that, `currentTimeMillis`
                // is good enough + suggesting that our users enable core library desugaring isn't going to bring any benefits,
                // so the KDoc for [Clock] does not mention any of this.
                Instant.fromEpochMilliseconds(System.currentTimeMillis())
        }
    }
}
