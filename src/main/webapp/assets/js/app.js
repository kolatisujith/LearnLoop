(function () {
    'use strict';

    document.documentElement.classList.add('js');

    var navbar = document.querySelector('.ll-navbar');

    function updateNavbar() {
        if (!navbar) {
            return;
        }
        navbar.classList.toggle('is-scrolled', window.scrollY > 8);
    }

    updateNavbar();
    window.addEventListener('scroll', updateNavbar, {passive: true});

    var year = document.getElementById('currentYear');
    if (year) {
        year.textContent = String(new Date().getFullYear());
    }

    var statusLink = document.getElementById('platform-status');
    var footerStatus = document.getElementById('footer-platform-status');
    if (statusLink && typeof window.fetch === 'function') {
        var statusText = statusLink.querySelector('[data-status-text]');
        var healthUrl = statusLink.getAttribute('data-health-url');
        var controller = new AbortController();
        var timeout = window.setTimeout(function () {
            controller.abort();
        }, 5000);

        window.fetch(healthUrl, {
            method: 'GET',
            headers: {'Accept': 'application/json'},
            cache: 'no-store',
            signal: controller.signal
        }).then(function (response) {
            if (!response.ok) {
                throw new Error('Health endpoint returned ' + response.status);
            }
            return response.json();
        }).then(function (payload) {
            if (payload.status !== 'UP') {
                throw new Error('Application is not up');
            }
            statusLink.classList.add('is-online');
            statusLink.classList.remove('is-offline');
            statusLink.title = payload.application + ' ' + payload.version + ' is running';
            statusLink.setAttribute('aria-label', 'Platform online. Open health endpoint.');
            if (footerStatus) {
                footerStatus.classList.add('is-online');
                footerStatus.classList.remove('is-offline');
            }
            if (statusText) {
                statusText.textContent = 'Platform online';
            }
        }).catch(function () {
            statusLink.classList.add('is-offline');
            statusLink.classList.remove('is-online');
            statusLink.title = 'Health endpoint is unavailable';
            statusLink.setAttribute('aria-label', 'Platform status unavailable. Open health endpoint.');
            if (footerStatus) {
                footerStatus.classList.add('is-offline');
                footerStatus.classList.remove('is-online');
            }
            if (statusText) {
                statusText.textContent = 'Status unavailable';
            }
        }).finally(function () {
            window.clearTimeout(timeout);
        });
    }

    var revealItems = document.querySelectorAll('[data-reveal]');
    var reducedMotion = window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches;

    if (!('IntersectionObserver' in window) || reducedMotion) {
        revealItems.forEach(function (item) {
            item.classList.add('is-visible');
        });
    } else {
        var observer = new IntersectionObserver(function (entries) {
            entries.forEach(function (entry) {
                if (entry.isIntersecting) {
                    entry.target.classList.add('is-visible');
                    observer.unobserve(entry.target);
                }
            });
        }, {threshold: 0.12, rootMargin: '0px 0px -40px 0px'});

        revealItems.forEach(function (item) {
            observer.observe(item);
        });
    }

    document.querySelectorAll('form[data-confirm]').forEach(function (form) {
        form.addEventListener('submit', function (event) {
            var message = form.getAttribute('data-confirm');
            if (message && !window.confirm(message)) {
                event.preventDefault();
            }
        });
    });

    document.querySelectorAll('[data-copy-target]').forEach(function (button) {
        button.addEventListener('click', function () {
            var input = document.getElementById(button.getAttribute('data-copy-target'));
            if (!input) {
                return;
            }

            var originalLabel = button.textContent;
            input.focus();
            input.select();
            input.setSelectionRange(0, input.value.length);

            copyText(input.value).then(function () {
                button.textContent = 'Copied';
                window.setTimeout(function () {
                    button.textContent = originalLabel;
                }, 2000);
            }).catch(function () {
                button.textContent = 'Press Ctrl+C';
                window.setTimeout(function () {
                    button.textContent = originalLabel;
                }, 3000);
            });
        });
    });

    function copyText(value) {
        if (navigator.clipboard && window.isSecureContext) {
            return navigator.clipboard.writeText(value);
        }

        return new Promise(function (resolve, reject) {
            if (document.execCommand('copy')) {
                resolve();
            } else {
                reject(new Error('Copy is unavailable.'));
            }
        });
    }

    var navCollapse = document.getElementById('llNavbar');
    if (navCollapse) {
        document.querySelectorAll('.ll-navbar .nav-link, .ll-navbar .btn').forEach(function (link) {
            link.addEventListener('click', function () {
                if (navCollapse.classList.contains('show') && window.bootstrap) {
                    window.bootstrap.Collapse.getOrCreateInstance(navCollapse).hide();
                }
            });
        });
    }
}());
