// runs in <head> so the theme doesn't flash
(function () {
    var root = document.documentElement;
    try {
        var theme = localStorage.getItem('theme');
        if (theme === 'dark' || (!theme && window.matchMedia('(prefers-color-scheme: dark)').matches)) {
            root.classList.add('dark');
        }
        var accent = localStorage.getItem('accent');
        if (accent) {
            root.dataset.accent = accent;
        }
    } catch (e) {
    }
})();
