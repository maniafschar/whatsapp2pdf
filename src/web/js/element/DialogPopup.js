
export { DialogPopup };

class DialogPopup extends HTMLElement {
	constructor() {
		super();
		this._root = this.attachShadow({ mode: 'open' });
	}
	connectedCallback() {
		this._root.appendChild(document.createElement('style')).textContent = `
:host(*) {
	position: relative;
}

*::-webkit-scrollbar {
	display: none;
}

popup {
	transform: scale(0);
	position: fixed;
	width: fit-content;
	max-width: 90%;
	background-color: blanchedalmond;
	top: 6%;
	left: 0;
	right: 0;
	margin: 0 auto;
	z-index: 9;
	border-radius: 1em 0 1em 1em;
	filter: drop-shadow(0 0 0.5em rgba(0, 0, 0, 0.3));
	transition: all ease-out .4s;
	min-width: 5em;
}

popup::before {
	content: 'x';
	position: absolute;
	top: -1.5em;
	right: 0;
	padding: 0.25em 1em;
	background-color: blanchedalmond;
	border-radius: 0.5em 0.5em 0 0;
	color: rgba(0, 0, 0, 0.1);
	font-weight: bold;
	font-size: 1.2em;
}

close {
	position: absolute;
	top: -2.5em;
	right: 0;
	z-index: 2;
	width: 5.5em;
	height: 3.5em;
	cursor: pointer;
	display: block;
}

content {
	position: relative;
	display: block;
	margin: 1em;
	max-height: 82vh;
	max-width: 50em;
	overflow: auto;
	text-align: left;
}

error {
	padding-bottom: 1em;
	position: relative;
	display: block;
	text-align: center;
	color: red;
	font-style: italic;
    font-weight: bold;
	font-size: 0.8em;
}

label {
	position: relative;
	color: darkmagenta;
	font-size: 0.8em;
	background: rgba(255, 255, 255, 0.4);
	padding: 0.5em;
	border-radius: 0.5em 0.5em 0 0;
	clear: left;
	float: left;
}

value {
	position: relative;
	min-width: 7em;
	max-height: 20em;
	max-width: 100%;
	margin-bottom: 1em;
	line-height: 1.5;
	overflow: auto;
	padding: 0.5em;
	border-radius: 0 0.5em 0.5em 0.5em;
	background: rgba(255, 255, 255, 0.4);
	float: left;
	clear: left;
	user-select: text;
	box-sizing: border-box;
}

field {
	position: relative;
	display: block;
	min-height: 2em;
	padding: 0.5em;
	border-radius: 0 0.5em 0.5em 0.5em;
	background: rgba(255, 255, 255, 0.4);
	margin-bottom: 1em;
	clear: left;
}

textarea,
input {
	appearance: none;
	position: relative;
	font-size: 1em;
	font-weight: normal;
	outline: none !important;
	font-family: Comfortaa, Verdana, "Helvetica Neue", Helvetica, Arial, sans-serif !important;
	height: 2em;
	padding: 0em 0.75em;
	border-radius: 0.5em;
	background: rgba(255, 255, 255, 0.85);
	vertical-align: top;
	border: none;
	width: 100%;
	color: black;
	user-select: text;
}

input[type="file"] {
	opacity: 0;
	cursor: pointer;
	position: absolute;
	top: 0;
	left: 0;
	bottom: 0;
	right: 0;
	display: block;
	height: 100%;
}

textarea {
	height: 5em;
	padding-top: 0.5em;
	overflow-y: auto;
	resize: none;
}

a {
	text-decoration: none;
	color: darkblue;
	cursor: pointer;
}

button {
	background: rgba(255, 255, 255, 0.4);
	border: solid 1px rgba(0, 0, 0, 0.05);
	padding: 0.5em 1em;
	border-radius: 1em;
	outline: none;
	cursor: pointer;
	font: inherit;
	font-size: 0.8em;
}

button.icon {
	font-size: 1.3em;
	width: 2em;
	height: 2em;
	position: absolute;
	background: rgba(255, 0, 0, 0.2);
	margin: 0;
	padding: 0;
}

button.icon svg {
	width: 50%;
	height: 50%;
}

buttons {
	position: relative;
	display: block;
	float: left;
	clear: left;
	text-align: center;
	margin-bottom: 1em;
	width: 100%;
}

pre {
	padding-bottom: 1em;
	box-sizing: border-box;
	display: block;
	position: relative;
	margin: 0;
	overflow: auto;
}`;
		var popup = this._root.appendChild(document.createElement('popup'));
		popup.appendChild(document.createElement('close')).onclick = () => this.close(this._root.querySelector('popup'));
		popup.appendChild(document.createElement('content'));
		document.addEventListener('popup', event => event.detail?.body ? this.open(event, this._root.querySelector('popup')) : this.close(this._root.querySelector('popup')));
	}

	open(event, popup) {
		var i = this.hash(typeof event.detail.body == 'string' ? event.detail.body : event.detail.body.innerHTML);
		if (popup.getAttribute('i') == i) {
			this.close(popup);
			return;
		}
		if (typeof event.detail.body == 'string')
			popup.querySelector('content').innerHTML = event.detail.body;
		else {
			popup.querySelector('content').textContent = '';
			popup.querySelector('content').appendChild(event.detail.body);
		}
		popup.setAttribute('i', i);
		if (!popup.style.transform)
			popup.style.transform = 'scale(1)';
		var right = event.detail.align == 'right';
		popup.style.left = right ? 'initial' : '';
		popup.style.right = right ? '1em' : '';
	}

	close(popup) {
		popup.addEventListener('transitionend', () => popup.querySelector('content').textContent = '', { capture: false, passive: true, once: true });
		popup.style.transform = '';
		popup.removeAttribute('i');
	}

	content() {
		return this._root.querySelector('content');
	}

	hash(s) {
		let hash = 0;
		for (const char of s) {
			hash = (hash << 5) - hash + char.charCodeAt(0);
			hash |= 0; // Constrain to 32bit integer
		}
		return hash;
	}
}