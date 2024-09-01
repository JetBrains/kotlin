// DUMP_IR

// FILE: com/example/model/Post.kt
package com.example.model
data class Post(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val url: String,
)

// FILE: main.kt
package home

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import com.example.model.Post

@Composable
fun PostImage(post: Post) {
    Image(painter = painterResource(post.id), contentDescription = post.title)
}