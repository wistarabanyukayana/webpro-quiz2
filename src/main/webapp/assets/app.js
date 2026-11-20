(function () {
    var root = document.documentElement;

    function save(key, value) {
        try {
            localStorage.setItem(key, value);
        } catch (e) {
        }
    }

    document.querySelectorAll('[data-theme-toggle]').forEach(function (button) {
        button.setAttribute('aria-pressed', String(root.classList.contains('dark')));
        button.addEventListener('click', function () {
            var dark = root.classList.toggle('dark');
            button.setAttribute('aria-pressed', String(dark));
            save('theme', dark ? 'dark' : 'light');
        });
    });

    // show/hide password
    document.querySelectorAll('[data-reveal]').forEach(function (button) {
        var input = document.getElementById(button.dataset.reveal);
        button.addEventListener('click', function () {
            var hidden = input.type === 'password';
            input.type = hidden ? 'text' : 'password';
            button.textContent = hidden ? 'Hide' : 'Show';
        });
    });

    // logout needs a second click
    document.querySelectorAll('form[data-logout]').forEach(function (form) {
        var button = form.querySelector('button');
        var label = form.querySelector('[data-logout-label]');
        var timer;
        form.addEventListener('submit', function (event) {
            if (button.hasAttribute('data-armed')) {
                return;
            }
            event.preventDefault();
            button.setAttribute('data-armed', '');
            label.textContent = 'Are you sure?';
            timer = setTimeout(function () {
                button.removeAttribute('data-armed');
                label.textContent = 'Log out';
            }, 4000);
        });
    });

})();

// loading text on submit
document.addEventListener('submit', function (event) {
    if (event.defaultPrevented) {
        return;
    }
    var button = event.submitter;
    if (button && button.dataset.busyText) {
        button.setAttribute('aria-busy', 'true');
        button.textContent = button.dataset.busyText;
        setTimeout(function () {
            button.disabled = true;
        });
    }
});
