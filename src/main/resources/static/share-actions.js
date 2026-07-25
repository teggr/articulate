(function () {
  async function copyToClipboard(text) {
    if (navigator.clipboard && window.isSecureContext) {
      await navigator.clipboard.writeText(text);
      return;
    }
    const area = document.createElement('textarea');
    area.value = text;
    area.setAttribute('readonly', '');
    area.style.position = 'absolute';
    area.style.left = '-9999px';
    document.body.appendChild(area);
    area.select();
    document.execCommand('copy');
    document.body.removeChild(area);
  }

  function toAbsolute(url) {
    try {
      return new URL(url, window.location.origin).toString();
    } catch (_error) {
      return url;
    }
  }

  document.addEventListener('click', async function (event) {
    const button = event.target.closest('[data-share-url]');
    if (!button) {
      return;
    }
    const shareUrl = toAbsolute(button.getAttribute('data-share-url') || '');
    if (!shareUrl) {
      return;
    }

    try {
      if (button.classList.contains('share-native') && navigator.share) {
        await navigator.share({ url: shareUrl });
        return;
      }
      await copyToClipboard(shareUrl);
    } catch (error) {
      if (error && error.name === 'AbortError') {
        return;
      }
    }
  });
})();
