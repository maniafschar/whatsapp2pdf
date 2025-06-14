export { api };

class api {
	static url = '{placeholderServer}';

	static analyse() {
		var file = document.getElementById('chatFile');
		if (file.files[0]) {
			document.getElementsByTagName('error')[0].innerHTML = '';
			document.getElementsByTagName('progressbar')[0].style.display = 'block';
			var formData = new FormData();
			formData.append('file', file.files[0]);
			api.ajax({
				url: api.url + '/rest/api/analyse',
				method: 'POST',
				body: formData,
				success: api.postAnalyse,
				error: xhr => {
					document.getElementsByTagName('progressbar')[0].style.display = null;
					document.getElementsByTagName('error')[0].innerHTML = xhr.status < 500 ? 'The server is unavailable. Please try again later.' : 'PDF creation failed. Is that a WhatsApp exported chat file?';
				}
			});
		} else
			document.getElementsByTagName('error')[0].innerHTML = 'Please select a file to convert.';
	}

	static preview(event, period) {
		event.preventDefault();
		event.stopPropagation();
		var file = document.getElementById('chatFile');
		document.getElementsByTagName('error')[0].innerHTML = '';
		document.getElementsByTagName('progressbar')[0].style.display = 'block';
		api.ajax({
			url: api.url + '/rest/api/preview/' + document.querySelector('id').innerText + '?period=' + encodeURIComponent(period) + '&user=' + encodeURIComponent(document.querySelector('user .selected').getAttribute('value')),
			method: 'POST',
			success: api.download,
			error: xhr => {
				document.getElementsByTagName('progressbar')[0].style.display = null;
				document.getElementsByTagName('error')[0].innerHTML = xhr.status < 500 ? 'The server is unavailable. Please try again later.' : 'PDF creation failed. Please try again later.';
			}
		});
	}

	static buy() {
		if (document.querySelector('period .spinner,period .download'))
			return;
		var file = document.getElementById('chatFile');
		document.getElementsByTagName('error')[0].innerHTML = '';
		document.getElementsByTagName('progressbar')[0].style.display = 'block';
		var period = '';
		var periods = document.querySelectorAll('period .selected');
		if (periods.length < 1)
			return;
		for (var i = 0; i < periods.length; i++)
			period += 'periods=' + encodeURIComponent(periods[i].getAttribute('value')) + '&';
		api.ajax({
			url: api.url + '/rest/api/buy/' + document.querySelector('id').innerText + '?' + period + 'user=' + encodeURIComponent(document.querySelector('user .selected').getAttribute('value')),
			method: 'POST',
			success: api.postBuy,
			error: xhr => {
				document.getElementsByTagName('progressbar')[0].style.display = null;
				document.getElementsByTagName('error')[0].innerHTML = xhr.status < 500 ? 'The server is unavailable. Please try again later.' : 'PDF creation failed. Please try again later.';
			}
		});
	}

	static cleanUp() {
		var file = document.getElementById('chatFile');
		document.getElementsByTagName('error')[0].innerHTML = '';
		document.getElementsByTagName('progressbar')[0].style.display = 'block';
		api.ajax({
			url: api.url + '/rest/api/cleanUp/' + document.querySelector('id').innerText,
			method: 'DELETE',
			success: () => {
				document.getElementsByTagName('progressbar')[0].style.display = null;
				document.getElementsByTagName('attributes')[0].style.display = null;
				document.getElementById('chatFile').value = '';
			}
		});
	}

	static saveFeedback() {
		api.ajax({
			url: api.url + '/rest/api/feedback/' + document.querySelector('id').innerText,
			method: 'POST',
			success: xhr => {
				document.querySelector('popup content').innerHTML = xhr;
			},
			error: xhr => {
				document.getElementsByTagName('progressbar')[0].style.display = null;
				document.getElementsByTagName('error')[0].innerHTML = xhr.status < 500 ? 'The server is unavailable. Please try again later.' : 'Saving feedback failed: ' + xhr.responseText;
			}
		});
	}

