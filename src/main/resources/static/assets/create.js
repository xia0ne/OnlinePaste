(function () {
	'use strict';

	var form = document.getElementById('create-form');
	if (!form || !window.OP) {
		return;
	}

	var content = document.getElementById('content');
	var preview = document.getElementById('preview');
	var counter = document.getElementById('char-count');
	var submit = document.getElementById('submit-button');
	var expiration = document.getElementById('expiration');
	var customId = document.getElementById('custom-id');
	var errorBox = document.getElementById('create-error');
	var result = document.getElementById('result');
	var maxLength = Number(content.getAttribute('maxlength')) || 100000;
	var previewTimer = null;

	function renderPreview() {
		var text = content.value;
		counter.textContent = text.length.toLocaleString() + ' / ' + maxLength.toLocaleString();
		if (!text.trim()) {
			preview.innerHTML = '<p class="muted">Nothing to preview yet.</p>';
			return;
		}
		preview.innerHTML = OP.renderMarkdown(text);
		OP.highlightAll(preview);
		OP.typesetMath(preview);
	}

	function showError(message) {
		errorBox.textContent = message;
		errorBox.hidden = false;
	}

	function showResult(data) {
		document.getElementById('share-url').value = data.url;
		document.getElementById('paste-id').value = data.id;
		document.getElementById('open-link').href = data.url;
		document.getElementById('result-expires').textContent = data.permanent
			? 'This paste never expires.'
			: 'Expires ' + OP.formatLocal(data.expiresAt) + ' (' + OP.formatRemaining(data.expiresAt) + ')';
		var qr = document.getElementById('qr-image');
		qr.src = data.qrUrl;
		qr.onerror = function () {
			qr.closest('.qr-panel').hidden = true;
		};
		result.hidden = false;
		result.scrollIntoView({ behavior: 'smooth', block: 'start' });
		OP.toast('Paste created');
	}

	content.addEventListener('input', function () {
		clearTimeout(previewTimer);
		previewTimer = setTimeout(renderPreview, 120);
	});

	form.addEventListener('submit', function (event) {
		event.preventDefault();
		errorBox.hidden = true;

		if (!content.value.trim()) {
			showError('Write or paste something first.');
			content.focus();
			return;
		}

		var payload = {
			content: content.value,
			expiration: expiration.value
		};
		var requestedId = customId.value.trim();
		if (requestedId) {
			payload.id = requestedId;
		}

		var originalLabel = submit.textContent;
		submit.disabled = true;
		submit.textContent = 'Creating…';

		fetch('/api/pastes', {
			method: 'POST',
			headers: {
				'Content-Type': 'application/json',
				'Accept': 'application/json'
			},
			body: JSON.stringify(payload)
		}).then(function (response) {
			return response.json().catch(function () {
				return {};
			}).then(function (data) {
				if (!response.ok) {
					throw new Error(data.message || 'Could not create the paste.');
				}
				return data;
			});
		}).then(showResult).catch(function (error) {
			showError(error.message || 'Network error, please retry.');
		}).finally(function () {
			submit.disabled = false;
			submit.textContent = originalLabel;
		});
	});

	document.addEventListener('keydown', function (event) {
		if ((event.metaKey || event.ctrlKey) && event.key === 'Enter') {
			event.preventDefault();
			form.requestSubmit();
		}
	});

	renderPreview();
})();
