(function () {
	'use strict';

	var OP = (window.OP = window.OP || {});
	var THEME_KEY = 'onlinepaste-theme';
	var toastTimer = null;

	function systemTheme() {
		return window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
	}

	function effectiveTheme() {
		return document.documentElement.dataset.theme === 'dark' ? 'dark' : 'light';
	}

	function applyHighlightTheme(theme) {
		var light = document.getElementById('hljs-light');
		var dark = document.getElementById('hljs-dark');
		if (light) {
			light.disabled = theme === 'dark';
		}
		if (dark) {
			dark.disabled = theme !== 'dark';
		}
	}

	function updateThemeButtons(theme) {
		document.querySelectorAll('[data-theme-toggle]').forEach(function (button) {
			button.setAttribute('aria-pressed', String(theme === 'dark'));
			button.title = theme === 'dark' ? 'Switch to light theme' : 'Switch to dark theme';
		});
	}

	OP.applyTheme = function (theme) {
		var next = theme === 'dark' ? 'dark' : 'light';
		document.documentElement.dataset.theme = next;
		try {
			localStorage.setItem(THEME_KEY, next);
		} catch (error) {
			// Ignore storage failures; the theme still changes for this page.
		}
		applyHighlightTheme(next);
		updateThemeButtons(next);
	};

	function escapeHtml(value) {
		return String(value)
			.replace(/&/g, '&amp;')
			.replace(/</g, '&lt;')
			.replace(/>/g, '&gt;')
			.replace(/"/g, '&quot;')
			.replace(/'/g, '&#39;');
	}

	OP.escapeHtml = escapeHtml;

	function safeHref(value) {
		var href = String(value == null ? '' : value).trim();
		if (/^(https?:\/\/|mailto:|\/|#)/i.test(href)) {
			return href;
		}
		return '#';
	}

	/**
	 * Renders markdown with raw HTML escaped and unsafe link/image schemes
	 * removed. Server side rendering uses th:text, this keeps the client side
	 * preview at the same level of trust.
	 */
	OP.renderMarkdown = function (text) {
		if (!window.marked) {
			return '<pre class="raw-content">' + escapeHtml(text) + '</pre>';
		}
		var renderer = new marked.Renderer();
		renderer.html = function (html) {
			var raw = typeof html === 'string' ? html : (html && html.text) || '';
			return escapeHtml(raw);
		};
		renderer.link = function (href, title, label) {
			var titleAttribute = title ? ' title="' + escapeHtml(title) + '"' : '';
			return '<a href="' + escapeHtml(safeHref(href)) + '"' + titleAttribute
				+ ' target="_blank" rel="noopener noreferrer nofollow">' + (label || '') + '</a>';
		};
		renderer.image = function (href, title, alt) {
			var titleAttribute = title ? ' title="' + escapeHtml(title) + '"' : '';
			return '<img src="' + escapeHtml(safeHref(href)) + '" alt="' + escapeHtml(alt || '')
				+ '"' + titleAttribute + ' loading="lazy" referrerpolicy="no-referrer">';
		};
		marked.setOptions({ gfm: true, breaks: true, headerIds: false, mangle: false });
		return marked.parse(text, { renderer: renderer });
	};

	OP.highlightAll = function (root) {
		if (!window.hljs || !root) {
			return;
		}
		root.querySelectorAll('pre code').forEach(function (block) {
			if (block.dataset.highlighted === 'yes') {
				return;
			}
			try {
				hljs.highlightElement(block);
				block.dataset.highlighted = 'yes';
			} catch (error) {
				// Highlighting is progressive enhancement only.
			}
		});
	};

	function whenMathJaxReady(callback) {
		if (window.MathJax && window.MathJax.startup && window.MathJax.startup.promise) {
			window.MathJax.startup.promise.then(callback).catch(function () {});
			return;
		}
		var attempts = 0;
		var timer = setInterval(function () {
			attempts += 1;
			if (window.MathJax && window.MathJax.startup && window.MathJax.startup.promise) {
				clearInterval(timer);
				window.MathJax.startup.promise.then(callback).catch(function () {});
			} else if (attempts > 40) {
				clearInterval(timer);
			}
		}, 250);
	}

	OP.typesetMath = function (root) {
		if (!root) {
			return;
		}
		whenMathJaxReady(function () {
			try {
				window.MathJax.typesetPromise([root]).catch(function () {});
			} catch (error) {
				// Math is optional; keep the rendered text visible.
			}
		});
	};

	OP.formatRemaining = function (iso) {
		if (!iso) {
			return 'Never expires';
		}
		var diff = new Date(iso).getTime() - Date.now();
		if (isNaN(diff)) {
			return '';
		}
		if (diff <= 0) {
			return 'Expired';
		}
		var minutes = Math.floor(diff / 60000);
		if (minutes < 60) {
			return 'in ' + Math.max(1, minutes) + ' min';
		}
		var hours = Math.floor(minutes / 60);
		if (hours < 24) {
			return 'in ' + hours + 'h ' + (minutes % 60) + 'm';
		}
		var days = Math.floor(hours / 24);
		return 'in ' + days + 'd ' + (hours % 24) + 'h';
	};

	OP.formatLocal = function (iso) {
		if (!iso) {
			return 'Never';
		}
		var date = new Date(iso);
		return isNaN(date.getTime()) ? iso : date.toLocaleString();
	};

	OP.copy = function (text) {
		if (navigator.clipboard && window.isSecureContext) {
			return navigator.clipboard.writeText(text).then(function () {
				return true;
			}).catch(function () {
				return fallbackCopy(text);
			});
		}
		return Promise.resolve(fallbackCopy(text));
	};

	function fallbackCopy(text) {
		try {
			var area = document.createElement('textarea');
			area.value = text;
			area.setAttribute('readonly', '');
			area.style.position = 'fixed';
			area.style.top = '-1000px';
			area.style.opacity = '0';
			document.body.appendChild(area);
			area.select();
			var copied = document.execCommand('copy');
			document.body.removeChild(area);
			return copied;
		} catch (error) {
			return false;
		}
	}

	OP.toast = function (message, type) {
		var toast = document.getElementById('toast');
		if (!toast) {
			toast = document.createElement('div');
			toast.id = 'toast';
			toast.className = 'toast';
			toast.setAttribute('role', 'status');
			toast.setAttribute('aria-live', 'polite');
			document.body.appendChild(toast);
		}
		toast.textContent = message;
		toast.style.borderColor = type === 'error' ? '#b42318' : '';
		toast.classList.add('show');
		clearTimeout(toastTimer);
		toastTimer = setTimeout(function () {
			toast.classList.remove('show');
		}, 2400);
	};

	function bindCopyButtons() {
		document.addEventListener('click', function (event) {
			var trigger = event.target.closest('[data-copy-target], [data-copy-text]');
			if (!trigger) {
				return;
			}
			event.preventDefault();
			var text = trigger.getAttribute('data-copy-text');
			if (text == null) {
				var selector = trigger.getAttribute('data-copy-target');
				var target = selector ? document.querySelector(selector) : null;
				text = target ? (target.value !== undefined ? target.value : target.textContent) : '';
			}
			OP.copy(text).then(function (copied) {
				if (!copied) {
					OP.toast('Copy failed, please select the text manually', 'error');
					return;
				}
				var original = trigger.textContent;
				trigger.textContent = 'Copied';
				trigger.classList.add('copied');
				OP.toast('Copied to clipboard');
				setTimeout(function () {
					trigger.textContent = original;
					trigger.classList.remove('copied');
				}, 1600);
			});
		});
	}

	function bindThemeToggle() {
		document.querySelectorAll('[data-theme-toggle]').forEach(function (button) {
			button.addEventListener('click', function () {
				OP.applyTheme(effectiveTheme() === 'dark' ? 'light' : 'dark');
			});
		});
	}

	function init() {
		applyHighlightTheme(effectiveTheme());
		updateThemeButtons(effectiveTheme());
		bindCopyButtons();
		bindThemeToggle();

		if (window.matchMedia) {
			var media = window.matchMedia('(prefers-color-scheme: dark)');
			var onChange = function () {
				if (!document.documentElement.dataset.theme) {
					applyHighlightTheme(systemTheme());
				}
			};
			if (media.addEventListener) {
				media.addEventListener('change', onChange);
			} else if (media.addListener) {
				media.addListener(onChange);
			}
		}
	}

	if (document.readyState === 'loading') {
		document.addEventListener('DOMContentLoaded', init);
	} else {
		init();
	}
})();
