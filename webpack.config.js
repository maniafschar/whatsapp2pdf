const path = require('path');

const babelConfig = {
	presets: ['@babel/preset-env'],
	plugins: ['@babel/plugin-transform-runtime']
};

module.exports = (env) => {
	if (!env.server) {
		throw new Error('Server not set!\ne.g. npx webpack --env server=http://localhost:9000');
	}

	return [{
		entry: './src/web/admin/js/main.js',
		mode: 'production',
		output: {
			globalObject: 'this',
			filename: 'admin/js/main.js',
			path: path.resolve(__dirname, 'dist'),
		},
		optimization: {
			minimize: true
		},
		target: ['web', 'es5'],
		module: {
			rules: [
				{
					test: /\.m?js$/,
					exclude: /(node_modules|bower_components)/,
					use: {
						loader: 'babel-loader',
						options: babelConfig
					}
				}
			]
		}
	},
	{
		entry: './src/web/js/action.js',
		mode: 'production',
		output: {
			globalObject: 'this',
			filename: 'js/main.js',
			path: path.resolve(__dirname, 'dist'),
		},
		optimization: {
			minimize: true
		},
		target: ['web', 'es5'],
		devServer: {
			static: {
				directory: path.join(__dirname, 'dist')
			},
			client: {
				overlay: false,
			},
			compress: true,
			port: 9000,
			hot: false,
			liveReload: false,
			setupMiddlewares: (middlewares, devServer) => {
				const express = require('express');
				devServer.app.use('/', express.static(path.resolve(__dirname, 'dist')));
				return middlewares;
			},
			devMiddleware: {
				writeToDisk: true
			}
		},
		plugins: [
			{
				apply: compiler => {
					compiler.hooks.afterEmit.tap('params', () => {
						const fs = require('fs');
						const files = ['dist/js/main.js', 'dist/admin/js/main.js'];
						files.forEach(file => {
							fs.writeFileSync(file, fs.readFileSync(file, 'utf8')
								.replace('{placeholderServer}', env.server));
						});
					})
				}
			}
		],
		module: {
			rules: [
				{
					test: /\.m?js$/,
					exclude: /(node_modules|bower_components)/,
					use: {
						loader: 'babel-loader',
						options: babelConfig
					}
				}
			]
		}
	}]
}