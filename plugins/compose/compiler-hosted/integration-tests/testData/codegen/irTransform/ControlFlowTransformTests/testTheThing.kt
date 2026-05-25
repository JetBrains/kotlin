import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.key
import androidx.compose.runtime.NonRestartableComposable


@NonRestartableComposable
@Composable
fun Simple() {
  // this has a composable call in it, and since we don't know the number of times the
  // lambda will get called, we place a group around the whole call
  run {
    A()
  }
  A()
}

@NonRestartableComposable
@Composable
fun WithReturn() {
  // this has an early return in it, so it needs to end all of the groups present.
  run {
    A()
    return@WithReturn
  }
  A()
}

@NonRestartableComposable
@Composable
fun NoCalls() {
  // this has no composable calls in it, so shouldn't cause any groups to get created
  run {
    println("hello world")
  }
  A()
}

@NonRestartableComposable
@Composable
fun NoCallsAfter() {
  // this has a composable call in the lambda, but not after it, which means the
  // group should be able to be coalesced into the group of the function
  run {
    A()
  }
}
