export { api };

class api {
	static url = '{placeholderServer}';
	static feedbackStatus = 'You need to <a onclick="ui.showTab(1)">create</a> at least one document, to give feedback.';

	static analyse(event) {
		document.getElementsByTagName('attributes')[0].style.display = 'none';
		document.getElementsByTagName('upload')[0].style.display = '';
		if (event.target.files[0]) {
			var formData = new FormData();
			formData.append('file', event.target.files[0]);
			api.ajax({
				url: api.url + '/rest/api/pdf/analyse',
				method: 'POST',
				body: formData,
				success: api.postAnalyse,
				error: xhr => {
					document.getElementsByTagName('error')[0].innerHTML =
						xhr.status == 403 ? 'The file was blocked by your proxy. Please try a correct WhatsApp exported chat file.' :
							xhr.status < 500 ? 'The server is unavailable. Please try again later.' : 'PDF creation failed. Is it a WhatsApp exported chat file?';
				}
			});
		} else
			document.getElementsByTagName('error')[0].innerHTML = 'Please select a file to convert.';
	}

	static preview(event, period) {
		event.preventDefault();
		event.stopPropagation();
		api.ajax({
			url: api.url + '/rest/api/pdf/preview/' + document.querySelector('id').innerText + '?period=' + encodeURIComponent(period) + '&user=' + encodeURIComponent(document.querySelector('user .selected').getAttribute('value')),
			method: 'POST',
			success: api.download,
			error: xhr => {
				document.getElementsByTagName('error')[0].innerHTML = xhr.status < 500 ? 'The server is unavailable. Please try again later.' : 'PDF creation failed. Please try again later.';
			}
		});
	}

	static buy() {
		document.querySelector('period').classList.remove('error');
		var period = '';
		var periods = document.querySelectorAll('period .selected');
		if (periods.length < 1) {
			document.querySelector('period').classList.add('error');
			return;
		}
		for (var i = 0; i < periods.length; i++)
			period += 'periods=' + encodeURIComponent(periods[i].getAttribute('value')) + '&';
		api.ajax({
			url: api.url + '/rest/api/pdf/buy/' + document.querySelector('id').innerText + '?' + period + 'user=' + encodeURIComponent(document.querySelector('user .selected').getAttribute('value')),
			method: 'POST',
			success: api.postBuy,
			error: xhr => {
				document.getElementsByTagName('error')[0].innerHTML = xhr.status < 500 ? 'The server is unavailable. Please try again later.' : 'PDF creation failed. Please try again later.';
			}
		});
	}

	static delete() {
		api.ajax({
			url: api.url + '/rest/api/pdf/' + document.querySelector('id').innerText,
			method: 'DELETE',
			success: () => {
				document.getElementsByTagName('attributes')[0].style.display = null;
				document.getElementsByTagName('upload')[0].style.display = null;
				api.feedbackStatus = 'You already deleted the uploaded data. Please give feedback before deleting the data.';
				document.querySelectorAll('input[type="file"]').forEach(e => e.value = '');
			}
		});
	}

	static saveFeedback() {
		var body = {
			rating: document.querySelector('popup input-rating').getAttribute('value'),
			id: document.querySelector('popup input[name="id"]').value,
			pin: document.querySelector('popup input[name="pin"]').value,
			note: document.querySelector('popup textarea[name="note"]').value,
			name: document.querySelector('popup input[name="name"]').value,
			email: document.querySelector('popup input[name="email"]').value
		};
		if (!body.email || !body.name || !body.note) {
			document.querySelector('popup content error').innerHTML = 'Please enter all fields.';
			return;
		}
		api.ajax({
			url: api.url + '/rest/api/feedback/' + document.querySelector('id').innerText,
			method: 'PUT',
			body: body,
			success: xhr => {
				document.querySelector('popup content message').innerHTML = xhr;
				document.querySelector('popup content data').style.display = '';
				api.feedbackStatus = 'You already successfully added a feedback. You may edit it with the link in the email we sent you.';
			},
			error: xhr => {
				document.querySelector('popup content error').innerHTML = xhr.status < 500 ? 'The server is unavailable. Please try again later.' : 'Saving feedback failed: ' + xhr.responseText;
			}
		});
	}

	static feedback() {
		api.ajax({
			hideProgressBar: true,
			url: api.url + '/rest/api/feedback/list',
			success: xhr => {
				var s = '';
				var formatTime = function (s) {
					var d = new Date(s.replace('+00:00', ''));
					d = new Date(Date.UTC(d.getFullYear(), d.getMonth(), d.getDate(), d.getHours(), d.getMinutes(), d.getSeconds()))
					return d.toLocaleString();
				};
				for (var i = 0; i < xhr.length; i++)
					s += '<rating><input-rating rating="' + xhr[i].rating + '"></input-rating><top>' + formatTime(xhr[i].modifiedAt) + '<br/>' + xhr[i].name + '</top><note>' + xhr[i].note.replace(/\n/g, '<br/>') + '</note></rating>';
				document.querySelector('feedbacks').innerHTML = s;
			}
		});
	}

