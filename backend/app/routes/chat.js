'use strict';
const _ = require('lodash');
const errors = require('restify-errors');
const mongoose = require('mongoose');
const User = require('../models/user');
const Chat = require('../models/chat');
const Message = require('../models/message');

/**
 * Starts new Chat
 * @Deprecated
 * @param {Object} request request
 * @param {Object} response response
 * @param {Function} next next handler
 * @returns {undefined}
 */
async function newChat(request, response, next) {
    if (!request.body.recipients) {
        return next(new errors.BadRequestError('You must at least have one recipient for the message.'));
    }

    if (request.body.message === '') {
        return next(new errors.BadRequestError('You must not send empty messages.'));
    }


    const recipients = _.map(await User.find({
        _id: {$in: request.body.recipients}
    }), (it) => it._id);

    if (recipients.length !== request.body.recipients.length) {
        return next(new errors.BadRequestError('Unknown recipient.'));
    }

    let chat = await Chat.create({
        participants: [request.authentication._id, recipients]
    });

    const msg = await Message.create({
        chatId: chat._id,
        message: request.body.message,
        sender: request.authentication._id
    });

    chat.addMessage(msg);
    chat = await chat.save();
    chat = await chat.populate({
        path: 'messages',
        populate: {
            path: 'sender',
            populate: {
                path: 'avatar'
            }
        },
        options: {
            limit: 1
        }
    }).populate({
        path: 'participants',
        populate: {
            path: 'avatar'
        }
    }).execPopulate();
    return response.send(chat);
}

/**
 * Replies to a message in a chat
 *
 * @param {Object} request request
 * @param {Object} response response
 * @param {Function} next next handler
 * @returns {undefined}
 */
async function replyMessage(request, response, next) {
    const chat = await Chat.findOne({
        _id: request.params.chatId
    });

    if (_.find(chat.participants, request.authentication._id))
        return next(new errors.BadRequestError('You are not are participant of this chat.'));

    let reply = await Message.create({
        chatId: chat._id,
        message: request.body.message,
        sender: request.authentication._id,
        date: Date.now()
    });

    chat.addMessage(reply);

    await chat.save();

    reply = await reply.populate({
        path: 'sender',
        populate: {
            path: 'avatar'
        }
    }).execPopulate();

    response.send(reply);
    return next();
}

/**
 * Retrieves all conversations from the user
 *
 * @param {Object} request request
 * @param {Object} response response
 * @param {Function} next next handler
 * @returns {undefined}
 */
async function getConversations(request, response, next) {
    // Get chats for user
    const chat = await Chat.find({
        participants: request.authentication._id
    }).populate({
        path: 'messages',
        populate: {
            path: 'sender',
            populate: {
                path: 'avatar'
            }
        },
        options: {
            limit: 1,
            sort: {date: -1}
        }
    }).populate({
        path: 'participants',
        populate: {
            path: 'avatar'
        }
    });
    response.send(chat);
    return next();
}

/**
 * Retrieves an existing conversation from the user to user or creates one
 *
 * @param {Object} request request
 * @param {Object} response response
 * @param {Function} next next handler
 * @returns {undefined}
 */
async function getOrCreateConversation(request, response, next) {
    if(!request.params.other_id)
        return next(new errors.BadRequestError('other_id is missing'));
    let chat = await Chat.findOne({
        participants: {$all: [request.params.other_id,  request.authentication._id]}
    }).populate({
        path: 'messages',
        populate: {
            path: 'sender',
            populate: {
                path: 'avatar'
            }
        },
        options: {
            limit: 1,
            sort: {date: -1}
        }
    }).populate({
        path: 'participants',
        populate: {
            path: 'avatar'
        }
    });
    if(chat) {
        response.send(chat);
        return next();
    } else {
        chat = await Chat.create({
            participants: [request.authentication._id, request.params.other_id]
        });
        getOrCreateConversation(request, response, next);
    }
}

/**
 * Retrieves whole conversation history of a chat
 *
 * @param {Object} request request
 * @param {Object} response response
 * @param {Function} next next handler
 * @returns {undefined}
 */
async function getConversation(request, response, next) {

    let page = 0;
    if(request.params.page)
        page = request.params.page;

    let filter = null;
    if(request.params.lastMessage) {
        filter = {
            _id: {$gt: new mongoose.Types.ObjectId(request.params.lastMessage)}
        };
    }

    // Get chats for user
    const chat = await Chat.findOne({
        _id: request.params.chatId,
        participants: request.authentication._id
    }).populate({
        path: 'messages',
        match: filter,
        populate: {
            path: 'sender',
            populate: {
                path: 'avatar'
            }
        },
        options: {
            limit: 20,
            sort: {date: -1},
            skip: page * 20
        }
    }).populate({
        path: 'participants',
        populate: {
            path: 'avatar'
        }
    });
    if (!chat)
        return next(new errors.BadRequestError('Chat not found.'));
    response.send(chat);
    return next();
}

module.exports = {
    replyMessage,
    getConversation,
    getConversations,
    getOrCreateConversation
};