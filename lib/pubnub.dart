import 'dart:async';
import 'dart:convert';
import 'package:flutter/services.dart';

class Pubnub {

  static var messageReceived;
  static var statusReceived;

  static const MethodChannel channelPubNub  = const MethodChannel('pubnub');
  static const EventChannel messageChannel = const EventChannel('messageStream');
  static const EventChannel statusChannel = const EventChannel('plugins.flutter.io/pubnub_status');

  Pubnub(String publishKey, String subscribeKey, String secretKey) {
    var args = {
      'publishKey': publishKey,
      'subscribeKey': subscribeKey,
      'secretKey': secretKey
    };
    channelPubNub.invokeMethod('create', args);
  }

  subscribe(String channel) async {

    var args = {
      'channelName': channel
    };

    if (channelPubNub != null) {
      channelPubNub.invokeMethod('subscribe', args);
    }
    else {
      new NullThrownError();
    }

  }

  Future<String> history(Map<String,dynamic> args) async {

    var data;

    if (channelPubNub != null) {
      data = await channelPubNub.invokeMethod('history', args);
    }
    else {
      new NullThrownError();
    }

    return data;

  }

  Future<bool> sendMessage(String message,String name, int idUsuario) async {

    bool data;

    var args = {
      'message': message,
      'senderName': name,
      'senderIdUsuario':idUsuario
    };
    if (channelPubNub != null) {
      data = await channelPubNub.invokeMethod('message', args);
    }
    else {
      new NullThrownError();
    }

    return data;
  }

  Stream<dynamic> get onMessageReceived {

    if (messageReceived == null) {
      messageReceived = messageChannel
        .receiveBroadcastStream()
        .map((dynamic event) => _parseMessage(event));
    }
    return messageReceived;

  }

  Stream<dynamic> get onStatusReceived {
    if (statusReceived == null) {
      statusReceived = statusChannel
        .receiveBroadcastStream()
        .map((dynamic event) => _parseStatus(event));
    }
    return statusReceived;
  }

  dynamic _parseMessage(messageString) {

    var message = jsonDecode(messageString);

    return message;
  }

  dynamic _parseStatus(status) {
    return status;
  }

}
