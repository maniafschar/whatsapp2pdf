import { Object } from "core-js";

export { SortableTable };

class SortableTable extends HTMLElement {
	list = [];
	filter = null;
	sort = null;
	columns = [];
	convert = null;
	openDetail = null;
	deleteButton = false;
	id = new Date().getTime() + Math.random();

	constructor() {
		super();
		this._root = this.attachShadow({ mode: 'open' });
	}

	connectedCallback() {
		this._root.appendChild(document.createElement('style')).textContent = `
:host(*) {
	display: block;
	position: relative;
}

*::-webkit-scrollbar {
	display: none;
}

table {
	border-collapse: collapse;
	width: 100%;
	overflow: hidden;
	position: relative;
	display: block;
	font-size: 1em;
	z-index: 3;
	height: 100%;
}

thead,
tbody {
	display: block;
	overflow-x: hidden;
	position: relative;
}
	
tbody {
	overflow-y: auto;
	height: calc(100% - 2em);
}

tr {
	width: 100%;
	position: relative;
	display: block;
	white-space: nowrap;
}

tbody tr {
	cursor: pointer;
}

tr.selected {
	background-color: rgba(255, 100, 50, 0.1);
}
	
td,
th {
	vertical-align: top;
	text-align: left;
	padding: 0.5em 0.75em;
	position: relative;
	white-space: nowrap;
	font-size: 1em;
	overflow: auto;
	max-height: 3.8em;
	-webkit-text-size-adjust: 100%;
	box-sizing: border-box;
	display: inline-block;
}

th {
	font-weight: bold;
	background: rgba(170, 170, 255, 0.15);
}

tbody tr:last-child {
	margin-bottom: 0.5em;
}

th.asc::before {
	content: '↓';
	margin-right: 0.2em;
	color: grey;
}

th.desc::before {
	content: '↑';
	margin-right: 0.2em;
	color: grey;
}

.clickable {
	cursor: pointer;
}

.unclickable {
	cursor: default;
}

a {
	text-decoration: none;
	color: darkblue;
}`;
		this._root.appendChild(document.createElement('table'));
		document.addEventListener('tableFilter', event => {
			if (this.id == event.detail.id)
				this.filterTable(event.detail);
		});
	}

	setConvert(convert) {
		this.convert = convert;
	}

	setOpenDetail(openDetail) {
		this.openDetail = openDetail;
	}

	renderTable() {
		var data = this.convert ? this.convert(this.list) : [...this.list];
		for (var i = 0; i < data.length; i++)
			data[i].i = i;
		var table = this._root.querySelector('table');
		table.textContent = '';
		var thead = table.appendChild(document.createElement('thead'));
		var tr = thead.appendChild(document.createElement('tr'));
		var widths = [], narrowView = window.outerWidth < 700, additionalSpace = 0.0;
		for (var i = 0; i < this.columns.length; i++) {
			if (narrowView && this.columns[i].excludeNarrow) {
				additionalSpace += this.columns[i].width;
				widths.push(0);
			} else
				widths.push(this.columns[i].width);
		}
		if (additionalSpace) {
			additionalSpace = 100.0 / (100.0 - additionalSpace);
			for (var i = 0; i < widths.length; i++)
				widths[i] = additionalSpace * widths[i];
		}
		for (var i = 0; i < this.columns.length; i++) {
			var th = tr.appendChild(document.createElement('th'));
			th.innerText = this.columns[i].label;
			if (this.columns[i].style)
				th.setAttribute('style', this.columns[i].style);
			if (widths[i] == 0)
				th.style.display = 'none';
			else
				th.style.width = widths[i] + '%';
			if (this.columns[i].sort) {
				th.setAttribute('onclick', 'this.getRootNode().host.sortColumn(event)');
				th.setAttribute('class', 'clickable');
			} else if (this.columns[i].filter) {
				th.setAttribute('onclick', 'this.getRootNode().host.openFilter(event)');
				th.setAttribute('class', 'clickable');
			}
		}
		if (this.sort) {
			var column = parseInt(this.sort.substring(0, this.sort.indexOf('-')));
			var factor = this.sort.indexOf('-asc') > 0 ? 1 : -1;
			data = data.sort((a, b) => {
				var compare;
				if (typeof a[column] == 'string')
					compare = a[column].localeCompare(b[column]);
				else if (a[column].attributes?.value)
					compare = parseFloat(a[column].attributes.value) - (b[column].attributes?.value ? parseFloat(b[column].attributes.value) : -1.7976931348623156e+308);
				else if (b[column].attributes?.value)
					compare = -1.7976931348623156e+308 - parseFloat(b[column].attributes.value);
				else if (a[column].text)
					compare = a[column].text.localeCompare(b[column].text);
				else
					compare = a[column] - b[column];
				return compare * factor;
			});
			while (!data[0][column]?.text)
				data.push(data.splice(0, 1)[0]);
		}
		var isFiltered = function (filter, row) {
			if (filter) {
				var field = row[parseInt(filter.split('-')[0])];
				var filterValue = decodeURIComponent(filter.substring(filter.indexOf('-') + 1));
				return field != filterValue && field.indexOf(filterValue + '<br/>') != 0
					&& (field.indexOf('<a ') < 0 || field.indexOf('>' + filterValue + '</a>') < 0);
			}
			return false;
		};
		var tbody = table.appendChild(document.createElement('tbody'));
		for (var i = 0; i < data.length; i++) {
			if (!isFiltered(this.filter, data[i])) {
				tr = tbody.appendChild(document.createElement('tr'));
				tr.setAttribute('i', data[i].i);
				for (var i2 = 0; i2 < this.columns.length; i2++) {
					var td = tr.appendChild(document.createElement('td'));
					if (this.columns[i2].style)
						td.setAttribute('style', this.columns[i2].style);
					var s = typeof data[i][i2] == 'object' ? data[i][i2].text : data[i][i2];
					if (s)
						td.innerHTML = s;
					else
						td.innerHTML = '&nbsp;';
					if (data[i][i2].attributes) {
						var keys = Object.keys(data[i][i2].attributes);
						for (var i3 = 0; i3 < keys.length; i3++)
							td.setAttribute(keys[i3], data[i][i2].attributes[keys[i3]]);
					}
					if (widths[i2] == 0)
						td.style.display = 'none';
					else
						td.style.width = widths[i2] + '%';
					td.setAttribute('onclick', 'this.getRootNode().host.openDetails(event)');
					if (this.columns[i2].noaction)
						td.setAttribute('class', 'unclickable');
				}
			}
		}
		this._root.querySelectorAll('tr th').forEach(e => e.classList.remove('asc', 'desc'));
		if (this.sort)
			this._root.querySelectorAll('tr th')[parseInt(this.sort.split('-')[0])].classList.add(this.sort.indexOf('-asc') > 0 ? 'asc' : 'desc');
		this.dispatchEvent(new CustomEvent('changed', { detail: { numberOfRows: this._root.querySelectorAll('tbody tr').length } }));
	}

