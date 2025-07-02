export { api };

class api {
	static url = 'https://wa2pdf.com/rest/sc/';
	static data = { log: [], ticket: [] };

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
				api.data.ticket = xhr.tickets;
				api.data.log = xhr.logs;
				var formatTime = function (s) {
					var d = new Date(s.replace('+00:00', ''));
					d = new Date(Date.UTC(d.getFullYear(), d.getMonth(), d.getDate(), d.getHours(), d.getMinutes(), d.getSeconds()))
					return d.getDate() + '.' + (d.getMonth() + 1) + ' ' + d.getHours() + ':' + d.getMinutes() + ':' + d.getSeconds();
				};
				var narrowView = window.outerWidth < 700;
				var s = '<table><thead><tr>';
				if (!narrowView)
					s += '<th [[w1]]>id</th>';
				s += '<th [[w2]]>createdAt</th><th [[w3]]>note</th></tr></thead>';
				for (var i = 0; i < xhr.tickets.length; i++) {
					s += '<tr>';
					if (!narrowView)
						s += '<td [[w1]]>' + xhr.tickets[i].id + '</td>';
					s += '<td onclick="api.open(event)" i="ticket-' + i + '" class="clickable" [[w2]]>' + formatTime(xhr.tickets[i].createdAt) + '</td><td [[w3]]>' + api.sanitizeText(xhr.tickets[i].note) + '<button onclick="api.deleteTicket(event, ' + xhr.tickets[i].id + ')">delete</button></td></tr>';
				}
				document.querySelector('tickets').innerHTML = api.replaceWidths(narrowView ? [0, 20, 80] : [5, 10, 85], s) + '</table>';
				api.renderLog(xhr.logs);
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
				document.querySelector('output pre').innerHTML = api.sanitizeText(xhr);
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
		document.querySelector('output pre').innerHTML = '';
	}

	static open(event) {
		if (event.target.getAttribute('i') == document.querySelector('popup content').getAttribute('i')) {
			api.popupClose();
			return;
		}
		var id = event.target.getAttribute('i').split('-');
		var data = api.data[id[0]][id[1]];
		var keys = Object.keys(data);
		var s = '';
		for (var i = 0; i < keys.length; i++) {
			if (data[keys[i]])
				s += '<label>' + keys[i] + '</label><value>' + api.sanitizeText(data[keys[i]]) + '</value>';
		}
		document.querySelector('popup content').innerHTML = s;
		document.querySelector('popup content').setAttribute('i', event.target.getAttribute('i'));
		document.querySelector('popup').style.transform = 'scale(1)';
	}

	static log() {
		api.ajax({
			url: api.url + 'log?search=' + encodeURIComponent(document.querySelector('input[name="search"]').value),
			success: api.renderLog
		});
	}

	static popupClose() {
		document.getElementsByTagName('popup')[0].style.transform = '';
		document.querySelector('popup content').removeAttribute('i');
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
						document.querySelector('output pre').innerHTML = JSON.stringify(xhr);
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
		return s && s.replace ? s.replace(/\n/g, '<br/>').replace(/\t/g, '&nbsp;&nbsp;&nbsp;&nbsp;') : s ? s : '';
	}

	static renderLog(logs) {
		var narrowView = window.outerWidth < 700;
		var s = '<table><thead><tr>';
		if (!narrowView)
			s += '<th [[w1]]>id</th>';
		s += '<th [[w2]]>createdAt</th><th [[w3]]>status</th><th [[w4]]>ip</th><th [[w5]]>time</th><th [[w6]]>uri</th>';
		if (!narrowView)
			s += '<th [[w7]]>referer</th>';
		s += '</tr></thead>';
		for (var i = 0; i < logs.length; i++) {
			s += '<tr>';
			if (!narrowView)
				s += '<td [[w1]]>' + logs[i].id + '</td>';
			s += '<td onclick="api.open(event)" i="log-' + i + '" class="clickable" [[w2]]>' + formatTime(logs[i].createdAt) + '</td>' +
				'<td [[w3]]>' + logs[i].status + '</td>' +
				'<td [[w4]]>' + (logs[i].ip ? '<a href="https://whatismyipaddress.com/ip/' + logs[i].ip + '" target="sc_ip">' + logs[i].ip + '</a>' : '') + '</td>' +
				'<td [[w5]]>' + logs[i].time + '</td>' +
				'<td [[w6]]>' + logs[i].method + ' ' + logs[i].uri + (logs[i].query ? '?' + logs[i].query : '') + (logs[i].body ? '<br/>' + api.sanitizeText(logs[i].body) : '') + '</td>';
			if (!narrowView)
				s += '<td [[w7]]>' + logs[i].referer + '</td>';
			s += '</tr>';
		}
		document.querySelector('logs').innerHTML = api.replaceWidths(narrowView ? [0, 20, 10, 15, 10, 45] : [5, 10, 5, 10, 10, 25, 35], s) + '</table>';
	}

	static replaceWidths(widths, s) {
		for (var i = 0; i < widths.length; i++)
			s = s.replaceAll(' [[w' + (i + 1) + ']]>', ' style="width:' + widths[i] + '%;">');
		return s;
	}
}

window.api = api;
