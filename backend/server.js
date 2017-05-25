'use strict';
const os = require('os');
const config = require('config');
const restify = require('restify');
const session = require('./app/routes/session');
const user = require('./app/routes/user');
const util = require('./lib/util');
const mongoose = require('mongoose');
const auth = require('./app/middleware/filter/authentication');
const  restifyBunyanLogger = require('restify-bunyan-logger');
const fs = require('fs');


mongoose.Promise = global.Promise;

const server = restify.createServer();

util.connectDatabase(mongoose).then(() => {
    //Bootstrap database
    if (process.env.NODE_ENV !== 'production') {
        const User = require('./app/models/user');

        if (config.bootstrap) {
            if (config.bootstrap.User)
                util.bootstrap(User, config.bootstrap.User);
        }
    }
});


const db = mongoose.connection;
db.on('error', console.error.bind(console, 'connection error:'));
server.use(restify.bodyParser({
    maxBodySize: 1024*1024,
    mapParams: true,
    mapFiles: true,
    overrideParams: false,
    keepExtensions: true,
    uploadDir: os.tmpdir(),
    multiples: true,
    hash: 'sha1'
}));

server.on('after', restifyBunyanLogger());

// session
server.post('session', session.postSession);

// user
server.post('user', user.postUser);
server.del('user', auth, user.deleteUser);
server.post('user/avatar', auth, user.uploadAvatar);
server.get('user/avatar', auth, user.getAvatar);

// delete downloads
server.on('after', (request) => {
    if(request.files) {
        const key = Object.keys(request.files);
        key.forEach((k) => {
            fs.unlink(request.files[k].path, () => {});
        });
    }
});

server.listen(8081, function () {
    console.log('%s listening at %s', server.name, server.url);
});

module.exports = server;