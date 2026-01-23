
export { InputImage };

class InputImage extends HTMLElement {
	success;
	constructor() {
		super();
		this._root = this.attachShadow({ mode: 'open' });
	}
	connectedCallback() {
		this._root.appendChild(document.createElement('style')).textContent = `
:host(*) {
	position: absolute;
	background-color: rgba(255, 0, 0, 0.4);
	border-radius: 1.3em;
	width: 2.6em;
	height: 2.6em;
	text-align: center;
	display: inline-block;
	line-height: 2.6;
	z-index: 3;
}
input {
	opacity: 0;
	cursor: pointer;
	position: absolute;
	left: 0;
	top: 0;
	width: 100%;
	height: 100%;
}`;
		var element = document.createElement('input');
		element.setAttribute('type', 'file');
		element.setAttribute('onchange', 'this.getRootNode().host.load(this)');
		element.setAttribute('accept', '.gif, .png, .jpg');
		this._root.appendChild(element);
		this._root.appendChild(document.createTextNode('+'));
	}
	setSuccess(success) {
		this.success = success;
	}
	dataURItoBlob(dataURI) {
		var arr = dataURI.split(','), mime = arr[0].match(/:(.*?);/)[1];
		arr[1] = atob(arr[1]);
		var ab = new ArrayBuffer(arr[1].length);
		var ia = new Uint8Array(ab);
		for (var i = 0; i < arr[1].length; i++)
			ia[i] = arr[1].charCodeAt(i);
		return new Blob([ab], { type: mime });
	}
	load(e) {
		var file = e.files && e.files.length > 0 ? e.files[0] : null;
		if (file && this.success) {
			var reader = new FileReader();
			var t = this;
			reader.onload = function (r) {
				var image = new Image();
				image.onload = function () {
					var scaled = t.scale(image);
					scaled.size = t.dataURItoBlob(scaled.data).size;
					t.success({
						original: {
							size: file.size,
							width: image.naturalWidth,
							height: image.naturalHeight
						},
						scaled: {
							size: scaled.size,
							width: scaled.width,
							height: scaled.height
						},
						name: file.name,
						type: 'jpg',
						data: scaled.data,
						sizeRatio: (100 - scaled.size / file.size * 100).toFixed(0)
					});
				};
				image.src = r.target.result;
			};
			reader.readAsDataURL(file);
		}
	}
	scale(image) {
		var canvas = document.createElement('canvas'), scale = 1;
		var ctx = canvas.getContext('2d'), max = parseInt(this.getAttribute('max'));
		if (image.naturalWidth > image.naturalHeight)
			scale = max / image.naturalWidth;
		else
			scale = max / image.naturalHeight;
		if (scale > 1)
			scale = 1;
		canvas.width = scale * image.naturalWidth;
		canvas.height = scale * image.naturalHeight;
		ctx.fillStyle = 'white';
		ctx.fillRect(0, 0, canvas.width, canvas.height);
		ctx.drawImage(image, 0, 0, image.naturalWidth, image.naturalHeight, 0, 0, canvas.width, canvas.height);
		return { data: canvas.toDataURL('image/jpeg', 0.9), width: parseInt(canvas.width + 0.5), height: parseInt(canvas.height + 0.5) };
	}
}