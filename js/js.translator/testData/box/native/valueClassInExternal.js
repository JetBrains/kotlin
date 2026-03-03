function isUnwrapped(value) {
    return typeof value === "number"
}

function isWrapped(value) {
    return typeof value === "object"
}

function getNotExported(value) { return value }
function getExported(value) { return value }
