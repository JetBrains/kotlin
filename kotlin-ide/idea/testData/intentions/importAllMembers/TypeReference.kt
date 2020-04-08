// INTENTION_TEXT: "Import members from 'com.test.States'"
import com.test.States

fun foo(s: States) {
    when (s) {
        is <caret>States.Loading -> {
        }
        is States.Error -> {
        }
        is States.Content -> {
        }
    }
}

fun bar(): States.Loading = States.Loading