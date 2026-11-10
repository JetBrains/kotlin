(function () {
  try {
    const parentDocument = window.parent.document;
    const background = window.parent.getComputedStyle(parentDocument.body).backgroundColor;
    const channels = background.match(/\d+(\.\d+)?/g);
    if (!channels || channels.length < 3 || (channels.length > 3 && Number(channels[3]) === 0)) return;
    const luminance = 0.299 * channels[0] + 0.587 * channels[1] + 0.114 * channels[2];
    document.documentElement.classList.add(luminance < 128 ? 'dark' : 'light');
  } catch (e) {}
})();
