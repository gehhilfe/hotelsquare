'use strict';

const restify = require('restify');
const Venue = require('../models/venue');
const googleapilib = require('googleplaces');
const config = require('config');

/**
 * queries for venues
 *
 * @function query
 * @param {Object} request request
 * @param {Object} response response
 * @param {Function} next next handler
 * @returns {undefined}
 */
function queryVenue(request, response, next) {
    const api = googleapilib(config.googleapi.GOOGLE_PLACES_API_KEY, config.googleapi.GOOGLE_PLACES_OUTPUT_FORMAT);

    const params = {
        location: request.body.location,
        keyword: request.body.keyword
    };

    api.nearBySearch(params, function (error, res) {
        if (error) {
            response.send(500, error);
            return next();
        } else {
            response.json(res);
            return next();
        }
    });
}

module.exports = {
    queryVenue
};

