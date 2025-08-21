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
				ui.renderTicket();
				ui.data.log = xhr.logs;
				ui.renderLog();
				document.querySelector('input[name="searchLogs"]').value = xhr.search;
			}
		});
	}

	static build(type) {
		api.ajax({
			url: api.url + 'build/' + type,
			method: 'POST',
			success: xhr => {
				ui.popupOpen('<pre>' + ui.sanitizeText(xhr) + '</pre>');
			}
		});
	}

	static deleteTicket(event, id) {
		api.ajax({
			url: api.url + 'ticket/' + id,
			method: 'DELETE',
			success: xhr => {
				var e = document.querySelector('tickets tr[i="' + id + '"]');
				if (e)
					e.outerHTML = '';
				ui.popupClose();
			}
		});
	}

	static log(event) {
		if (event && event.keyCode == 13) {
			api.ajax({
				url: api.url + 'log?search=' + encodeURIComponent(document.querySelector('input[name="searchLogs"]').value),
				success: xhr => {
					ui.data.log = xhr;
					ui.renderLog();
				}
			});
		}
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
						ui.popupOpen('<pre>' + JSON.stringify(xhr) + '</pre>');
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
	static data = { log: [], ticket: [], multiline: true };

	static open(event) {
		if (event.target.getAttribute('i') == document.querySelector('popup content').getAttribute('i')) {
			ui.popupClose();
			return;
		}
		var id = event.target.getAttribute('i').split('-');
		var data;
		for (var i = 0; i < ui.data[id[0]].length; i++) {
			if (ui.data[id[0]][i].id == id[1]) {
				data = ui.data[id[0]][i];
				break;
			}
		}
		var keys = Object.keys(data);
		var s = '';
		for (var i = 0; i < keys.length; i++) {
			if (data[keys[i]]) {
				if (keys[i] == 'id')
					id[1] = data[keys[i]];
				s += '<label>' + keys[i] + '</label><value>' + ui.sanitizeText(data[keys[i]]) + '</value>';
			}
		}
		if (id[0] == 'ticket')
			s += '<buttons><button onclick="api.deleteTicket(event, ' + id[1] + ')">delete</button></buttons>';
		document.querySelector('popup content').setAttribute('i', event.target.getAttribute('i'));
		ui.popupOpen(s);
	}

	static popupOpen(s, right) {
		if (document.querySelector('popup content').innerHTML == s) {
			ui.popupClose();
			return;
		}
		document.querySelector('popup content').innerHTML = s;
		var e = document.querySelector('popup').style;
		e.transform = 'scale(1)';
		e.left = right ? 'initial' : '';
		e.right = right ? '1em' : '';
	}

	static popupClose() {
		document.getElementsByTagName('popup')[0].style.transform = '';
		document.querySelector('popup content').removeAttribute('i');
		setTimeout(function() { document.querySelector('popup content').innerHTML = ''; }, 500);
	}

	static filter(event, field) {
		var e = event.target;
		while (e && e.nodeName != 'FILTER')
			e = e.parentElement;
		var value = e && field + '-' + e.querySelector('entry').innerText.trim();
		if ((field || field == 0) && (!value || value != document.querySelector('logs').getAttribute('filter'))) {
			if (!e)
				return;
			document.querySelector('logs').setAttribute('filter', value);
		} else
			document.querySelector('logs').removeAttribute('filter');
		ui.renderLog();
	}

	static columnIndex(column) {
		var trs = document.querySelector('logs tr').querySelectorAll('th');
		column = column.trim();
		for (var i = 0; i < trs.length; i++) {
			if (trs[i].innerText == column)
				return i;
		}
	}

	static openFilter(event) {
		document.querySelector('logs').removeAttribute('filter');
		var field = ui.columnIndex(event.target.innerText);
		var s = '';
		var processed = [], value;
		var logs = ui.convertLogData();
		for (var i = 0; i < logs.length; i++) {
			value = logs[i][field + (ui.isNarrowView() ? 1 : 0)];
			if (value) {
				if (value.indexOf('<br/>') > -1)
					value = value.substring(0, value.indexOf('<br/>'));
				processed[value] = processed[value] ? processed[value] + 1 : 1;
			}
		}
		var sorted = Object.keys(processed).sort((a, b) => processed[b] - processed[a] == 0 ? (a > b ? 1 : -1) : processed[b] - processed[a]);
		for (var i = 0; i < sorted.length; i++)
			s += '<filter onclick="ui.filter(event,' + field + ')"><entry>' + sorted[i] + '</entry><count>' + processed[sorted[i]] + '</count></filter>';
		ui.popupOpen(s, true);
	}

	static sanitizeText(s) {
		s = s && s.replace ? s.replace(/\n/g, '<br/>').replace(/\t/g, '&nbsp;&nbsp;&nbsp;&nbsp;') : s ? s : '';
		if (!ui.data.multiline && s.indexOf('<br/>') > -1)
			s = s.substring(0, s.indexOf('<br/>'));
		return s;
	}

	static formatTime(s) {
		var d = new Date(s.replace('+00:00', ''));
		d = new Date(Date.UTC(d.getFullYear(), d.getMonth(), d.getDate(), d.getHours(), d.getMinutes(), d.getSeconds()))
		return d.getDate() + '.' + (d.getMonth() + 1) + ' ' + d.getHours() + ':' + d.getMinutes() + ':' + d.getSeconds();
	}

	static isNarrowView() {
		return window.outerWidth < 700;
	}

	static convertLogData() {
		var d = [];
		for (var i = 0; i < ui.data.log.length; i++) {
			var row = [];
			row.push(ui.data.log[i].id);
			row.push(ui.formatTime(ui.data.log[i].createdAt));
			row.push(ui.data.log[i].logStatus);
			row.push(ui.data.log[i].ip);
			row.push(ui.data.log[i].time);
			row.push(ui.data.log[i].method + ' ' + ui.data.log[i].uri + (ui.data.log[i].query ? '?' + ui.data.log[i].query : '') + ui.sanitizeText(ui.data.log[i].body ? '<br/>' + ui.data.log[i].body : ''));
			row.push(ui.data.log[i].referer);
			d.push(row);
		}
		return d;
	}
	
	static renderLog() {
		var filter = document.querySelector('logs').getAttribute('filter');
		var d = ui.convertLogData();
		var narrowView = ui.isNarrowView();
		var s = '<table><thead><tr>';
		if (!narrowView)
			s += '<th onclick="ui.sortColumn(event)" class="clickable" [[w1]]>id</th>';
		s += '<th [[w2]]>createdAt</th><th onclick="ui.openFilter(event)" class="clickable" [[w3]]>status</th><th onclick="ui.openFilter(event)" class="clickable" [[w4]]>ip</th><th onclick="ui.sortColumn(event)" class="clickable" [[w5]]>time</th><th onclick="ui.openFilter(event)" class="clickable" [[w6]]>uri</th>';
		if (!narrowView)
			s += '<th onclick="ui.openFilter(event)" class="clickable" [[w7]]>referer</th>';
		s += '</tr></thead>';
		var sort = document.querySelector('logs').getAttribute('sort');
		if (sort) {
			var column = parseInt(sort.substring(0, sort.indexOf('-'))) + (narrowView ? 1 : 0);
			var factor = sort.indexOf('-asc') > 0 ? 1 : -1;
			d = d.sort((a, b) => (typeof a[column] == 'string' ? a[column].localeCompare(b[column]) : a[column] - b[column]) * factor);
		}
		for (var i = 0; i < d.length; i++) {
			if (!filter || d[i][parseInt(filter.substring(0, filter.indexOf('-'))) + (narrowView ? 1 : 0)] == filter.substring(filter.indexOf('-') + 1)
			   	|| d[i][parseInt(filter.substring(0, filter.indexOf('-'))) + (narrowView ? 1 : 0)].indexOf(filter.substring(filter.indexOf('-') + 1) + '<br/>') == 0) {
				s += '<tr>';
				if (!narrowView)
					s += '<td [[w1]]>' + d[i][0] + '</td>';
				s += '<td onclick="ui.open(event)" i="log-' + d[i][0] + '" class="clickable" [[w2]]>' + d[i][1] + '</td>' +
					'<td [[w3]]>' + d[i][2] + '</td>' +
					'<td [[w4]]>' + (d[i][3] ? '<a href="https://whatismyipaddress.com/ip/' + d[i][3] + '" target="sc_ip">' + d[i][3] + '</a>' : '') + '</td>' +
					'<td [[w5]]>' + d[i][4] + '</td>' +
					'<td [[w6]]>' + d[i][5] + '</td>';
				if (!narrowView)
					s += '<td [[w7]]>' + d[i][6] + '</td>';
				s += '</tr>';
			}
		}
		document.querySelector('logs').innerHTML = ui.replaceWidths(narrowView ? [0, 20, 10, 15, 10, 45] : [5, 10, 5, 10, 10, 25, 35], s) + '</table>';
		document.querySelector('msg').innerHTML = (document.querySelectorAll('logs tr').length - 1) + ' log entries';
		document.querySelector('logs tr').querySelectorAll('th').forEach(e => e.classList.remove('asc', 'desc'));
		if (sort)
			document.querySelector('logs tr').querySelectorAll('th')[parseInt(sort.substring(0, sort.indexOf('-')))].classList.add(sort.indexOf('-asc') > 0 ? 'asc' : 'desc');
	}

	static renderTicket() {
		var narrowView = ui.isNarrowView();
		var s = '<table><thead><tr>';
		if (!narrowView)
			s += '<th [[w1]]>id</th>';
		s += '<th [[w2]]>createdAt</th><th onclick="ui.sortColumn(event)" class="clickable" [[w3]]>note</th></tr></thead>';
		for (var i = 0; i < ui.data.ticket.length; i++) {
			s += '<tr i="' + ui.data.ticket[i].id + '">';
			if (!narrowView)
				s += '<td [[w1]]>' + ui.data.ticket[i].id + '</td>';
			s += '<td onclick="ui.open(event)" i="ticket-' + ui.data.ticket[i].id + '" class="clickable" [[w2]]>' + ui.formatTime(ui.data.ticket[i].createdAt) + '</td><td [[w3]]>' + ui.sanitizeText(ui.data.ticket[i].note) + '</td></tr>';
		}
		document.querySelector('tickets').innerHTML = ui.replaceWidths(narrowView ? [0, 20, 80] : [5, 10, 85], s) + '</table>';
	}

	static replaceWidths(widths, s) {
		for (var i = 0; i < widths.length; i++)
			s = s.replaceAll(' [[w' + (i + 1) + ']]>', ' style="width:' + widths[i] + '%;">');
		return s;
	}

	static resetSize() {
		document.querySelectorAll('body container>element').forEach(e => {
			e.children[0].style.height = '';
		});

	}

	static showTab(i) {
		document.querySelector('tabBody container').style.marginLeft = -(i * 100) + '%';
		document.querySelector('tab.selected')?.classList.remove('selected');
		document.querySelectorAll('tab')[i].classList.add('selected');
	}

	static sortColumn(event) {
		var field = ui.columnIndex(event.target.innerText);
		var sort = document.querySelector('logs').getAttribute('sort');
		if (!sort)
			document.querySelector('logs').setAttribute('sort', field + '-asc');
		else if (sort == field + '-asc')
			document.querySelector('logs').setAttribute('sort', field + '-desc');
		else
			document.querySelector('logs').removeAttribute('sort');
		ui.renderLog();
	}

	static toggleMultiline() {
		ui.data.multiline = !ui.data.multiline;
		ui.renderLog();
		ui.renderTicket();
	}
}

window.api = api;
window.ui = ui;
