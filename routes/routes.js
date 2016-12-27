var Location = require('../models/Jams.js');

module.exports = function(app) {

	app.get('/', function(req, res) {
		res.json({ message: 'welcome to our api!' });
	});

	
}