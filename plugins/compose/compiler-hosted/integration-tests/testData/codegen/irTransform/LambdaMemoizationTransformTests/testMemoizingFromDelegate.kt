import androidx.compose.runtime.*

@Composable
fun StrongSkippingIssue(
    data: ClassWithData
) {
    val state by remember { mutableStateOf("") }
    val action by data::action
    val action1 by getData()::action
    { 
        action
    }
    {
        action1
    }
    {
        state
    }
}
