const path = require('path');
const express = require('express');

module.exports = (env) => {
	return {
		entry: './src/js/start.js',
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
						if (!env.server)
							throw 'Server not set!\ne.g. npx webpack --env server=http://localhost:9000';
						var fs = require('fs');
						var file = 'dist/js/main.js';
						fs.writeFileSync(file, fs.readFileSync(file, 'utf8')
							.replace('{placeholderServer}', env.server));
						if (env.emoji) {
							fs.mkdirSync('main/resources/emoji');
							const fontkit = require('fontkit');
							const emoji = require('node-emoji');
							const font = fontkit.openSync('./Apple Color Emoji.ttc').fonts[0];
							let x = '100';
							let run = font.layout(emoji.get(x));
							let glyph = run.glyphs[0].getImageForSize(128);
							fs.writeFileSync('main/resources/emoji/' + x + '.png', glyph.data);
						}
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
						options: {
							presets: ['@babel/preset-env'],
							plugins: ['@babel/plugin-transform-runtime']
						}
					}
				}
			]
		}
	}
}
