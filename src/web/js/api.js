export { api };

class api {
	static url = '{placeholderServer}/rest/api/';

	static analyse(data, success, error) {
		api.ajax({
			url: api.url + 'pdf/analyse',
			method: 'POST',
			body: data,
			success: success,
			error: error
		});
	}

	static preview(id, period, user, success, error) {
		api.ajax({
			noProgressBar: true,
			url: api.url + 'pdf/preview/' + id + '?period=' + encodeURIComponent(period) + '&user=' + encodeURIComponent(user),
			method: 'POST',
			success: success,
			error: error
		});
	}

	static buy(id, user, periods, summary, success, error) {
		document.dispatchEvent(new CustomEvent('progressbar', { detail: { type: 'open' } }));
		var period = '';
		for (var i = 0; i < periods.length; i++)
			period += 'periods=' + encodeURIComponent(periods[i]) + '&';
		api.ajax({
			noProgressBar: true,
			url: api.url + 'pdf/buy/' + id + '?' + period + 'user=' + encodeURIComponent(user) + '&summary=' + summary,
			method: 'POST',
			success: success,
			error: error
		});
	}

	static delete(id, success) {
		api.ajax({
			url: api.url + 'pdf/' + id,
			method: 'DELETE',
			success: success
		});
	}

	static saveFeedback(id, body, success, error) {
		api.ajax({
			url: api.url + 'feedback/' + id,
			method: 'PUT',
			body: body,
			success: success,
			error: error
		});
	}

	static feedback(success) {
		api.ajax({
			noProgressBar: true,
			url: api.url + 'feedback/list',
			success: success
		});
	}

	static feedbackConfirm(data, success, error) {
		api.ajax({
			url: api.url + 'feedback/confirm',
			method: 'PUT',
			body: data,
			success: success,
			error: error
		});
	}

	static ajax(param) {
		var xhr = new XMLHttpRequest();
		if (!param.method)
			param.method = 'GET';
		xhr.onreadystatechange = function () {
			if (xhr.readyState == 4) {
				if (!param.noProgressBar)
					document.dispatchEvent(new CustomEvent('progressbar'));
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
				} else {
					if (xhr.status < 500) {
						var xhrError = new XMLHttpRequest();
						xhrError.open('POST', api.url + 'ticket', true);
						xhrError.setRequestHeader('Content-Type', 'application/json');
						xhrError.send(JSON.stringify({ note: param.method + ' ' + param.url + ' -> ' + xhr.status + ' ' + xhr.responseURL + ' | ' + xhr.response }));
					}
					if (param.error) {
						xhr.param = param;
						param.error(xhr);
					} else
						document.dispatchEvent(new CustomEvent('popup', { detail: { body: 'An error occurred while processing your request. Please try again later.' } }));
				}
			}
		};
		xhr.open(param.method, param.url, true);
		if (typeof param.body == 'string')
			xhr.setRequestHeader('Content-Type', 'application/x-www-form-urlencoded');
		else if (param.body && !(param.body instanceof FormData)) {
			xhr.setRequestHeader('Content-Type', 'application/json');
			param.body = JSON.stringify(param.body);
		}
		if (!param.noProgressBar)
			setTimeout(function () { if (xhr.readyState != 4) document.dispatchEvent(new CustomEvent('progressbar', { detail: { type: 'open' } })) }, 100);
		xhr.send(param.body);
	}
}