	static postAnalyse(data) {
		if (!data) {
			document.getElementsByTagName('error')[0].innerHTML = 'PDF creation failed. Is it a WhatsApp exported chat file?';
			return;
		}
		document.getElementsByTagName('attributes')[0].style.display = 'block';
		document.getElementsByTagName('upload')[0].style.display = 'none';
		document.querySelector('attributes button[onclick*="delete"]').style.display = '';
		document.querySelector('attributes id').innerText = data.id;
		ui.showTab(1);
		var s = '<table><tr><th>Period</th><th>Chats</th><th>Words</th><th>Letters</th><th></th></tr>';
		for (var i = 0; i < data.periods.length; i++)
			s += '<tr value="' + data.periods[i].period + '"><td>' + data.periods[i].period.replace('-\\d\\d', '').replace('\\d\\d/', '').replace('\\d\\d.', '') + '</td><td>' + data.periods[i].chats.toLocaleString() + '</td><td>' + data.periods[i].words.toLocaleString() + '</td><td>' + data.periods[i].letters.toLocaleString() + '</td><td><button onclick="api.preview(event, &quot;' + data.periods[i].period.replaceAll('\\', '\\\\') + '&quot;)">Preview</button></td></tr>';
		document.getElementsByTagName('attributes')[0].querySelector('period').innerHTML = s + '</table>';
		document.getElementsByTagName('attributes')[0].querySelectorAll('period tr').forEach(tr => {
			tr.addEventListener('click', () => {
				document.querySelector('period').classList.remove('error');
				if (tr.classList.contains('download')) {
					var link = document.createElement('a');
					link.setAttribute('href', api.url + '/rest/api/pdf/' + document.querySelector('id').innerText + '/true?period=' + encodeURIComponent(tr.getAttribute('value')));
					link.setAttribute('target', '_blank');
					link.click();
					tr.classList.remove('download');
					if (document.querySelectorAll('period .spinner,period .download').length == 0)
						document.querySelector('attributes button[onclick*="delete"]').style.display = '';
					api.feedbackStatus = '';
				} else if (tr.classList.contains('selected'))
					tr.classList.remove('selected');
				else if (!tr.classList.contains('spinner'))
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
		api.feedbackStatus = 'Download one of your printed documents, then you can give feedback.';
		document.querySelector('attributes button[onclick*="delete"]').style.display = 'none';
		var periods = document.querySelectorAll('period .selected');
		for (var i = 0; i < periods.length; i++) {
			api.download(periods[i].getAttribute('value'));
			var tr = document.querySelector('period tr[value="' + periods[i].getAttribute('value').replaceAll('\\', '\\\\') + '"]');
			tr.classList.remove('selected');
			tr.classList.add('spinner');
		}
	}

	static download(period) {
		api.showProgressBar();
		var download = function () {
			api.ajax({
				hideProgressBar: true,
				url: api.url + '/rest/api/pdf/' + document.querySelector('id').innerText + '/false' + (period ? '?period=' + encodeURIComponent(period) : ''),
				method: 'GET',
				success: () => {
					api.hideProgressBar();
					if (period) {
						var tr = document.querySelector('period tr[value="' + period.replaceAll('\\', '\\\\') + '"]');
						tr.classList.remove('spinner');
						tr.classList.add('download');
					} else {
						var link = document.createElement('a');
						link.setAttribute('href', api.url + '/rest/api/pdf/' + document.querySelector('id').innerText + '/true');
						link.setAttribute('target', '_blank');
						link.click();
					}
				},
				error: xhr => {
					var error = xhr.responseText || 'Unknown error';
					if (error.indexOf('"status":566,') > -1)
						setTimeout(function () { download(period); }, 1000);
					else {
						if (xhr.status < 500)
							error = 'The server is unavailable. Please try again later.';
						else if (error.indexOf('Invalid ID') > -1) {
							document.getElementsByTagName('attributes')[0].style.display = null;
							document.getElementsByTagName('upload')[0].style.display = '';
							document.getElementById('chatFile').value = '';
							error = 'Uploaded chat already deleted. Please upload new chat.';
						} else {
							if (period) {
								var tr = document.querySelector('period tr[value="' + period.replaceAll('\\', '\\\\') + '"]').classList;
								tr.remove('spinner');
								tr.add('selected');
							}
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
				if (xhr.status < 300 || !param.hideProgressBar)
					api.hideProgressBar();
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
				} else {
					if (api.status < 500) {
						var xhrError = new XMLHttpRequest();
						xhrError.open('POST', api.url + '/rest/api/ticket', true);
						xhrError.setRequestHeader('Content-Type', 'application/json');
						xhrError.send(JSON.stringify({ note: xhr.status + ' ' + xhr.responseURL + '\n' + xhr.response }));
					}
					if (param.error) {
						xhr.param = param;
						param.error(xhr);
					} else
						document.getElementsByTagName('error')[0].innerHTML = 'An error occurred while processing your request. Please try again later.';
				}
			}
		};
		xhr.open(param.method ? param.method : 'GET', param.url, true);
		if (typeof param.body == 'string')
			xhr.setRequestHeader('Content-Type', 'application/x-www-form-urlencoded');
		else if (param.body && !(param.body instanceof FormData)) {
			xhr.setRequestHeader('Content-Type', 'application/json');
			param.body = JSON.stringify(param.body);
		}
		if (!param.hideProgressBar)
			api.showProgressBar();
		xhr.send(param.body);
	}

	static hideProgressBar() {
		var e = document.getElementsByTagName('progressbar')[0];
		e.addEventListener("transitionend", event => { e.style.display = null; }, { once: true });
		e.style.opacity = null;
	}

	static showProgressBar() {
		document.getElementsByTagName('error')[0].innerHTML = '';
		var e = document.getElementsByTagName('progressbar')[0].style;
		e.display = 'block';
		setTimeout(function() { e.opacity = 1; }, 100);
	}
}

