import { api } from "./api";
import { DialogPopup } from "./element/DialogPopup";
import { InputCheckbox } from "./element/InputCheckbox";
import { InputRating } from "./element/InputRating";
import { ProgressBar } from "./element/ProgressBar";

export { action };

class action {
	static feedbackStatus = 'You need to <a onclick="ui.showTab(1)">create</a> at least one document, to give feedback.';

	static init() {
		window.onresize();
		api.feedback(xhr => {
			var s = '';
			var formatTime = function (s) {
				var d = new Date(s.replace('+00:00', ''));
				d = new Date(Date.UTC(d.getFullYear(), d.getMonth(), d.getDate(), d.getHours(), d.getMinutes(), d.getSeconds()))
				return d.toLocaleString();
			};
			for (var i = 0; i < xhr.length; i++)
				s += '<rating><input-rating rating="' + xhr[i].rating + '"></input-rating><top>' + formatTime(xhr[i].modifiedAt) + '<br/>' + xhr[i].name + '</top><note>' + xhr[i].note.replace(/\n/g, '<br/>') + '</note></rating>';
			document.querySelector('feedbacks').innerHTML = s;
		});
		setTimeout(function () { document.querySelector('body>container').style.opacity = 1; }, 400);
		if (window.location.search) {
			var params = new URL(location.href).searchParams;
			if (params.get('id') && params.get('pin'))
				api.feedbackConfirm(
					{
						id: params.get('id'),
						pin: params.get('pin')
					},
					xhr => {
						if (xhr) {
							var s = action.feedbackStatus;
							action.feedbackStatus = xhr;
							action.feedback();
							api.feedback();
							action.feedbackStatus = s;
						}
					},
					xhr => {
						document.dispatchEvent(new CustomEvent('popup', {
							detail: {
								body: xhr.status < 500 ? 'The server is unavailable. Please try again later.' : 'Saving feedback failed: ' + xhr.responseText
							}
						}));
					}
				);
		}
	}

	static feedback() {
		var data = action.feedbackStatus ? action.feedbackStatus : `
<data>
	<br />
	<input type="hidden" name="id"></input>
	<input type="hidden" name="pin"></input>
	<input-rating ui="dialog"></input-rating>
	<label>Feedback<textarea name="note"></textarea></label>
	<label>Name<input name="name"></input></label>
	<label>Email<input name="email"></input></label>
	<label style="display: none;">Image
		<imageupload>
			<span>Click to upload...</span>
			<input type="file" id="chatFile" accept=".zip" style="max-width: 30em;" />
		</imageupload>
	</label>
	<error></error>
	<button onclick="action.feedbackSave()">Save</button>
</data>`;
		document.dispatchEvent(new CustomEvent('popup', {
			detail: {
				body: data
			}
		}));
	}

	static analyse(event) {
		document.getElementsByTagName('attributes')[0].style.display = 'none';
		document.getElementsByTagName('upload')[0].style.display = '';
		if (event.target.files[0]) {
			var formData = new FormData();
			formData.append('file', event.target.files[0]);
			api.analyse(formData,
				action.postAnalyse,
				xhr =>
					document.dispatchEvent(new CustomEvent('popup', {
						detail: {
							body:
								xhr.status == 403 ? 'The file was blocked by your proxy. Please try a correct WhatsApp exported chat file.' :
									xhr.status < 500 ? 'The server is unavailable. Please try again later.' : 'PDF creation failed. Is it a WhatsApp exported chat file?'
						}
					}))
			)
		} else
			document.dispatchEvent(new CustomEvent('popup', { detail: { body: 'Please select a file to convert.' } }));
	}

	static buy() {
		document.querySelector('period').classList.remove('error');
		var period = '';
		var periods = document.querySelectorAll('period tr.selected');
		if (periods.length < 1) {
			document.querySelector('period').classList.add('error');
			return;
		}
		for (var i = 0; i < periods.length; i++)
			period += 'periods=' + encodeURIComponent(periods[i].getAttribute('value')) + '&';
		document.dispatchEvent(new CustomEvent('progressbar', { detail: { type: 'open' } }));
		api.buy(
			document.querySelector('id').innerText,
			period,
			document.querySelector('user .selected').getAttribute('value'),
			document.querySelector('summary').classList.contains('selected'),
			action.postBuy,
			xhr => document.dispatchEvent(new CustomEvent('popup', { detail: { body: xhr.status < 500 ? 'The server is unavailable. Please try again later.' : 'PDF creation failed. Please try again later.' } }))
		);
	}

	static delete(i) {
		api.delete(document.querySelector('id').innerText, () => {
			document.getElementsByTagName('attributes')[0].style.display = null;
			document.getElementsByTagName('upload')[0].style.display = null;
			action.feedbackStatus = 'You already deleted the uploaded data. Please give feedback before deleting the data.';
			document.querySelectorAll('input[type="file"]').forEach(e => e.value = '');
		});
	}

