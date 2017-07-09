'use strict';
const mongoose = require('mongoose');
const Venue = require('../../app/models/venue');
const Comment = require('../../app/models/comment');
const SearchRequest = require('../../app/models/searchrequest');
const Util = require('../../lib/util');
const chai = require('chai');
const chaiHttp = require('chai-http');
const server = require('../../server');
const User = require('../../app/models/user');
const jsonwt = require('jsonwebtoken');
const config = require('config');
const request = require('supertest');
chai.should();
chai.use(chaiHttp);
chai.use(require('chai-things'));

const mochaAsync = (fn) => {
    return (done) => {
        fn.call().then(done, (err) => {
            return done(err);
        });
    };
};

describe('venue', () => {

    let aVenue;
    let bVenue;
    let user, token;
    beforeEach(mochaAsync(async () => {
        mongoose.Promise = global.Promise;

        await Util.connectDatabase(mongoose);
        await Venue.remove({});
        await User.remove({});
        await SearchRequest.remove({});

        const res = await request(server)
            .post('/searches/venues')
            .send({
                locationName: 'Hügelstraße, Darmstadt',
                keyword: 'Krone',
                radius: 5000
            });
        aVenue = res.body.results[0];
        user = await User.create({name: 'peter111', email: 'peter123@cool.de', password: 'peter99', gender: 'm'});
        const bVenue = await Venue.findOne({_id: aVenue._id});
        token = jsonwt.sign(user.toJSON(), config.jwt.secret, config.jwt.options);
        const comment = await Comment.create({
            kind: 'VenueComment',
            author: user,
            text: 'this is a comment',
            likes: 0,
            dislikes: 0,
            date: Date.now(),
            venue: bVenue
        });
        const bcomment = await Comment.create({
            kind: 'VenueComment',
            author: user,
            text: 'this is a second comment',
            likes: 0,
            dislikes: 0,
            date: Date.now(),
            venue: bVenue
        });
        if(comment && bcomment) {
            bVenue.comments.push(comment);
            bVenue.comments.push(bcomment);
            await bVenue.save();
        }
    }));

    it('GET venue details', (mochaAsync(async () => {
        const res = await request(server)
            .get('/venues/' + aVenue._id + '');
        res.should.have.status(200);
        res.body.should.have.property('name');
        res.body.should.have.property('location');
    })));

    describe('checkin', () => {
        it('should count checkin', (mochaAsync(async () => {
            let res = await request(server)
                .put('/venues/' + aVenue._id + '/checkin')
                .set('x-auth', token);
            res.body.should.have.property('count', 1);

            res = await request(server)
                .put('/venues/' + aVenue._id + '/checkin')
                .set('x-auth', token);
            res.body.should.have.property('count', 2);
        })));
    });

    describe('comments', () => {
        it('should get all comments', (mochaAsync(async () => {
            const res = await request(server)
                .get('/venues/' + aVenue._id + '/comments');

            res.body.length.should.equal(2);
        })));
    });
});

describe('venue search', () => {

    before(async () => {
        mongoose.Promise = global.Promise;

        await Util.connectDatabase(mongoose);
        await Venue.remove({});
        await SearchRequest.remove({});
    });


    it('should return some places', (done) => {
        request(server)
            .post('/searches/venues')
            .send({
                location: {
                    type: 'Point',
                    coordinates: [-74.0059, 40.7127]
                },
                keyword: 'bar',
                radius: 1000
            })
            .end((err, res) => {
                res.should.have.status(200);
                res.body.results.should.all.not.have.property('comments');
                return done();
            });
    });

    it('should return krone in darmstadt for Krone', (done) => {
        request(server)
            .post('/searches/venues')
            .send({
                locationName: 'Hügelstraße, Darmstadt',
                keyword: 'Krone',
                radius: 5000
            })
            .end((err, res) => {
                res.should.have.status(200);
                res.body.results.should.contain.a.thing.with.property('name', 'Goldene Krone');
                return done();
            });
    });

    it('should return krone in darmstadt when searching for bar', (done) => {
        request(server)
            .post('/searches/venues')
            .send({
                locationName: 'Schustergasse 18, 64283 Darmstadt',
                keyword: 'bar',
                radius: 5000
            })
            .end((err, res) => {
                res.should.have.status(200);
                res.body.results.should.contain.a.thing.with.property('name', 'Goldene Krone');
                return done();
            });
    });

    it('should return hobbit in darmstadt', (done) => {
        request(server)
            .post('/searches/venues')
            .send({
                locationName: 'Kantplatz, Darmstadt',
                keyword: 'Hobbit',
                radius: 5000
            })
            .end((err, res) => {
                res.should.have.status(200);
                res.body.results.should.contain.a.thing.with.property('name', 'Hobbit');
                return done();
            });
    });
});