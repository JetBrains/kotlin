@file:Repository("https://repo.maven.apache.org/maven2/")
@file:Repository("https://maven.google.com/")
// See https://mvnrepository.com/artifact/org.jetbrains.compose/compose-full
@file:DependsOn("org.jetbrains.compose:compose-full:1.10.1")

import androidx.compose.material.Text
import androidx.compose.runtime.Composable

@Composable
fun App() {
    Text("Hello Compose")
}

val text = "Hello World"
text