	static feedbackSave() {
		var popup = document.querySelector('dialog-popup').content();
		var body = {
			rating: popup.querySelector('input-rating').getAttribute('value'),
			id: popup.querySelector('input[name="id"]').value,
			pin: popup.querySelector('input[name="pin"]').value,
			note: popup.querySelector('textarea[name="note"]').value,
			name: popup.querySelector('input[name="name"]').value,
			email: popup.querySelector('input[name="email"]').value
		};
		if (!body.email || !body.name || !body.note) {
			popup.querySelector('error').innerHTML = 'Please enter all fields.';
			return;
		}
		api.saveFeedback(
			id,
			body,
			xhr => {
				popup.querySelector('message').innerHTML = xhr;
				popup.querySelector('data').style.display = '';
				action.feedbackStatus = 'You already successfully added a feedback. You may edit it with the link in the email we sent you.';
			},
			xhr => popup.querySelector('error').innerText = xhr.status < 500 ? 'The server is unavailable. Please try again later.' : 'Saving feedback failed: ' + xhr.responseText
		);
	}

	static preview(event, period) {
		var mainTR = event.target.parentElement.parentElement.previousElementSibling;
		if (mainTR.classList.value == 'download') {
			mainTR.click();
			return;
		}
		if (mainTR.classList.value && !mainTR.classList.contains('selected'))
			return
		event.preventDefault();
		event.stopPropagation();
		document.dispatchEvent(new CustomEvent('progressbar', { detail: { type: 'open' } }));
		api.preview(
			document.querySelector('id').innerText,
			period,
			document.querySelector('user .selected').getAttribute('value'),
			action.download,
			xhr => document.dispatchEvent(new CustomEvent('popup', { detail: { body: xhr.status < 500 ? 'The server is unavailable. Please try again later.' : 'PDF creation failed. Please try again later.' } }))
		);
	}

	static postAnalyse(data) {
		if (!data) {
			document.dispatchEvent(new CustomEvent('popup', { detail: { body: 'PDF creation failed. Is it a WhatsApp exported chat file?' } }))
			return;
		}
		document.getElementsByTagName('attributes')[0].style.display = 'block';
		document.getElementsByTagName('upload')[0].style.display = 'none';
		document.querySelector('attributes button[onclick*="delete"]').style.display = '';
		document.querySelector('attributes id').innerText = data.id;
		var s = '<table><tr><th>Period</th><th>Chats</th><th>Words</th><th>Letters</th></tr>';
		for (var i = 0; i < data.periods.length; i++)
			s += '<tr value="' + data.periods[i].period + '"><td>' + data.periods[i].period.replace('-\\d', '').replace('\\d/', '').replace('\\d.', '') + '</td><td>' + data.periods[i].chats.toLocaleString() + '</td><td>' + data.periods[i].words.toLocaleString() + '</td><td>' + data.periods[i].letters.toLocaleString() + '</td></tr><tr><td colspan="4"><button onclick="action.preview(event, &quot;' + data.periods[i].period.replaceAll('\\', '\\\\') + '&quot;)">Preview</button></td></tr>';
		document.getElementsByTagName('attributes')[0].querySelector('period').innerHTML = s + '</table><summary>add AI generated summary<br/><span>(data will be sent to Google Gemini)</span></summary>';
		document.getElementsByTagName('attributes')[0].querySelectorAll('period tr').forEach(tr => {
			tr.addEventListener('click', () => {
				document.querySelector('period').classList.remove('error');
				if (tr.classList.contains('download')) {
					var link = document.createElement('a');
					link.setAttribute('href', api.url + '/rest/api/pdf/' + document.querySelector('id').innerText + '/true?period=' + encodeURIComponent(tr.getAttribute('value')));
					link.setAttribute('target', '_blank');
					link.click();
					tr.classList.remove('download');
					tr.nextElementSibling.querySelector('button').innerText = 'Preview';
					if (document.querySelectorAll('period .spinner,period .download').length == 0)
						document.querySelector('attributes button[onclick*="delete"]').style.display = '';
					action.feedbackStatus = '';
					if (!document.querySelector('period tr.download'))
						document.querySelector('period').classList.remove('downloadHint');
				} else if (tr.classList.contains('selected'))
					tr.classList.remove('selected');
				else if (!tr.classList.contains('spinner') && !tr.children[0].getAttribute('colspan'))
					tr.classList.add('selected');
			});
		});
		document.getElementsByTagName('summary')[0].onclick = function () {
			var e = document.getElementsByTagName('summary')[0];
			if (e.classList.contains('selected'))
				e.classList.remove('selected');
			else
				e.classList.add('selected');
		};
		s = '<table><tr><th>User</th><th>Chats</th><th>Words</th><th>Letters</th></tr>';
		for (var i = 0; i < data.users.length; i++)
			s += '<tr value="' + data.users[i].user + '"' + (s.indexOf('" class="selected">') < 0 ? ' class="selected"' : '') + '><td>' + data.users[i].user + '</td><td>' + data.users[i].chats.toLocaleString() + '</td><td>' + data.users[i].words.toLocaleString() + '</td><td>' + data.users[i].letters.toLocaleString() + '</td></tr>';
		document.getElementsByTagName('attributes')[0].querySelector('user').innerHTML = s + '</table>';
		document.getElementsByTagName('attributes')[0].querySelectorAll('user tr').forEach(tr => {
			tr.addEventListener('click', () => {
				document.querySelector('user .selected').classList.remove('selected');
				tr.classList.add('selected');
			});
		});
	}

