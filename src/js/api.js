export { api };

class api {
	static url = '{placeholderServer}';

	static convert() {
		var file = document.getElementById('chatFile');
		if (file.files[0]) {
			var formData = new FormData();
			formData.append('file', file.files[0]);
			api.ajax({
				url: api.url + '/rest/api/convert',
				method: 'POST',
				body: formData,
				success: function (response) {
					var name = file.files[0].name;
					if (name.indexOf('.') > 0)
						name = name.substring(0, name.lastIndexOf('.'));
					var link = document.createElement('a');
					link.setAttribute('href', api.url + '/rest/api/pdf/' + response + '/' + name);
					link.setAttribute('target', '_blank');
					link.click();
					file.value = '';
				}
			});
		}
	}

	static ajax(param) {
		var xmlhttp = new XMLHttpRequest();
		xmlhttp.onreadystatechange = function () {
			if (xmlhttp.readyState == 4) {
				var errorHandler = function () {
				};
				if (xmlhttp.status >= 200 && xmlhttp.status < 300) {
					if (param.success) {
						var response = xmlhttp.responseText;
						if (response && (response.indexOf('{') === 0 || response.indexOf('[') === 0)) {
							try {
								response = JSON.parse(xmlhttp.responseText)
							} catch (e) {
							}
						}
						param.success(response);
					}
				} else
					errorHandler.call();
			}
		};
		xmlhttp.open(param.method ? param.method : 'GET', param.url, true);
		if (typeof param.body == 'string')
			xmlhttp.setRequestHeader('Content-Type', 'application/x-www-form-urlencoded');
		else if (!(param.body instanceof FormData) && param.body) {
			xmlhttp.setRequestHeader('Content-Type', 'application/json');
			param.body = JSON.stringify(param.body);
		}
		xmlhttp.send(param.body);
	}
}
