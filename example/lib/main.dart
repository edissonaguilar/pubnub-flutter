import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:pubnub/pubnub.dart';

void main() => runApp(MyApp());

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  Pubnub pubnub;
  String receivedStatus = 'Status: Unknown';
  String receivedMessage = '';
  String sendMessage = '';

  @override
  void initState() {
    super.initState();

    pubnub = new Pubnub(
        "pub-c-103e0292-fe69-41d7-945c-cd51fcea3d35",
        "sub-c-6eb2ddbe-5197-11e9-a1ab-dae925867aca",
        "sec-c-YjUwNDkxYzktMmIyMy00NmZmLWI3NjctMWZhYTU2YzBmOWZi");

    pubnub.onStatusReceived.listen((status) {
      setState(() {
        receivedStatus = status;
      });
    });

    pubnub.onMessageReceived.listen((message) {
      print(message);
      setState(() {
        receivedMessage = message['text'];
      });
    });
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      theme: ThemeData(primaryColor: Colors.red),
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Pubnub Plugin'),
        ),
        body: Center(
            child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: <Widget>[
            new Expanded(
              child: new Text(
                receivedStatus,
                style: new TextStyle(color: Colors.black45),
              ),
            ),
            new Expanded(
              child: new Text(
                receivedMessage,
                style: new TextStyle(color: Colors.black45),
              ),
            ),
            TextField(
              maxLength: 80,
              onChanged: (text) {
                sendMessage = text;
              },
              decoration: InputDecoration(
                  border: OutlineInputBorder(),
                  hintText: "Message to send",
                  hintStyle: TextStyle(
                      fontWeight: FontWeight.w300, color: Colors.grey)),
              style:
                  TextStyle(color: Colors.black, fontWeight: FontWeight.w300),
            ),
            Row(
                mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                children: <Widget>[
                  FlatButton(
                      color: Colors.black12,
                      onPressed: () {
                        pubnub.subscribe(
                            "cf-channel-78883642-7646-401d-82e8-08aa64a76887");
                      },
                      child: Text("Subscribe"))
                ]),
            FlatButton(
                color: Colors.black12,
                onPressed: () async {
                  var data = await pubnub.history({
                    "channel":
                        "cf-channel-78883642-7646-401d-82e8-08aa64a76887",
                    "count": 5
                  });

                  // print(data);

                  var json= jsonDecode(data);

                  print(json.runtimeType);

                  // var data =await pubnub.sendMessage(sendMessage,'Edisson Flutter',0);
                },
                child: Text("Send Message")),
          ],
        )),
      ),
    );
  }
}
