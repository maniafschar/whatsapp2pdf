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

	static preview() {
		var file = document.getElementById('chatFile');
		document.getElementsByTagName('error')[0].innerHTML = '';
		document.getElementsByTagName('progressbar')[0].style.display = 'block';
		api.ajax({
			url: api.url + '/rest/api/preview/' + document.querySelector('id').innerText + '?period=' + encodeURIComponent(document.querySelector('period .selected').getAttribute('value')) + '&user=' + encodeURIComponent(document.querySelector('user .selected').getAttribute('value')),
			method: 'POST',
			success: api.download,
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

	static postAnalyse(data) {
		document.getElementsByTagName('progressbar')[0].style.display = null;
		document.getElementsByTagName('attributes')[0].style.display = 'block';
		document.getElementsByTagName('attributes')[0].querySelector('id').innerText = data.id;
		var s = '<table><tr><th>Period</th><th>Chats</th><th>Words</th><th>Letters</th></tr>';
		for (var i = 0; i < data.periods.length; i++)
			s += '<tr value="' + data.periods[i].period + '"' + (s.indexOf('" class="selected">') < 0 ? ' class="selected"' : '') + '><td>' + data.periods[i].period.replace('-\\d\\d', '').replace('/\\d\\d', '').replace('\\d\\d.', '') + '</td><td>' + data.periods[i].chats.toLocaleString() + '</td><td>' + data.periods[i].words.toLocaleString() + '</td><td>' + data.periods[i].letters.toLocaleString() + '</td></tr>';
		document.getElementsByTagName('attributes')[0].querySelector('period').innerHTML = s + '</table>';
		document.getElementsByTagName('attributes')[0].querySelectorAll('period td').forEach(td => {
			td.addEventListener('click', () => {
				document.querySelector('period .selected').classList.remove('selected');
				td.parentElement.classList.add('selected');
			});
		});
		s = '<table><tr><th>User</th><th>Chats</th><th>Words</th><th>Letters</th></tr>';
		for (var i = 0; i < data.users.length; i++)
			s += '<tr value="' + data.users[i].user + '"' + (s.indexOf('" class="selected">') < 0 ? ' class="selected"' : '') + '><td>' + data.users[i].user + '</td><td>' + data.users[i].chats.toLocaleString() + '</td><td>' + data.users[i].words.toLocaleString() + '</td><td>' + data.users[i].letters.toLocaleString() + '</td></tr>';
		document.getElementsByTagName('attributes')[0].querySelector('user').innerHTML = s + '</table>';
		document.getElementsByTagName('attributes')[0].querySelectorAll('user td').forEach(td => {
			td.addEventListener('click', () => {
				document.querySelector('user .selected').classList.remove('selected');
				td.parentElement.classList.add('selected');
			});
		});
	}
	static download() {
		var download = function () {
			api.ajax({
				url: api.url + '/rest/api/pdf/' + document.querySelector('id').innerText,
				method: 'GET',
				success: () => {
					document.getElementsByTagName('progressbar')[0].style.display = null;
					var link = document.createElement('a');
					link.setAttribute('href', api.url + '/rest/api/pdf/' + document.querySelector('id').innerText);
					link.setAttribute('target', '_blank');
					link.click();
				},
				error: xhr => {
					var error = xhr.responseText || 'Unknown error';
					if (error.indexOf('"status":566,') > -1)
						setTimeout(download, 1000);
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
							error = 'Creation failed: ' + error + '!';
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
