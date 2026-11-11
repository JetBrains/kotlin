/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

(function () {
    window.addEventListener('resize', foldOrUnfoldChips);
    window.addEventListener('DOMContentLoaded', foldOrUnfoldChips);
    window.addEventListener('load', foldOrUnfoldChips);
})();

function foldOrUnfoldChips() {
    const foldClass = 'chips-folded';
    const root = document.documentElement;
    root.classList.remove(foldClass);
    if (root.scrollWidth > root.clientWidth || chipsAreWrapped()) {
        root.classList.add(foldClass);
    }
}

function chipsAreWrapped() {
    return Array.prototype.some.call(document.querySelectorAll('td'), function (cell) {
        const chips = cell.querySelectorAll('.chip');
        if (chips.length < 2) return false;
        const first = chips[0].getBoundingClientRect();
        return chips[chips.length - 1].getBoundingClientRect().top > first.top + first.height / 2;
    });
}
