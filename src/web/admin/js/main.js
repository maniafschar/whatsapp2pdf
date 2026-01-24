import { DialogPopup } from "../../js/element/DialogPopup";
import { ProgressBar } from "../../js/element/ProgressBar";
import { SortableTable } from "../../js/element/SortableTable";

export { api };

class api {
	static url = '{placeholderServer}/rest/sc/';

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
				var log = document.querySelector('log').appendChild(document.createElement('sortable-table'));
				log.list = xhr.logs;
				log.columns.push({ label: 'id', sort: true, width: 5, excludeNarrow: true });
				log.columns.push({ label: 'createdAt', width: 10, detail: true });
				log.columns.push({ label: 'status', width: 5, filter: true });
				log.columns.push({ label: 'ip', width: 10, filter: true, onopen: 'ui.openIp' });
				log.columns.push({ label: 'time', width: 10, sort: true });
				log.columns.push({ label: 'uri', width: 25, filter: true });
				log.columns.push({ label: 'referer', width: 35, excludeNarrow: true });
				log.setConvert(list => {
					var d = [];
					for (var i = 0; i < list.length; i++) {
						var row = [];
						row.push(list[i].id);
						row.push(ui.formatTime(list[i].createdAt));
						row.push(list[i].logStatus);
						row.push(list[i].ip);
						row.push(list[i].time);
						row.push(list[i].method + ' ' + list[i].uri + (list[i].query ? '?' + list[i].query : '') + ui.trim(ui.sanitizeText(list[i].body ? '<br/>' + list[i].body : '')));
						row.push(list[i].referer);
						d.push(row);
					}
					return d;
				});
				log.renderTable();

				var ticket = document.querySelector('ticket').appendChild(document.createElement('sortable-table'));
				ticket.list = xhr.tickets;
				ticket.deleteButton = true;
				ticket.columns.push({ label: 'id', width: 10, sort: true, excludeNarrow: true });
				ticket.columns.push({ label: 'createdAt', width: 20, detail: true });
				ticket.columns.push({ label: 'note', width: 70, sort: true });
				ticket.setConvert(list => {
					var d = [];
					for (var i = 0; i < list.length; i++) {
						var row = [];
						row.push(list[i].id);
						row.push(ui.formatTime(list[i].createdAt));
						row.push(ui.trim(ui.sanitizeText(list[i].note)));
						d.push(row);
					}
					return d;
				});
				ticket.renderTable();
				document.addEventListener('deleteEntry', event => api.deleteTicket(event.detail.id));

				document.querySelector('input[name="searchLogs"]').value = xhr.search;
				document.querySelector('tabHeader').addEventListener('changed', () =>
					document.querySelector('msg').innerText = (document.querySelector('tabHeader tab').classList.contains('selected') ? log : ticket).table().querySelectorAll('tbody tr').length + ' entries');
				log.addEventListener('changed', () => document.querySelector('tabHeader').dispatchEvent(new CustomEvent('changed', { detail: { index: 0 } })));
				ticket.addEventListener('changed', () => document.querySelector('tabHeader').dispatchEvent(new CustomEvent('changed', { detail: { index: 1 } })));
				document.querySelector('tabHeader').dispatchEvent(new CustomEvent('changed', { detail: { index: 0 } }));
			}
		});
	}

	static build(type) {
		api.ajax({
			url: api.url + 'build/' + type,
			method: 'POST',
			success: xhr =>
				document.dispatchEvent(new CustomEvent('popup', { detail: { body: '<pre>' + ui.sanitizeText(xhr) + '</pre>' } }))
		});
	}

	static deleteTicket(id) {
		api.ajax({
			url: api.url + 'ticket/' + id,
			method: 'DELETE',
			success: () => {
				document.querySelector('ticket sortable-table').deleteRow(id);
				document.dispatchEvent(new CustomEvent('popup'));
			}
		});
	}

	static log(event) {
		if (event && event.keyCode == 13) {
			api.ajax({
				url: api.url + 'log?search=' + encodeURIComponent(document.querySelector('input[name="searchLogs"]').value),
				success: xhr => {
					var log = document.querySelector('log sortable-table');
					log.list = xhr;
					log.renderTable();
				}
			});
		}
	}

	static ajax(param) {
		var xhr = new XMLHttpRequest();
		xhr.onreadystatechange = function () {
			if (xhr.readyState == 4) {
				document.dispatchEvent(new CustomEvent('progressbar'));
				var errorHandler = function () {
					if (param.error) {
						xhr.param = param;
						param.error(xhr);
					} else
						document.dispatchEvent(new CustomEvent('popup', { detail: { body: '<pre>' + JSON.stringify(xhr) + '</pre>' } }));
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
		setTimeout(function () { if (xhr.readyState != 4) document.dispatchEvent(new CustomEvent('progressbar', { detail: { type: 'open' } })) }, 100);
		xhr.send(param.body);
	}
}

class ui {
	static multiline = false;

	static sanitizeText(s) {
		return s && s.replace ? s.replace(/\n/g, '<br/>').replace(/\t/g, '&nbsp;&nbsp;&nbsp;&nbsp;') : s ? s : '';
	}

	static trim(s) {
		return !ui.multiline && s.indexOf('<br/>') > -1 ? s.substring(0, s.indexOf('<br/>')) : s;
	}

	static openIp(event) {
		var ip = document.querySelector('sortable-table').list[event.target.parentElement.getAttribute('i')].ip;
		if (ip)
			window.open('https://whatismyipaddress.com/ip/' + ip, 'sc_ip');
	}

	static formatTime(s) {
		var d = new Date(s.replace('+00:00', ''));
		d = new Date(Date.UTC(d.getFullYear(), d.getMonth(), d.getDate(), d.getHours(), d.getMinutes(), d.getSeconds()))
		return d.getDate() + '.' + (d.getMonth() + 1) + ' ' + d.getHours() + ':' + d.getMinutes() + ':' + d.getSeconds();
	}

	static showTab(event) {
		var tabHeader = ui.parents(event.target, 'tabHeader');
		var i = [...tabHeader.children].indexOf(ui.parents(event.target, 'tab'));
		document.querySelector('tabBody container').style.marginLeft = -(i * 100) + '%';
		document.querySelector('tab.selected')?.classList.remove('selected');
		document.querySelectorAll('tab')[i].classList.add('selected');
		document.querySelector('tabHeader').dispatchEvent(new CustomEvent('changed', { detail: { index: i } }));
	}

	static toggleMultiline() {
		ui.multiline = !ui.multiline;
		document.querySelector('log sortable-table').renderTable();
		document.querySelector('ticket sortable-table').renderTable();
	}

	static parents(e, nodeName) {
		if (e) {
			nodeName = nodeName.toUpperCase();
			while (e && e.nodeName != nodeName)
				e = e.parentNode;
		}
		return e;
	}
}

customElements.define('dialog-popup', DialogPopup);
customElements.define('progress-bar', ProgressBar);
customElements.define('sortable-table', SortableTable);

window.api = api;
window.ui = ui;