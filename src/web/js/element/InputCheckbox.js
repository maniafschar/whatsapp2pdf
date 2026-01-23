
export { InputCheckbox };

class InputCheckbox extends HTMLElement {
	constructor() {
		super();
		this._root = this.attachShadow({ mode: 'closed' });
	}
	connectedCallback() {
		this._root.appendChild(document.createElement('style')).textContent = `
:host([checked="true"]) label {
	opacity: 1;
}
:host([readonly="true"]) label {
	opacity: 0.4;
}
:host([checked="true"]) label:before {
	position: absolute;
	content: '\\2713';
	left: 0.5em;
}
label {
	opacity: 0.8;
	position: relative;
	display: block;
	padding-left: 2em;
	cursor: pointer;
	text-align: left;
}`;
		this.setAttribute('onclick', 'this.toggleCheckbox(event)' + (this.getAttribute('onclick') ? ';' + this.getAttribute('onclick') : ''));
		var element = document.createElement('label');
		element.innerText = this.getAttribute('label');
		this._root.appendChild(element);
		this.tabIndex = 0;
		this.addEventListener('keydown', function (event) {
			if (event.key == ' ')
				this.click();
		});
		this.attributeChangedCallback('label', null, this.getAttribute('label'));
	}
	static get observedAttributes() { return ['label']; }
	attributeChangedCallback(name, oldValue, newValue) {
		if (name == 'label' && newValue && this._root.querySelector('label')) {
			this._root.querySelector('label').innerText = newValue;
			this.removeAttribute('label');
		}
	}
	toggleCheckbox(event) {
		if (this.getAttribute("readonly") != 'true') {
			var e = event.target, alterState = true;
			if (e.getAttribute('type') == 'radio') {
				alterState = e.getAttribute('checked') != 'true';
				if (!alterState && e.getAttribute('deselect') != 'true')
					return;
				e.parentElement.querySelectorAll('input-checkbox[name="' + e.getAttribute('name') + '"][type="radio"]').setAttribute('checked', 'false');
			}
			if (alterState) {
				e.setAttribute('checked', e.getAttribute('checked') == 'true' ? 'false' : 'true');
				this.dispatchEvent(new CustomEvent('Checkbox', { detail: { value: e.getAttribute('checked') == 'true' ? e.getAttribute('value') : '' } }));
			}
		}
	}
}