	static postAnalyse(data) {
		document.getElementsByTagName('progressbar')[0].style.display = null;
		document.getElementsByTagName('attributes')[0].style.display = 'block';
		document.querySelector('attributes button[onclick*="cleanUp"]').style.display = '';
		document.getElementsByTagName('attributes')[0].querySelector('id').innerText = data.id;
		var s = '<table><tr><th>Period</th><th>Chats</th><th>Words</th><th>Letters</th><th></th></tr>';
		for (var i = 0; i < data.periods.length; i++)
			s += '<tr value="' + data.periods[i].period + '"' + (s.indexOf('" class="selected">') < 0 ? ' class="selected"' : '') + '><td>' + data.periods[i].period.replace('-\\d\\d', '').replace('/\\d\\d', '').replace('\\d\\d.', '') + '</td><td>' + data.periods[i].chats.toLocaleString() + '</td><td>' + data.periods[i].words.toLocaleString() + '</td><td>' + data.periods[i].letters.toLocaleString() + '</td><td><button onclick="api.preview(event, &quot;' + data.periods[i].period.replaceAll('\\', '\\\\') + '&quot;)">Preview</button></td></tr>';
		document.getElementsByTagName('attributes')[0].querySelector('period').innerHTML = s + '</table>';
		document.getElementsByTagName('attributes')[0].querySelectorAll('period tr').forEach(tr => {
			tr.addEventListener('click', () => {
				if (tr.classList.contains('download')) {
					var link = document.createElement('a');
					link.setAttribute('href', api.url + '/rest/api/pdf/' + document.querySelector('id').innerText + '?period=' + encodeURIComponent(tr.getAttribute('value')));
					link.setAttribute('target', '_blank');
					link.click();
					tr.classList.remove('download');
					if (document.querySelectorAll('period .spinner,period .download').length == 0)
						document.querySelector('attributes button[onclick*="cleanUp"]').style.display = '';
					if (document.querySelectorAll('period .selected,period .spinner,period .download').length == 0)
						tr.classList.add('selected');
					document.querySelector('description button').style.display = '';
				} else if (tr.classList.contains('selected')) {
					if (document.querySelectorAll('period .selected').length > 1)
						tr.classList.remove('selected');
				} else if (!tr.classList.contains('spinner'))
					tr.classList.add('selected');
			});
		});
		s = '<table><tr><th>User</th><th>Chats</th><th>Words</th><th>Letters</th></tr>';
		for (var i = 0; i < data.users.length; i++)
			s += '<tr value="' + data.users[i].user + '"' + (s.indexOf('" class="selected">') < 0 ? ' class="selected"' : '') + '><td>' + data.users[i].user + '</td><td>' + data.users[i].chats.toLocaleString() + '</td><td>' + data.users[i].words.toLocaleString() + '</td><td>' + data.users[i].letters.toLocaleString() + '</td></tr>';
		document.getElementsByTagName('attributes')[0].querySelector('user').innerHTML = s + '</table>';
		document.getElementsByTagName('attributes')[0].querySelectorAll('user tr').forEach(tr => {
			tr.addEventListener('click', () => {
				document.querySelector('user .selected').classList.remove('selected');
				tr.classList.add('selected');
			});
		});
	}

	static postBuy() {
		document.querySelector('attributes button[onclick*="cleanUp"]').style.display = 'none';
		var periods = document.querySelectorAll('period .selected');
		for (var i = 0; i < periods.length; i++) {
			api.download(periods[i].getAttribute('value'));
			var tr = document.querySelector('period tr[value="' + periods[i].getAttribute('value').replaceAll('\\', '\\\\') + '"]');
			tr.classList.remove('selected');
			tr.classList.add('spinner');
		}
	}

	static download(period) {
		var download = function () {
			api.ajax({
				url: api.url + '/rest/api/pdf/' + document.querySelector('id').innerText + (period ? '?period=' + encodeURIComponent(period) : ''),
				method: 'GET',
				success: () => {
					document.getElementsByTagName('progressbar')[0].style.display = null;
					if (period) {
						var tr = document.querySelector('period tr[value="' + period.replaceAll('\\', '\\\\') + '"]');
						tr.classList.remove('spinner');
						tr.classList.add('download');
					} else {
						var link = document.createElement('a');
						link.setAttribute('href', api.url + '/rest/api/pdf/' + document.querySelector('id').innerText);
						link.setAttribute('target', '_blank');
						link.click();
					}
				},
				error: xhr => {
					var error = xhr.responseText || 'Unknown error';
					if (error.indexOf('"status":566,') > -1)
						setTimeout(function () { download(period); }, 1000);
					else {
						document.getElementsByTagName('progressbar')[0].style.display = null;
						if (xhr.status < 500)
							error = 'The server is unavailable. Please try again later.';
						else if (error.indexOf('Invalid ID') > -1) {
							document.getElementsByTagName('attributes')[0].style.display = null;
							document.getElementById('chatFile').value = '';
							error = 'Uploaded chat already deleted. Please upload new chat.';
						} else {
							if (error.indexOf(' ') < 5)
								error = error.substring(error.indexOf(' ')).trim();
							error = 'Creation failed: ' + error;
						}
						document.getElementsByTagName('error')[0].innerHTML = error;
					}
				}
			});
		}
		setTimeout(download, 1000);
	}

	static ajax(param) {
		var xhr = new XMLHttpRequest();
		xhr.onreadystatechange = function () {
			if (xhr.readyState == 4) {
				var errorHandler = function () {
					if (param.error) {
						xhr.param = param;
						param.error(xhr);
					} else {
						document.getElementsByTagName('progressbar')[0].style.display = null;
						document.getElementsByTagName('error')[0].innerHTML = 'An error occurred while processing your request. Please try again later.';
					}
				};
				if (xhr.status >= 200 && xhr.status < 300) {
					if (param.success) {
						var response = xhr.responseText;
						if (response && (response.indexOf('{') === 0 || response.indexOf('[') === 0)) {
							try {
								response = JSON.parse(xhr.responseText)
							} catch (e) {
							}
						}
						param.success(response);
					}
				} else
					errorHandler();
			}
		};
		xhr.open(param.method ? param.method : 'GET', param.url, true);
		if (typeof param.body == 'string')
			xhr.setRequestHeader('Content-Type', 'application/x-www-form-urlencoded');
		else if (param.body && !(param.body instanceof FormData)) {
			xhr.setRequestHeader('Content-Type', 'application/json');
			param.body = JSON.stringify(param.body);
		}
		xhr.send(param.body);
	}
}
