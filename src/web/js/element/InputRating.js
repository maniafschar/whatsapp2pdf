export { InputRating };

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
		this._root = this.attachShadow({ mode: 'open' });
	}
	connectedCallback() {
		this._root.appendChild(document.createElement('style')).textContent = `
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
	color: rgb(210, 225, 20);
}

ratingSelection span {
	width: 2em;
	display: inline-block;
	position: relative;
	cursor: pointer;
}`;
		var element;
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