	static postBuy() {
		action.feedbackStatus = 'Download one of your printed documents, then you can give feedback.';
		document.querySelector('attributes button[onclick*="delete"]').style.display = 'none';
		var periods = document.querySelectorAll('period tr.selected');
		for (var i = 0; i < periods.length; i++) {
			var tr = document.querySelector('period tr[value="' + periods[i].getAttribute('value').replaceAll('\\', '\\\\') + '"]');
			tr.classList.remove('selected');
			tr.classList.add('spinner');
			tr.nextElementSibling.querySelector('button').innerText = '⌛ preparing...';
			action.download(periods[i].getAttribute('value'));
		}
	}

	static download(period) {
		var download = function () {
			api.ajax({
				noProgressBar: true,
				url: api.url + '/rest/api/pdf/' + document.querySelector('id').innerText + '/false' + (period ? '?period=' + encodeURIComponent(period) : ''),
				method: 'GET',
				success: () => {
					document.dispatchEvent(new CustomEvent('progressbar'));
					if (period) {
						var tr = document.querySelector('period tr[value="' + period.replaceAll('\\', '\\\\') + '"]');
						tr.classList.remove('spinner');
						tr.classList.add('download');
						tr.nextElementSibling.querySelector('button').innerText = 'Download';
						document.querySelector('period').classList.add('downloadHint');
					} else {
						var link = document.createElement('a');
						link.setAttribute('href', api.url + '/rest/api/pdf/' + document.querySelector('id').innerText + '/true');
						link.setAttribute('target', '_blank');
						link.click();
					}
				},
				error: xhr => {
					var error = xhr.responseText || 'Unknown error';
					if (error.indexOf('"status":566,') > -1)
						setTimeout(function () { download(period); }, 1000);
					else {
						if (xhr.status < 500)
							error = 'The server is unavailable. Please try again later.';
						else {
							document.dispatchEvent(new CustomEvent('progressbar'));
							if (error.indexOf('Invalid ID') > -1) {
								document.getElementsByTagName('attributes')[0].style.display = null;
								document.getElementsByTagName('upload')[0].style.display = '';
								document.getElementById('chatFile').value = '';
								error = 'Uploaded chat already deleted. Please upload new chat.';
							} else {
								if (period) {
									var tr = document.querySelector('period tr[value="' + period.replaceAll('\\', '\\\\') + '"]').classList;
									tr.remove('spinner');
									tr.add('selected');
								}
								if (error.indexOf(' ') < 5)
									error = error.substring(error.indexOf(' ')).trim();
								error = 'Creation failed: ' + error;
							}
						}
						document.dispatchEvent(new CustomEvent('popup', { detail: { body: error } }));
					}
				}
			});
		}
		if (!period)
			download();
		else if (document.querySelector('period .spinner'))
			setTimeout(download, 1000);
	}
}

customElements.define('dialog-popup', DialogPopup);
customElements.define('input-checkbox', InputCheckbox);
customElements.define('input-rating', InputRating);
customElements.define('progress-bar', ProgressBar);

window.api = api;
window.action = action;

document.querySelectorAll('input[type="file"]').forEach(e => e.onchange = action.analyse);

window.onresize = function () {
	var mobile = parseFloat(getComputedStyle(document.body).fontSize) * 50 < window.innerWidth ? 0 : 5;
	var diagonal = Math.sqrt(Math.pow(window.innerWidth, 2) + Math.pow(window.innerHeight, 2));
	document.body.style.fontSize = (Math.min(10 + diagonal / 160, 26) + mobile) + 'px';
	var imageWidth = 1536, imageHeight = 1024;
	var imageStyle = document.querySelector('body element.intro>img').style;
	if (window.innerHeight / imageHeight * imageWidth > window.innerWidth) {
		imageStyle.height = window.innerHeight;
		imageStyle.width = null;
		imageStyle.marginTop = null;
	} else {
		imageStyle.width = window.innerWidth;
		imageStyle.height = null;
		imageStyle.marginTop = window.innerHeight - window.innerWidth / imageWidth * imageHeight;
	}
}