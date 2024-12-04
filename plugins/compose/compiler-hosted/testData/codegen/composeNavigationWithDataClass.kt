// DUMP_IR

// MODULE: navigation.runtime
// MODULE_KIND: LibraryBinary
// FILE: androidx/navigation/NavHostController.kt
package androidx.navigation

@DslMarker
public annotation class NavDestinationDsl

@NavDestinationDsl
public open class NavGraphBuilder

public open class NavHostController

public class NavBackStackEntry

// MODULE: navigation.compose(navigation.runtime)
// MODULE_KIND: LibraryBinary
// FILE: androidx/navigation/compose/NavHost.kt
package androidx.navigation.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavGraphBuilder

@Composable
public fun NavHost(
    navController: NavHostController,
    startDestination: String,
    modifier: Modifier = Modifier,
    builder: NavGraphBuilder.() -> Unit
) {}

// FILE: androidx/navigation/compose/NavGraphBuilder.kt
package androidx.navigation.compose

import androidx.compose.runtime.Composable
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.compose.animation.AnimatedContentScope

public fun NavGraphBuilder.composable(
    route: String,
    content: @Composable AnimatedContentScope.(NavBackStackEntry) -> Unit
) {}

// MODULE: main(navigation.runtime, navigation.compose)
// FILE: com/example/reply/data/Email.kt
package com.example.reply.data

/**
 * A simple data class to represent an Email.
 */
data class Email(
    val id: Long,
    val sender: String,
    val recipients: List<String> = emptyList(),
    val subject: String,
    val body: String,
    val attachments: List<String> = emptyList(),
    var isImportant: Boolean = false,
    var isStarred: Boolean = false,
    val createdAt: String,
    val threads: List<Email> = emptyList()
)
// FILE: com/example/reply/ui/ReplyInboxScreen.kt
package com.example.reply.ui

import androidx.compose.runtime.Composable

@Composable
fun ReplyInboxScreen(
    replyHomeUIState: ReplyHomeUIState,
) {}

// FILE: com/example/reply/ui/ReplyHomeUIState.kt
package com.example.reply.ui

import com.example.reply.data.Email

data class ReplyHomeUIState(
    val emails: List<Email> = emptyList(),
    val selectedEmails: Set<Long> = emptySet(),
    val openedEmail: Email? = null,
    val isDetailOnlyOpen: Boolean = false,
    val loading: Boolean = false,
    val error: String? = null
)

// FILE: main.kt
package home

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.reply.ui.ReplyHomeUIState
import com.example.reply.ui.ReplyInboxScreen

@Composable
private fun ReplyNavHost(
    navController: NavHostController,
    replyHomeUIState: ReplyHomeUIState,
    inbox: String,
    modifier: Modifier = Modifier,
) {
    NavHost(
        modifier = modifier,
        navController = navController,
        startDestination = inbox,
    ) {
        composable(inbox) {
            ReplyInboxScreen(
                replyHomeUIState = replyHomeUIState,
            )
        }
    }
}
