// "Remove redundant 'open' modifier" "true"

interface My {
    <caret>open fun foo()
}
/* FIR_COMPARISON */