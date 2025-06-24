export { api };

class api {
	static url = 'https://wa2pdf.com/rest/sc/';

	static init() {
		if (document.querySelector('login input').value) {
			window.localStorage.setItem('credentials', document.querySelector('login input').value);
			document.querySelector('login').style.display = 'none';
			document.querySelector('login input').value = '';
		}
		if (!window.localStorage.getItem('credentials')) {
			document.querySelector('login').style.display = 'block';
			return;
		}
		api.ajax({
			url: api.url + 'init',
			success: xhr => {
				var s = '<table><tr><th>id</th><th>createdAt</th><th>note</th></tr>';
				for (var i = 0; i < xhr.tickets.length; i++)
					s += '<tr><td>' + xhr.tickets[i].id + '</td><td>' + new Date(xhr.tickets[i].createdAt.replace('+00:00', '')).toLocaleString().replace(' ', '&nbsp;') + '</td><td>' + xhr.tickets[i].note.replace(/\n/g, '<br/>').replace(/\t/g, '&nbsp;&nbsp;&nbsp;&nbsp;') + '</td></tr>';
				document.querySelector('tickets').innerHTML = s + '</table>';
				s = '<table><tr><th>id</th><th>createdAt</th><th>status</th><th>method</th><th>uri</th><th>port</th><th>query</th><th>time</th><th>ip</th><th>body</th><th>referer</th></tr>';
				for (var i = 0; i < xhr.logs.length; i++)
					s += '<tr><td>' + xhr.logs[i].id + '</td><td>' + new Date(xhr.logs[i].createdAt.replace('+00:00', '')).toLocaleString().replace(' ', '&nbsp;') + '</td><td>' + xhr.logs[i].status + '</td><td>' + xhr.logs[i].method + '</td><td>' + xhr.logs[i].uri + '</td><td>' + xhr.logs[i].port + '</td><td>' + xhr.logs[i].query + '</td><td>' + xhr.logs[i].time + '</td><td>' + xhr.logs[i].ip + '</td><td>' + xhr.logs[i].body + '</td><td>' + xhr.logs[i].referer + '</td></tr>';
				document.querySelector('logs').innerHTML = s + '</table>';
			}
		});
	}

	static build(type) {
		api.clear();
		api.ajax({
			url: api.url + 'build/' + type,
			method: 'POST',
			success: xhr => {
				document.querySelector('output').innerHTML = xhr;
			}
		});
	}

	static clear() {
		document.querySelector('output').innerHTML = '';
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
						document.querySelector('progressbar').style.display = null;
						document.querySelector('output').innerHTML = JSON.stringify(xhr);
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
		xhr.setRequestHeader('user', window.localStorage.getItem('credentials'));
		if (typeof param.body == 'string')
			xhr.setRequestHeader('Content-Type', 'application/x-www-form-urlencoded');
		else if (param.body && !(param.body instanceof FormData)) {
			xhr.setRequestHeader('Content-Type', 'application/json');
			param.body = JSON.stringify(param.body);
		}
		xhr.send(param.body);
	}
}

window.api = api;
