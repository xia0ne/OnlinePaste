(function () {
	'use strict';

	var raw = document.getElementById('paste-content');
	if (!raw || !window.OP) {
		return;
	}

	var content = raw.textContent;
	var previewPane = document.getElementById('preview-pane');
	var previewBody = document.getElementById('preview-body');
	var codePane = document.getElementById('code-pane');
	var codeBlock = document.getElementById('code-block');
	var tabs = document.querySelectorAll('[data-view-tab]');
	var expiresNode = document.getElementById('expires-relative');
	var codeHighlighted = false;

	function showTab(name) {
		var isPreview = name !== 'code';
		previewPane.hidden = !isPreview;
		codePane.hidden = isPreview;

		tabs.forEach(function (tab) {
			tab.setAttribute('aria-selected', String(tab.getAttribute('data-view-tab') === name));
		});

		if (!isPreview && !codeHighlighted) {
			codeHighlighted = true;
			if (window.hljs) {
				try {
					codeBlock.innerHTML = hljs.highlightAuto(content).value;
					codeBlock.classList.add('hljs');
				} catch (error) {
					codeBlock.textContent = content;
				}
			} else {
				codeBlock.textContent = content;
			}
		}
	}

	tabs.forEach(function (tab) {
		tab.addEventListener('click', function () {
			showTab(tab.getAttribute('data-view-tab'));
		});
	});

	try {
		previewBody.innerHTML = OP.renderMarkdown(content);
		previewPane.hidden = false;
		raw.hidden = true;
		OP.highlightAll(previewBody);
		OP.typesetMath(previewBody);
	} catch (error) {
		// Keep the server rendered, escaped plain text visible.
		raw.hidden = false;
		previewPane.hidden = true;
	}

	function updateCountdown() {
		if (!expiresNode) {
			return;
		}
		var expires = expiresNode.getAttribute('data-expires');
		expiresNode.textContent = expires ? OP.formatRemaining(expires) : 'No expiry';
	}

	updateCountdown();
	setInterval(updateCountdown, 30000);

	showTab('preview');
})();
