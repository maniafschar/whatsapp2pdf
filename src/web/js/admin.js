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
				ui.data.ticket = xhr.tickets;
				var narrowView = ui.isNarrowView();
				var s = '<table><thead><tr>';
				if (!narrowView)
					s += '<th [[w1]]>id</th>';
				s += '<th [[w2]]>createdAt</th><th [[w3]]>note</th></tr></thead>';
				for (var i = 0; i < xhr.tickets.length; i++) {
					s += '<tr>';
					if (!narrowView)
						s += '<td [[w1]]>' + xhr.tickets[i].id + '</td>';
					s += '<td onclick="ui.open(event)" i="ticket-' + i + '" class="clickable" [[w2]]>' + ui.formatTime(xhr.tickets[i].createdAt) + '</td><td [[w3]]>' + ui.sanitizeText(xhr.tickets[i].note) + '</td></tr>';
				}
				s += '<tr><td>&nbsp;</td></tr>';
				document.querySelector('tickets').innerHTML = ui.replaceWidths(narrowView ? [0, 20, 80] : [5, 10, 85], s) + '</table>';
				ui.renderLog(xhr.logs);
				document.querySelector('input[name="searchLogs"]').value = xhr.search;
			}
		});
	}

	static build(type) {
		ui.clear();
		api.ajax({
			url: api.url + 'build/' + type,
			method: 'POST',
			success: xhr => {
				document.querySelector('output pre').innerHTML = ui.sanitizeText(xhr);
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

	static log(event) {
		if (event && event.keyCode == 13)
			api.ajax({
				url: api.url + 'log?search=' + encodeURIComponent(document.querySelector('input[name="searchLogs"]').value),
				success: ui.renderLog
			});
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
}

class ui {
	static data = { log: [], ticket: [] };

	static clear() {
		document.querySelector('output pre').innerHTML = '';
	}

	static open(event) {
		if (event.target.getAttribute('i') == document.querySelector('popup content').getAttribute('i')) {
			ui.popupClose();
			return;
		}
		var id = event.target.getAttribute('i').split('-');
		var data = ui.data[id[0]][id[1]];
		var keys = Object.keys(data);
		var s = '';
		for (var i = 0; i < keys.length; i++) {
			if (data[keys[i]])
				s += '<label>' + keys[i] + '</label><value>' + ui.sanitizeText(data[keys[i]]) + '</value>';
		}
		if (id[0] == 'ticket')
			s += '<buttons><button onclick="api.deleteTicket(event, ' + id[1] + ')">delete</button></buttons>';
		document.querySelector('popup content').setAttribute('i', event.target.getAttribute('i'));
		ui.popupOpen(s);
	}

	static popupOpen(s, right) {
		document.querySelector('popup content').innerHTML = s;
		var e = document.querySelector('popup').style;
		e.transform = 'scale(1)';
		e.left = right ? 'initial' : '';
		e.right = right ? '1em' : '';
	}

	static popupClose() {
		document.getElementsByTagName('popup')[0].style.transform = '';
		document.querySelector('popup content').removeAttribute('i');
	}

	static filter(event, field) {
		if (field) {
			var e = event.target;
			while (e && e.nodeName != 'FILTER')
				e = e.parentElement;
			if (!e)
				return;
			var value = e.querySelector('entry').innerText.trim();
			var trs = document.querySelectorAll('logs tr th');
			trs = document.querySelectorAll('logs tr');
			for (var i = 1; i < trs.length; i++)
				trs[i].style.display = trs[i].querySelectorAll('td')[field].innerText.trim() == value ? 'block' : 'none';
		} else {
			var trs = document.querySelectorAll('logs tr');
			for (var i = 1; i < trs.length; i++)
				trs[i].style.display = 'block';
		}
		document.querySelector('msg').innerHTML = (ui.data.log.length - document.querySelectorAll('logs tr[style*="none"]').length) + ' log entries';
	}

	static openFilter(event) {
		var field = event.target.innerText.trim();
		var trs = document.querySelector('logs tr').querySelectorAll('th');
		for (var i = 0; i < trs.length; i++) {
			if (trs[i].innerText == field) {
				field = i;
				break;
			}
		}
		var s = '<filter onclick="ui.filter(event)"><entry>All</entry><count>' + ui.data.log.length + '</count></filter>';
		var processed = [], value;
		trs = document.querySelectorAll('logs tr');
		for (var i = 1; i < trs.length; i++) {
			value = trs[i].querySelectorAll('td')[field].innerText;
			if (value)
				processed[value] = processed[value] ? processed[value] + 1 : 1;
		}
		var sorted = Object.keys(processed).sort((a, b) => processed[b] - processed[a] == 0 ? (a > b ? 1 : -1) : processed[b] - processed[a]);
		for (var i = 0; i < sorted.length; i++)
			s += '<filter onclick="ui.filter(event,' + field + ')"><entry>' + sorted[i] + '</entry><count>' + processed[sorted[i]] + '</count></filter>';
		ui.popupOpen(s, true);
	}

	static sanitizeText(s) {
		return s && s.replace ? s.replace(/\n/g, '<br/>').replace(/\t/g, '&nbsp;&nbsp;&nbsp;&nbsp;') : s ? s : '';
	}

	static formatTime(s) {
		var d = new Date(s.replace('+00:00', ''));
		d = new Date(Date.UTC(d.getFullYear(), d.getMonth(), d.getDate(), d.getHours(), d.getMinutes(), d.getSeconds()))
		return d.getDate() + '.' + (d.getMonth() + 1) + ' ' + d.getHours() + ':' + d.getMinutes() + ':' + d.getSeconds();
	}

	static isNarrowView() {
		return window.outerWidth < 700;
	}

	static renderLog(logs) {
		ui.data.log = logs;
		var narrowView = ui.isNarrowView();
		var s = '<table><thead><tr>';
		if (!narrowView)
			s += '<th [[w1]]>id</th>';
		s += '<th [[w2]]>createdAt</th><th onclick="ui.openFilter(event)" class="clickable" [[w3]]>status</th><th onclick="ui.openFilter(event)" class="clickable" [[w4]]>ip</th><th [[w5]]>time</th><th onclick="ui.openFilter(event)" class="clickable" [[w6]]>uri</th>';
		if (!narrowView)
			s += '<th onclick="ui.openFilter(event)" class="clickable" [[w7]]>referer</th>';
		s += '</tr></thead>';
		for (var i = 0; i < logs.length; i++) {
			s += '<tr>';
			if (!narrowView)
				s += '<td [[w1]]>' + logs[i].id + '</td>';
			s += '<td onclick="ui.open(event)" i="log-' + i + '" class="clickable" [[w2]]>' + ui.formatTime(logs[i].createdAt) + '</td>' +
				'<td [[w3]]>' + logs[i].status + '</td>' +
				'<td [[w4]]>' + (logs[i].ip ? '<a href="https://whatismyipaddress.com/ip/' + logs[i].ip + '" target="sc_ip">' + logs[i].ip + '</a>' : '') + '</td>' +
				'<td [[w5]]>' + logs[i].time + '</td>' +
				'<td [[w6]]>' + logs[i].method + ' ' + logs[i].uri + (logs[i].query ? '?' + logs[i].query : '') + (logs[i].body ? '<br/>' + ui.sanitizeText(logs[i].body) : '') + '</td>';
			if (!narrowView)
				s += '<td [[w7]]>' + logs[i].referer + '</td>';
			s += '</tr>';
		}
		s += '<tr><td>&nbsp;</td></tr>';
		document.querySelector('logs').innerHTML = ui.replaceWidths(narrowView ? [0, 20, 10, 15, 10, 45] : [5, 10, 5, 10, 10, 25, 35], s) + '</table>';
		document.querySelector('msg').innerHTML = logs.length + ' log entries';
	}

	static replaceWidths(widths, s) {
		for (var i = 0; i < widths.length; i++)
			s = s.replaceAll(' [[w' + (i + 1) + ']]>', ' style="width:' + widths[i] + '%;">');
		return s;
	}
}

window.api = api;
window.ui = ui;
