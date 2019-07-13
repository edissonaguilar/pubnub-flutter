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

    pubnub.onStatusReceived.listen((data) {
      
      var type = data['type'];
      var payload = data['data'];

      print(type);

      if(type=="message"){
        print(json.decode(payload));
      }

      setState(() {
        receivedStatus = payload;
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
                            "cf-channel-20a32390-a718-416a-aaf8-b929301ac22d");
                      },
                      child: Text("Subscribe"))
                ]),
            FlatButton(
                color: Colors.black12,
                onPressed: () async {
                  // var data = await pubnub.history({
                  //   "channel":
                  //       "cf-channel-20a32390-a718-416a-aaf8-b929301ac22d",
                  //   "count": 5
                  // });

                  // var json= jsonDecode(data);

                  // print(json);

                  pubnub.sendMessage(sendMessage,'Edisson Flutter',0);
                },
                child: Text("Send Message")),
          ],
        )),
      ),
    );
  }
}
