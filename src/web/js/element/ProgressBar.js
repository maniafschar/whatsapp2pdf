
export { ProgressBar };

class ProgressBar extends HTMLElement {
	constructor() {
		super();
		this._root = this.attachShadow({ mode: 'open' });
	}
	connectedCallback() {
		this._root.appendChild(document.createElement('style')).textContent = `
container {
	position: fixed;
	left: 0;
	top: 0;
	width: 100%;
	height: 100%;
	display: none;
	opacity: 0;
	background-color: rgba(0, 0, 0, 0.4);
	transition: all .4s ease-out;
	z-index: 10;
}

loader {
	position: absolute;
	top: calc(50% - 6em);
	left: calc(50% - 6em);
	width: 12em;
	height: 12em;
	border-radius: 50%;
	perspective: 800px;
}

loader div {
	position: absolute;
	box-sizing: border-box;
	width: 100%;
	height: 100%;
	border-radius: 50%;
}

loader .one {
	left: 0%;
	top: 0%;
	animation: rotate-one 1s linear infinite;
	border-bottom: 3px solid #b55;
}

loader .two {
	right: 0%;
	top: 0%;
	animation: rotate-two 1s linear infinite;
	border-right: 3px solid #8d8;
}

loader .three {
	right: 0%;
	bottom: 0%;
	animation: rotate-three 1s linear infinite;
	border-top: 3px solid #aaf;
}

@keyframes rotate-one {
	0% {
		transform: rotateX(35deg) rotateY(-45deg) rotateZ(0deg);
	}

	100% {
		transform: rotateX(35deg) rotateY(-45deg) rotateZ(360deg);
	}
}

@keyframes rotate-two {
	0% {
		transform: rotateX(50deg) rotateY(10deg) rotateZ(0deg);
	}

	100% {
		transform: rotateX(50deg) rotateY(10deg) rotateZ(360deg);
	}
}

@keyframes rotate-three {
	0% {
		transform: rotateX(35deg) rotateY(55deg) rotateZ(0deg);
	}

	100% {
		transform: rotateX(35deg) rotateY(55deg) rotateZ(360deg);
	}

}`;
		var loader = this._root.appendChild(document.createElement('container')).appendChild(document.createElement('loader'));
		loader.appendChild(document.createElement('div')).classList.add('one');
		loader.appendChild(document.createElement('div')).classList.add('two');
		loader.appendChild(document.createElement('div')).classList.add('three');
		document.addEventListener('progressbar', event => event.detail?.type == 'open' ? this.open(this._root.querySelector('container')) : this.close(this._root.querySelector('container')));
	}

	open(container) {
		container.style.display = 'block';
		setTimeout(() => container.style.opacity = 1, 10);
	}

	close(container) {
		container.style.opacity = null;
		setTimeout(() => container.style.display = null, 400);
	}
}