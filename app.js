
var express = require('express');
var connect = require('connect');
var app = express();
var mongoose = require('mongoose');

//mongoose.connect('mongodb://127.0.0.1:27017/ojam-server5');
//mongoose.connect('');

console.log("Connected to db");

var port = process.env.PORT || 8080;

var server = app.listen(port);
console.log("The app running on port " + port);
server.timeout = 120000;

app.use(express.static(__dirname + '/public'));
app.use(connect.logger('dev'));

var bodyParser = require('body-parser');
app.use(bodyParser.json({limit: '5mb'}));
app.use(bodyParser.urlencoded({limit: '5mb', extended: true}));

//routes
require('./routes/routes.js')(app);
