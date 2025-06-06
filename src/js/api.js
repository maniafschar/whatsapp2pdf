export { api };

class api {
	static url = '{placeholderServer}';

	static convert() {
		var file = document.getElementById('chatFile');
		if (file.files[0]) {
			document.getElementsByTagName('error')[0].innerHTML = '';
			document.getElementsByTagName('progressbar')[0].style.display = 'block';
			var formData = new FormData();
			formData.append('file', file.files[0]);
			api.ajax({
				url: api.url + '/rest/api/conversion',
				method: 'POST',
				body: formData,
				success: api.download,
				error: () => {
					document.getElementsByTagName('progressbar')[0].style.display = null;
					document.getElementsByTagName('error')[0].innerHTML = 'Conversion failed. Please try again later.';
				}
			});
		} else
			document.getElementsByTagName('error')[0].innerHTML = 'Please select a file to convert.';
	}

	static download(id) {
		var count = 0;
		var download = function () {
			api.ajax({
				url: api.url + '/rest/api/pdf/' + id,
				method: 'GET',
				success: () => {
					document.getElementsByTagName('progressbar')[0].style.display = null;
					var link = document.createElement('a');
					link.setAttribute('href', api.url + '/rest/api/pdf/' + id);
					link.setAttribute('target', '_blank');
					link.click();
					document.getElementById('chatFile').value = '';
				},
				error: () => {
					if (++count > 600) {
						document.getElementsByName('progressbar')[0].style.display = null;
						document.getElementsByTagName('error')[0].innerHTML = 'Download failed after 10 minutes. Please try again later.';
						return;
					}
					setTimeout(download, 1000);
				}
			});
		}
		setTimeout(download, 1000);
	}

	static ajax(param) {
		var xhr = new XMLHttpRequest();
		xhr.onreadystatechange = function () {
			if (xhr.readyState == 4) {
				var errorHandler = function () {
				};
				if (xhr.status >= 200 && xhr.status < 300) {
					if (param.success) {
						var response = xhr.responseText;
						if (response && (response.indexOf('{') === 0 || response.indexOf('[') === 0)) {
							try {
								response = JSON.parse(xhr.responseText)
							} catch (e) {
							}
						}
						param.success(response);
					}
				} else
					errorHandler.call();
			}
		};
		xhr.open(param.method ? param.method : 'GET', param.url, true);
		if (typeof param.body == 'string')
			xhr.setRequestHeader('Content-Type', 'application/x-www-form-urlencoded');
		else if (!(param.body instanceof FormData) && param.body) {
			xhr.setRequestHeader('Content-Type', 'application/json');
			param.body = JSON.stringify(param.body);
		}
		xhr.send(param.body);
	}
}
