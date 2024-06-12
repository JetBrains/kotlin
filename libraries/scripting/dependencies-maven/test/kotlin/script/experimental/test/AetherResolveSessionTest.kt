/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.test

import junit.framework.TestCase
import org.apache.maven.settings.Mirror
import org.apache.maven.settings.Server
import org.apache.maven.settings.Settings
import org.eclipse.aether.repository.RemoteRepository
import org.eclipse.aether.util.repository.AuthenticationBuilder
import kotlin.script.experimental.dependencies.maven.impl.AetherResolveSession

class AetherResolveSessionTest : TestCase() {

    private fun sessionFrom(settings: Settings) = AetherResolveSession(settingsFactory = { settings })

    fun testWorksWithEmptySettings() {
        val aether = sessionFrom(Settings())

        val remote = RemoteRepository.Builder("myServer", "default", "http://example.org")
            .build()
        val result = aether.resolveRemote(remote)
        assertEquals("myServer", result.id)
        assertEquals(null, result.authentication)
        assertEquals("http://example.org", result.url)
    }

    fun testMirror() {
        val aether = sessionFrom(Settings().apply {
            mirrors = listOf(Mirror().apply {
                id = "myMirror"
                url = "http://myMirror.org"
                mirrorOf = "*"
            })
        })

        val remote = RemoteRepository.Builder("myServer", "default", "http://example.org")
            .build()
        val result = aether.resolveRemote(remote)
        assertEquals("myMirror", result.id)
        assertEquals(null, result.authentication)
        assertEquals("http://myMirror.org", result.url)
    }

    fun testAuthenticateBasedOnIdWithMirror() {
        val aether = sessionFrom(Settings().apply {
            servers = listOf(
                Server().apply {
                    id = "myIServer"
                    username = "myUsername"
                    password = "myPasswword"
                },
                Server().apply {
                    id = "myMirror"
                    username = "myMirrorUsername"
                    password = "myMirrorPassword"
                },

                )
            mirrors = listOf(Mirror().apply {
                id = "myMirror"
                url = "http://myMirror.org"
                mirrorOf = "*"
            })

        })

        val remote = RemoteRepository.Builder("myServer", "default", "http://example.org")
            .build()
        val result = aether.resolveRemote(remote)
        assertEquals("myMirror", result.id)
        assertEquals("http://myMirror.org", result.url)
        assertEquals(
            AuthenticationBuilder()
                .addUsername("myMirrorUsername")
                .addPassword("myMirrorPassword")
                .build(),
            result.authentication
        )
    }

    fun testDoNotOverwriteCredentials() {
        val aether = sessionFrom(Settings().apply {
            servers = listOf(Server().apply {
                id = "myServer"
                username = "myUsername"
                password = "myPasswword"
            })
        })

        val remote = RemoteRepository.Builder("myServer", "default", "http://example.org")
            .setAuthentication(AuthenticationBuilder().addUsername("a").addPassword("b").build())
            .build()
        val result = aether.resolveRemote(remote)
        assertEquals("myServer", result.id)
        assertEquals("http://example.org", result.url)
        assertEquals(
            AuthenticationBuilder()
                .addUsername("a")
                .addPassword("b")
                .build(),
            result.authentication
        )
    }




}
