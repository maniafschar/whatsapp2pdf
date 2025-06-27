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
				var formatTime = function(d) {
					d = new Date(d.replace('+00:00', ''));
					return d.getDate() + '.' + (d.getMonth() + 1) + ' ' + d.getHours() + ':' + d.getMinutes() + ':' d.getSeconds();
				};
				var replaceWidths = function(widths, s) {
					for (var i = 0; i < widths.length; i++)
						s = s.replaceAll(' [[w' + (i + 1) + ']]>', ' style="width:' + widths[i] + '%;">');
					return s;
				};
				var narrowView = window.outerWidth < 700;
				var s = '<table><thead><tr><th [[w1]]>id</th><th [[w2]]>createdAt</th><th [[w3]]>note</th></tr></thead>';
				for (var i = 0; i < xhr.tickets.length; i++)
					s += '<tr><td [[w1]]>' + xhr.tickets[i].id + '</td><td [[w2]]>' + formatTime(xhr.tickets[i].createdAt) + '</td><td [[w3]]>' + api.sanitizeText(xhr.tickets[i].note) + '<button onclick="api.deleteTicket(event, ' + xhr.tickets[i].id + ')">delete</button></td></tr>';
				document.querySelector('tickets').innerHTML = replaceWidths([5, 10, 85], s) + '</table>';
				s = '<table><thead><tr><th [[w1]]>id</th><th [[w2]]>createdAt</th><th [[w3]]>status</th><th [[w4]]>ip</th><th [[w5]]>uri</th><th [[w6]]>time</th>';
				if (!narrowView)
					s += '<th [[w7]]>body</th><th [[w8]]>referer</th>';
				s += '</tr></thead>';
				for (var i = 0; i < xhr.logs.length; i++) {
					s += '<tr><td [[w1]]>' + xhr.logs[i].id + '</td><td [[w2]]>' + formatTime(xhr.logs[i].createdAt) + '</td><td [[w3]]>' + xhr.logs[i].status + '</td><td [[w4]]>' + (xhr.logs[i].ip ? '<a href="https://whatismyipaddress.com/ip/' + xhr.logs[i].ip + '" target="sc_ip">' + xhr.logs[i].ip + '</a>' : '') + '</td><td [[w5]]>' + xhr.logs[i].method + ' ' + xhr.logs[i].uri + (xhr.logs[i].query ? '?' + xhr.logs[i].query : '') + '</td><td [[w6]]>' + xhr.logs[i].time + '</td>';
					if (!narrowView)
						s += '<td [[w7]]>' + api.sanitizeText(xhr.logs[i].body) + '</td><td [[w8]]>' + xhr.logs[i].referer + '</td>';
					s += '</tr>';
				}
				document.querySelector('logs').innerHTML = replaceWidths(narrowView ? [10, 20, 10, 15, 35, 10] :  [5, 10, 5, 10, 15, 5, 15, 35], s) + '</table>';
				document.querySelector('msg').innerHTML = xhr.logs.length + ' log entries';
			}
		});
	}

	static build(type) {
		api.clear();
		api.ajax({
			url: api.url + 'build/' + type,
			method: 'POST',
			success: xhr => {
				document.querySelector('output').innerHTML = api.sanitizeText(xhr);
			}
		});
	}
	static deleteTicket(event, id) {
		api.ajax({
			url: api.url + 'ticket/' + id,
			method: 'DELETE',
			success: xhr => {
				var e = event.target;
				while (e && e.nodeName != 'TR')
					e = e.parentElement;
				if (e)
					e.outerHTML = '';
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
					document.querySelector('progressbar').style.display = null;
					if (param.error) {
						xhr.param = param;
						param.error(xhr);
					} else
						document.querySelector('output').innerHTML = JSON.stringify(xhr);
				};
				if (xhr.status >= 200 && xhr.status < 300) {
					document.getElementsByTagName('progressbar')[0].style.display = null;
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
		document.getElementsByTagName('progressbar')[0].style.display = 'block';
		xhr.send(param.body);
	}

	static sanitizeText(s) {
		return s ? s.replace(/\n/g, '<br/>').replace(/\t/g, '&nbsp;&nbsp;&nbsp;&nbsp;') : '';
	}
}

window.api = api;
