import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.ReadOnlyComposable


// note: using var makes this class unstable (and this vm needs to be unstable for the crash to occur)
class MyUnstableViewModel(var text: String?) {
    fun onClickyClicky() {}
}

@Composable fun Scratch(vm: MyUnstableViewModel) {
    Dialog(
        content = slotIfNotNull(vm.text) {
            Button(
                onClick = vm::onClickyClicky
            )
        }
    )
}
