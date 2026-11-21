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

    // confirm before submit
    var confirmDialog = document.getElementById('confirm-dialog');
    var pending = null;
    document.querySelectorAll('form[data-confirm]').forEach(function (form) {
        form.addEventListener('submit', function (event) {
            if (form.dataset.confirmed === '1' || !confirmDialog) {
                return;
            }
            event.preventDefault();
            pending = form;
            confirmDialog.querySelector('[data-confirm-message]').textContent = form.dataset.confirm;
            confirmDialog.showModal();
        });
    });
    if (confirmDialog) {
        confirmDialog.addEventListener('close', function () {
            if (confirmDialog.returnValue === 'ok' && pending) {
                pending.dataset.confirmed = '1';
                pending.requestSubmit();
            }
            pending = null;
        });
    }

    // dialogs
    document.querySelectorAll('[data-dialog-open]').forEach(function (button) {
        button.addEventListener('click', function (event) {
            var dialog = document.getElementById(button.dataset.dialogOpen);
            if (dialog) {
                event.preventDefault();
                dialog.showModal();
            }
        });
    });
    document.querySelectorAll('[data-dialog-close]').forEach(function (button) {
        button.addEventListener('click', function () {
            button.closest('dialog').close();
        });
    });
    document.querySelectorAll('dialog[data-open]').forEach(function (dialog) {
        dialog.showModal();
        dialog.addEventListener('close', function () {
            // remove ?new / ?edit so a reload doesn't reopen it
            var url = new URL(window.location.href);
            url.searchParams.delete('new');
            url.searchParams.delete('edit');
            history.replaceState(null, '', url);
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
