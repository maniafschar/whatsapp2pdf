export { api };

class api {
	static url = '{placeholderServer}';
	static count = 0;

	static analyse() {
		var file = document.getElementById('chatFile');
		if (file.files[0]) {
			api.count = 0;
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

	static convert() {
		var file = document.getElementById('chatFile');
		api.count = 0;
		document.getElementsByTagName('error')[0].innerHTML = '';
		document.getElementsByTagName('progressbar')[0].style.display = 'block';
		api.ajax({
			url: api.url + '/rest/api/conversion/' + document.querySelector('id').innerText + '?month=' + encodeURIComponent(document.querySelector('month .selected').getAttribute('value')) + '&user=' + encodeURIComponent(document.querySelector('user .selected').getAttribute('value')),
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
		api.count = 0;
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
		var s = '<table><tr><th>Month</th><th>Chats</th><th>Words</th><th>Letters</th></tr>';
		for (var i = 0; i < data.months.length; i++)
			s += '<tr value="' + data.months[i].month + '"' + (s.indexOf('" class="selected">') < 0 ? ' class="selected"' : '') + '><td>' + data.months[i].month.replace('-\\d\\d', '').replace('/\\d\\d', '').replace('\\d\\d.', '') + '</td><td>' + data.months[i].chats + '</td><td>' + data.months[i].words + '</td><td>' + data.months[i].letters + '</td></tr>';
		document.getElementsByTagName('attributes')[0].querySelector('month').innerHTML = s + '</table>';
		document.getElementsByTagName('attributes')[0].querySelectorAll('month td').forEach(td => {
			td.addEventListener('click', () => {
				document.querySelector('month .selected').classList.remove('selected');
				td.parentElement.classList.add('selected');
			});
		});
		s = '<table><tr><th>User</th><th>Chats</th><th>Words</th><th>Letters</th></tr>';
		for (var i = 0; i < data.users.length; i++)
			s += '<tr value="' + data.users[i].user + '"' + (s.indexOf('" class="selected">') < 0 ? ' class="selected"' : '') + '><td>' + data.users[i].user + '</td><td>' + data.users[i].chats + '</td><td>' + data.users[i].words + '</td><td>' + data.users[i].letters + '</td></tr>';
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
					if (xhr.status == 500 && xhr.responseText.indexOf('Invalid ID') > -1) {
						document.getElementsByTagName('progressbar')[0].style.display = null;
						document.getElementsByTagName('attributes')[0].style.display = null;
						document.getElementById('chatFile').value = '';
						document.getElementsByTagName('error')[0].innerHTML = 'Data already deleted. Please upload new chat.';
						return;
					} else if (++api.count > 600 || xhr.status < 500) {
						document.getElementsByTagName('progressbar')[0].style.display = null;
						document.getElementsByTagName('error')[0].innerHTML =
							xhr.status < 500 ? 'The server is unavailable. Please try again later.' :
								'Download timed out after 10 minutes. Please try again later.';
						return;
					}
					setTimeout(download, 1000);
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
