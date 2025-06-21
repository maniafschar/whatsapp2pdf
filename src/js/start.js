import { api } from "./api";

document.querySelectorAll('input[type="file"]').forEach(e => e.onchange = api.analyse);

window.onresize = function () {
	var diagonal = Math.sqrt(Math.pow(window.innerWidth, 2) + Math.pow(window.innerHeight, 2));
	document.body.style.fontSize = Math.min(10 + diagonal / 160, 26) + 'px';
}


export { ui };

class ui {
	static init() {
		window.onresize();
		api.feedback();
		if (window.location.search) {
			var params = new URL(location.href).searchParams;
			if (params.get('id') && params.get('pin')) {
				api.ajax({
					url: api.url + '/rest/api/feedback/confirm',
					method: 'PUT',
					body: {
						id: params.get('id'),
						pin: params.get('pin')
					},
					success: xhr => {
						document.querySelector('popup content message').innerHTML = xhr;
						ui.feedback();
						api.feedback();
					},
					error: xhr => {
						document.querySelector('popup content message').innerHTML = xhr.status < 500 ? 'The server is unavailable. Please try again later.' : 'Saving feedback failed: ' + xhr.responseText;
						ui.feedback();
					}
				});
			}
		}
	}

	static showDescription(i) {
		document.querySelector('description container').style.marginLeft = -(i * 100) + '%';
		document.querySelector('tab.selected')?.classList.remove('selected');
		document.querySelectorAll('tab')[i].classList.add('selected');
	}

	static feedback() {
		document.getElementsByTagName('popup')[0].style.transform = 'scale(1)';
		if (!document.querySelector('popup content message').innerText)
			document.querySelector('popup content data').style.display = 'block';
	}

	static feedbackClose() {
		document.getElementsByTagName('popup')[0].style.transform = '';
	}
}

class InputRating extends HTMLElement {
	rating = `<ratingSelection style="font-size:2em;margin-top:0.5em;">
	<empty><span>☆</span><span onclick="this.getRootNode().host.rate(event,2)">☆</span><span
			onclick="this.getRootNode().host.rate(event,3)">☆</span><span onclick="this.getRootNode().host.rate(event,4)">☆</span><span
			onclick="this.getRootNode().host.rate(event,5)">☆</span></empty>
	<full><span onclick="this.getRootNode().host.rate(event,1)">★</span><span onclick="this.getRootNode().host.rate(event,2)">★</span><span
			onclick="this.getRootNode().host.rate(event,3)">★</span><span onclick="this.getRootNode().host.rate(event,4)">★</span><span
			onclick="this.getRootNode().host.rate(event,5)" style="display:none;">★</span></full>
	</ratingSelection>`;
	constructor() {
		super();
		this._root = this.attachShadow({ mode: 'closed' });
	}
	connectedCallback() {
		const style = document.createElement('style');
		style.textContent = `
detailRating {
	font-size: 1.5em;
	margin: 1em 0 0.75em 0;
	display: block;
	position: relative;
	cursor: pointer;
}

ratingHint {
	margin: 0 1em 1em 1em;
	display: block;
}

ratingHistory {
	display: block;
}

ratingHistory rating {
	display: block;
	background: transparent;
	padding: 0.5em;
	text-align: left;
	width: 100%;
	position: relative;
}

ratingHistory rating img {
	max-width: 100%;
	border-radius: 1em;
}

ratingHistory span {
	position: relative;
}

rating,
ratingSelection {
	position: relative;
	line-height: 1;
	display: inline-block;
	height: 1.5em;
}

rating empty,
ratingSelection empty {
	opacity: 0.3;
	position: relative;
}

rating full,
ratingSelection full {
	position: absolute;
	left: 0;
	overflow: hidden;
	top: 0;
	color: var(--bg2stop);
}

ratingSelection span {
	width: 2em;
	display: inline-block;
	position: relative;
	cursor: pointer;
}`;
		this._root.appendChild(style);
		var element, id = this.getAttribute('id');
		var stars = '<empty>☆☆☆☆☆</empty><full style="width:{0}%;">★★★★★</full>';
		if (this.getAttribute('ui') == 'dialog') {
			this.removeAttribute('ui');
			element = document.createElement('div');
			element.innerHTML = this.getForm(id);
			this._root.appendChild(element);
		} else if (this.getAttribute('ui') == 'rating') {
			element = document.createElement('div');
			element.innerHTML = this.rating;
			this._root.appendChild(element.children[0]);
			element = document.createElement('input');
			element.setAttribute('type', 'hidden');
			element.setAttribute('name', 'rating');
			element.setAttribute('value', '80');
			this._root.appendChild(element);
		}
	}
	rate(event, x) {
		var e = event.target.getRootNode().querySelectorAll('ratingSelection > full span');
		ui.css(e, 'display', 'none');
		for (var i = 0; i < x; i++)
			ui.css(e[i], 'display', '');
		event.target.getRootNode().querySelector('[name="rating"]').value = x * 20;
		event.target.getRootNode().host.setAttribute('value', x * 20);
	}
	getForm(id) {
		return `${this.rating}<input type="hidden" name="rating" value="80" />`;
	}
	static open(id) {
		var lastRating = {};
		var render = function () {
			if (lastRating)
				ui.navigation.openPopup(ui.l('rating.title'), '<input-rating ui="dialog"' + (id ? ' id="' + id + '"' : '')
					+ (lastRating ? ' lastRating="' + encodeURIComponent(JSON.stringify(lastRating)) + '"' : '') + '></input-rating>');
		};
		render();
	}
	save(event) {
		api.saveFeedback();
	}
}

customElements.define('input-rating', InputRating);
window.api = api;
window.ui = ui;
