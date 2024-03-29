'use strict';

const _ = require('lodash');
const restify = require('restify');
const Comments = require('../models/comments');

const Comment = Comments.Comment;
const TextComment = Comments.TextComment;
const ImageComment = Comments.ImageComment;

const Image = require('../models/image');
const Venue = require('../models/venue');
const User = require('../models/user');


/**
 * Add like for authenticated user
 *
 * @param {IncomingMessage} request request
 * @param {Object} response response
 * @param {Function} next next handler
 * @returns {undefined}
 */
async function like(request, response, next) {
    const comment = await Comment.findOne({_id: request.params.id}).populate({
        path: 'author image',
        populate: {
            path: 'avatar'
        }
    });
    comment.like(request.authentication);
    await comment.save();
    response.send(comment.toJSONDetails());
    return next();
}


/**
 * Add dislike for authenticated user
 *
 * @param {IncomingMessage} request request
 * @param {Object} response response
 * @param {Function} next next handler
 * @returns {undefined}
 */
async function dislike(request, response, next) {
    const comment = await Comment.findOne({_id: request.params.id}).populate({
        path: 'author image',
        populate: {
            path: 'avatar'
        }
    });
    comment.dislike(request.authentication);
    await comment.save();
    response.send(comment.toJSONDetails());
    return next();
}

/**
 * Creates closure handler for creating text comments by adding these to a model
 * @param {Object} model Model to add comments to e.g. User, Venue, Image ...
 * @returns {function(*, *, *)} handler for text comment creation
 */
function textComment(model) {
    return async (request, response, next) => {
        const [o, author] = await Promise.all([
            model.findOne({_id: request.params.id}),
            User.findOne({_id: request.authentication._id}).populate('avatar')
        ]);

        if (!author || !o) {
            return next(restify.errors.BadRequestError('Author or target model not found!'));
        }

        const textComment = await TextComment.build(author, request.body.text, o);
        await o.save();
        response.send(textComment.toJSONDetails());
        return next();
    };
}

/**
 * Creates closure handler for creating image comments by adding these to a model
 * @param {Object} model Model to add comments to e.g. User, Venue, Image ...
 * @returns {function(*, *, *)} handler for text comment creation
 */
function imageComment(model) {
    return async (request, response, next) => {
        const [o, author] = await Promise.all([
            model.findOne({_id: request.params.id}),
            User.findOne({_id: request.authentication._id}).populate('avatar')
        ]);

        if (!author || !o) {
            return next(restify.errors.BadRequestError('Author or target model not found!'));
        }

        const imageComment = await ImageComment.build(author, request.files.image.path, o);
        await o.save();
        response.send(imageComment.toJSONDetails());
        return next();
    };
}

/**
 * Creates closure handler for retrieving comments from a given model
 * @param {Object} model Model to add comments to e.g. User, Venue, Image ...
 * @returns {function(*, *, *)} handler for text comment creation
 */
function getComments(model) {
    return async (request, response, next) => {
        let page = request.params.page;
        if (!page)
            page = 0;
        const o = await model.findOne({_id: request.params.id})
            .populate({
                path: 'comments.item',
                populate: {
                    path: 'author image',
                    populate: {
                        path: 'avatar'
                    }
                }
            })
            .slice('comments', [10 * page, 10]);
        response.send(_.map(o.comments, (e) => e.item.toJSONDetails()));
        return next();
    };
}

module.exports = {
    like,
    dislike,
    textComment,
    imageComment,
    getComments
};