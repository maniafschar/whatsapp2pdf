export { api };

class api {
	static url = 'https://wa2pdf.com/rest/sc/';
	static user;

	static init() {
		api.ajax({
			url: api.url + 'init',
			success: xhr => {
				document.querySelector('logs').innerHTML = JSON.stringify(xhr.logs);
				document.querySelector('tickets').innerHTML = JSON.stringify(xhr.tickets);
			}
		});
	}

	static ajax(param) {
		var xhr = new XMLHttpRequest();
		xhr.onreadystatechange = function () {
			if (xhr.readyState == 4) {
				var errorHandler = function () {
					if (param.error) {
						xhr.param = param;
						param.error(xhr);
					} else {
						document.getElementsByTagName('progressbar')[0].style.display = null;
						document.getElementsByTagName('error')[0].innerHTML = 'An error occurred while processing your request. Please try again later.';
					}
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
					errorHandler();
			}
		};
		xhr.open(param.method ? param.method : 'GET', param.url, true);
		xhr.setRequestHeader('user', api.user);
		if (typeof param.body == 'string')
			xhr.setRequestHeader('Content-Type', 'application/x-www-form-urlencoded');
		else if (param.body && !(param.body instanceof FormData)) {
			xhr.setRequestHeader('Content-Type', 'application/json');
			param.body = JSON.stringify(param.body);
		}
		xhr.send(param.body);
	}
}