	table() {
		return this._root.querySelector('table');
	}

	deleteRow(index) {
		var e = this._root.querySelector('tr[i="' + index + '"]');
		if (e) {
			e.textContent = '';
			this.dispatchEvent(new CustomEvent('changed'));
		}
	}

	filterTable(data) {
		if (data.token == this.filter)
			this.filter = null;
		else
			this.filter = data.token;
		this.renderTable();
	}

	columnIndex(column, e) {
		var trs = e.querySelectorAll('tr th');
		column = column.trim();
		for (var i = 0; i < trs.length; i++) {
			if (trs[i].innerText == column)
				return i;
		}
	}

	openDetails(event) {
		this._root.querySelectorAll('tr.selected')?.forEach(tr => tr.classList.remove('selected'));
		var tr = event.target;
		while (tr && tr.nodeName != 'TR')
			tr = tr.parentElement;
		tr?.classList.add('selected');
		if (event.target.getAttribute('onopen')) {
			var s = event.target.getAttribute('onopen').split('.');
			window[s[0]][s[1]](event);
			return;
		}
		if (this.openDetail) {
			this.openDetail(event);
			return;
		}
		var i = [...event.target.parentElement.children].indexOf(event.target);
		if (this.columns[i].onopen) {
			var s = this.columns[i].onopen.split('.');
			window[s[0]][s[1]](event);
			return;
		}
		var row = this.list[tr.getAttribute('i')];
		var keys = Object.keys(row);
		var s = '';
		var sanitizeText = s => s && s.replace ? s.replace(/\n/g, '<br/>').replace(/\t/g, '&nbsp;&nbsp;&nbsp;&nbsp;') : s ? s : '';
		for (var i = 0; i < keys.length; i++) {
			if (row[keys[i]])
				s += '<label>' + keys[i] + '</label><value>' + sanitizeText(row[keys[i]]) + '</value>';
		}
		if (this.deleteButton)
			s += '<buttons><button onclick="document.dispatchEvent(new CustomEvent(&quot;deleteEntry&quot;, { detail: { index: ' + tr.getAttribute('i') + ' } }))">delete</button></buttons>';
		document.dispatchEvent(new CustomEvent('popup', { detail: { body: s } }));
	}

	openFilter(event) {
		this.filter = null;
		var field = this.columnIndex(event.target.innerText, this._root);
		var s = '';
		var processed = [], value;
		var logs = this.convert ? this.convert(this.list) : this.list;
		for (var i = 0; i < logs.length; i++) {
			value = logs[i][field];
			if (value) {
				if (value.indexOf('<br/>') > -1)
					value = value.substring(0, value.indexOf('<br/>'));
				if (value.indexOf('<a ') == 0)
					value = value.replaceAll(/<[^>]*>/g, '');
				processed[value] = processed[value] ? processed[value] + 1 : 1;
			}
		}
		var sorted = Object.keys(processed).sort((a, b) => processed[b] - processed[a] == 0 ? (a > b ? 1 : -1) : processed[b] - processed[a]);
		for (var i = 0; i < sorted.length; i++)
			s += '<filter onclick="document.dispatchEvent(new CustomEvent(&quot;tableFilter&quot;, { detail: { token: &quot;' + field + '-' + encodeURIComponent(sorted[i]) + '&quot;, id: ' + this.id + ' } }))"><entry>' + sorted[i] + '</entry><count>' + processed[sorted[i]] + '</count></filter>';
		document.dispatchEvent(new CustomEvent('popup', { detail: { body: s, align: 'right' } }));
	}

	sortColumn(event) {
		var e = event.target;
		var field = e.innerText;
		while (e.nodeName != 'TABLE')
			e = e.parentElement;
		field = this.columnIndex(field, e);
		if (!this.sort)
			this.sort = field + '-asc';
		else if (this.sort == field + '-asc')
			this.sort = field + '-desc';
		else
			this.sort = null;
		this.renderTable();
	}
}