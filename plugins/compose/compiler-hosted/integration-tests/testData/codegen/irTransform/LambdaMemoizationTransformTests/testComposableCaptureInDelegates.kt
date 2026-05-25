import androidx.compose.runtime.*

class Test(val value: Int) : Delegate by Impl({
    value
})
