var Location = require('../models/Location.js');

module.exports = function(app) {

	app.get('/', function(req, res) {
		res.json({ message: 'welcome to our api!' });
	});
	
	app.post('/location', function(req, res) {
		var loc = req.location;
		var updated_at = req.updated_at;
		
		
	});

	
}
