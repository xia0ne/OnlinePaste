(function () {
	'use strict';
	try {
		var stored = localStorage.getItem('onlinepaste-theme');
		if (stored === 'dark' || stored === 'light') {
			document.documentElement.dataset.theme = stored;
		}
	} catch (error) {
		// Storage can be unavailable in private mode, fall back to the system theme.
	}
})();
