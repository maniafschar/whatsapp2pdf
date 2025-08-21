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
				ui.data.ticket.list = xhr.tickets;
				ui.render(ui.data.ticket);
				ui.data.log.list = xhr.logs;
				ui.render(ui.data.log);
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
					ui.data.log.list = xhr;
					ui.render(ui.data.log);
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
	static data = {
		log: {
			list: [],
			filter: null,
			sort: null,
			selector: 'log',
			columns: [
				{
					label: 'id',
					sort: true,
					excludeNarrow: true
				},
				{
					label: 'createdAt'
				},
				{
					label: 'status',
					filter: true
				},
				{
					label: 'ip',
					filter: true
				},
				{
					label: 'time',
					sort: true
				},
				{
					label: 'uri',
					filter: true
				},
				{
					label: 'referer',
					excludeNarrow: true
				}
			],
			convert() {
				var d = [];
				for (var i = 0; i < ui.data.log.list.length; i++) {
					var row = [];
					row.push(ui.data.log.list[i].id);
					row.push(ui.formatTime(ui.data.log.list[i].createdAt));
					row.push(ui.data.log.list[i].logStatus);
					row.push(ui.data.log.list[i].ip ? '<a href="https://whatismyipaddress.com/ip/' + ui.data.log.list[i].ip + '" target="sc_ip">' + ui.data.log.list[i].ip + '</a>' : '');
					row.push(ui.data.log.list[i].time);
					row.push(ui.data.log.list[i].method + ' ' + ui.data.log.list[i].uri + (ui.data.log.list[i].query ? '?' + ui.data.log.list[i].query : '') + ui.sanitizeText(ui.data.log.list[i].body ? '<br/>' + ui.data.log.list[i].body : ''));
					row.push(ui.data.log.list[i].referer);
					d.push(row);
				}
				return d;
			},
			widths(narrowView) {
				return narrowView ? [0, 20, 10, 15, 10, 45] : [5, 10, 5, 10, 10, 25, 35];
			}
		},
		ticket: {
			list: [],
			filter: null,
			sort: null,
			selector: 'ticket',
			columns: [
				{
					label: 'id',
					sort: true,
					excludeNarrow: true
				},
				{
					label: 'createdAt'
				},
				{
					label: 'note',
					sort: true
				}
			],
			convert() {
				var d = [];
				for (var i = 0; i < ui.data.ticket.list.length; i++) {
					var row = [];
					row.push(ui.data.ticket.list[i].id);
					row.push(ui.formatTime(ui.data.ticket.list[i].createdAt));
					row.push(ui.sanitizeText(ui.data.ticket.list[i].note));
					d.push(row);
				}
				return d;
			},
			widths(narrowView) {
				return narrowView ? [0, 20, 80] : [5, 10, 85];
			}
		},
		multiline: true
	};

	static open(event) {
		if (event.target.getAttribute('i') == document.querySelector('popup content').getAttribute('i')) {
			ui.popupClose();
			return;
		}
		var id = event.target.getAttribute('i').split('-');
		var data;
		for (var i = 0; i < ui.data[id[0]].list.length; i++) {
			if (ui.data[id[0]].list[i].id == id[1]) {
				data = ui.data[id[0]].list[i];
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
		if ((field || field == 0) && (!value || value != ui.data.log.filter)) {
			if (!e)
				return;
			ui.data.log.filter = value;
		} else
			ui.data.log.filter = null;
		ui.render(ui.data.log);
	}

	static columnIndex(column, e) {
		var trs = e.querySelector('tr').querySelectorAll('th');
		column = column.trim();
		for (var i = 0; i < trs.length; i++) {
			if (trs[i].innerText == column)
				return i;
		}
	}

	static openFilter(event) {
		ui.data.log.filter = null;
		var field = ui.columnIndex(event.target.innerText, document.querySelector(ui.data.log.selector));
		var s = '';
		var processed = [], value;
		var logs = ui.data.log.convert();
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
	
	static render(data) {
		var d = data.convert();
		var narrowView = ui.isNarrowView();
		var s = '<thead><tr>';
		for (var i = 0; i < data.columns.length; i++) {
			if (!narrowView || !data.columns[i].excludeNarrow)
				s += '<th' + 
						(data.columns[i].sort ? ' onclick="ui.sortColumn(event)" class="clickable"' : '') +
						(data.columns[i].filter ? ' onclick="ui.openFilter(event)" class="clickable"' : '') +
						' [[w' + (i + 1) + ']]>' + data.columns[i].label + '</th>';
		}
		s += '</tr></thead>';
		if (data.sort) {
			var column = parseInt(data.sort.substring(0, data.sort.indexOf('-'))) + (narrowView ? 1 : 0);
			var factor = data.sort.indexOf('-asc') > 0 ? 1 : -1;
			d = d.sort((a, b) => (typeof a[column] == 'string' ? a[column].localeCompare(b[column]) : a[column] - b[column]) * factor);
		}
		for (var i = 0; i < d.length; i++) {
			if (!data.filter || d[i][parseInt(data.filter.substring(0, data.filter.indexOf('-'))) + (narrowView ? 1 : 0)] == data.filter.substring(data.filter.indexOf('-') + 1)
			   	|| d[i][parseInt(data.filter.substring(0, data.filter.indexOf('-'))) + (narrowView ? 1 : 0)].indexOf(data.filter.substring(data.filter.indexOf('-') + 1) + '<br/>') == 0) {
				s += '<tr>';
				for (var i2 = 0; i2 < data.columns.length; i2++) {
					if (!narrowView || !data.columns[i2].excludeNarrow)
						s += '<td' + (data.columns[i2].label == 'createdAt' ? ' onclick="ui.open(event)" i="' + data.selector + '-' + d[i][0] + '" class="clickable"' : '') + ' [[w' + (i2 + 1) + ']]>' + d[i][i2] + '</td>';
				}
				s += '</tr>';
			}
		}
		document.querySelector(data.selector).innerHTML = '<table>' + ui.replaceWidths(data.widths(narrowView), s) + '</table>';
		if (data.selector == 'log')
			document.querySelector('msg').innerHTML = (document.querySelectorAll(data.selector + ' tr').length - 1) + ' log entries';
		document.querySelector(data.selector + ' tr').querySelectorAll('th').forEach(e => e.classList.remove('asc', 'desc'));
		if (data.sort)
			document.querySelector(data.selector + ' tr').querySelectorAll('th')[parseInt(data.sort.substring(0, data.sort.indexOf('-')))].classList.add(data.sort.indexOf('-asc') > 0 ? 'asc' : 'desc');
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
		var e = event.target;
		var field = e.innerText;
		while (e.nodeName != 'TABLE')
			e = e.parentElement;
		e = e.parentElement;
		field = ui.columnIndex(field, e);
		var data = e.nodeName == 'LOG' ? ui.data.log : ui.data.ticket;
		if (!data.sort)
			data.sort = field + '-asc';
		else if (data.sort == field + '-asc')
			data.sort = field + '-desc';
		else
			data.sort = null;
		ui.render(data);
	}

	static toggleMultiline() {
		ui.data.multiline = !ui.data.multiline;
		ui.render(ui.data.log);
		ui.render(ui.data.ticket);
	}
}

window.api = api;
window.ui = ui;
