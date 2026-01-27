
export { InputSelection };

class InputSelection extends HTMLElement {
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

item {
	position: relative;
	display: block;
	padding: 0.5em 0.5em 0.5em 1.5em;
	cursor: pointer;
}

item.selected {
	font-weight: bold;
}

item.selected::before {
	content: '✓';
	position: absolute;
	left: 0.1em;
	top: 0.5em;
}`;
	}
	add(id, label) {
		var item = this._root.appendChild(document.createElement('item'));
		item.innerText = label;
		item.setAttribute('i', id);
		item.setAttribute('onclick', 'this.getRootNode().host.onclick(event)');
		if (this.getAttribute('value') == id || !this.getAttribute('value') && this._root.querySelectorAll('item').length == 1) {
			item.classList.add('selected');
			this.setAttribute('value', id);
		}
	}
	onclick(event) {
		var e = event.target;
		if (event.target.classList.contains('selected'))
			return;
		while (e.previousElementSibling)
			e = e.previousElementSibling;
		while (e.nextElementSibling) {
			e.classList.remove('selected');
			e = e.nextElementSibling;
		}
		e.classList.remove('selected');
		event.target.classList.add('selected');
		this.setAttribute('value', event.target.getAttribute('i'));
		this.dispatchEvent(new CustomEvent('changed'));
	}
	clear() {
		this._root.querySelectorAll('item').forEach(e => e.remove());
	}
}