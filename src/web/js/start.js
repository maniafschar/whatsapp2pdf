import { api } from "./api";

document.querySelectorAll('input[type="file"]').forEach(e => e.onchange = api.analyse);

window.onresize = function () {
	var mobile = document.body.computedStyleMap().get('font-size').value * 50 < window.innerWidth ? 0 : 5;
	var diagonal = Math.sqrt(Math.pow(window.innerWidth, 2) + Math.pow(window.innerHeight, 2));
	document.body.style.fontSize = (Math.min(10 + diagonal / 160, 26) + mobile) + 'px';
	document.querySelector('body container header').style.borderRadius = mobile ? '0' : '';
	document.querySelector('body container tabBody').style.borderRadius = mobile ? '0' : '';
}


export { ui };

class ui {
	static init() {
		window.onresize();
		api.feedback();
		setTimeout(function() { document.body.style.opacity = 1; }, 100);
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
						if (xhr) {
							var s = api.feedbackStatus;
							api.feedbackStatus = xhr;
							ui.feedback();
							api.feedback();
							ui.showTab(2);
							api.feedbackStatus = s;
						}
					},
					error: xhr => {
						document.querySelector('popup content message').innerHTML = xhr.status < 500 ? 'The server is unavailable. Please try again later.' : 'Saving feedback failed: ' + xhr.responseText;
						ui.feedback();
					}
				});
			}
		}
	}

	static showTab(i) {
		document.querySelector('tabBody container').style.marginLeft = -(i * 100) + '%';
		document.querySelector('tab.selected')?.classList.remove('selected');
		document.querySelectorAll('tab')[i].classList.add('selected');
	}

	static feedback() {
		document.querySelector('popup content message').innerHTML = api.feedbackStatus;
		document.querySelector('popup content data').style.display = api.feedbackStatus ? '' : 'block';
		var s = document.getElementsByTagName('popup')[0].style;
		s.transform = s.transform && s.transform.indexOf('1') > 0 ? 'scale(0)' : 'scale(1)';
	}

	static feedbackClose() {
		document.getElementsByTagName('popup')[0].style.transform = '';
	}
}

class InputRating extends HTMLElement {
	rating = `<ratingSelection style="font-size:2em;margin:0.5em 0;">
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
	position: relative;
	color: darkgoldenrod;
}

ratingHint {
	margin: 0 1em 1em 1em;
	display: block;
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
			element = document.createElement('div');
			element.innerHTML = this.rating;
			this._root.appendChild(element.children[0]);
			element = document.createElement('input');
			element.setAttribute('type', 'hidden');
			element.setAttribute('name', 'rating');
			element.setAttribute('value', '80');
		} else {
			var stars = '<empty>☆☆☆☆☆</empty><full style="width:{0}%;">★★★★★</full>';
			element = document.createElement('detailRating');
			element.innerHTML = '<ratingSelection>' + stars.replace('{0}', this.getAttribute('rating')) + '</ratingSelection>';
		}
		this.removeAttribute('ui');
		this._root.appendChild(element);
		this._root.host.setAttribute('value', 80);
	}
	rate(event, x) {
		var e = event.target.getRootNode().querySelectorAll('ratingSelection > full span');
		for (var i = 0; i < 5; i++)
			e[i].style.display = i < x ? '' : 'none';
		event.target.getRootNode().querySelector('[name="rating"]').value = x * 20;
		event.target.getRootNode().host.setAttribute('value', x * 20);
	}
}

customElements.define('input-rating', InputRating);
window.api = api;
window.ui = ui;
