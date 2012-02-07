function getContext() {
    return getCanvas().getContext('2d');
}

function getCanvas() {
    return document.getElementsByTagName('canvas')[0];
}