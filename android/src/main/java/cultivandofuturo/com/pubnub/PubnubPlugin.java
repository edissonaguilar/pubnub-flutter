package cultivandofuturo.com.pubnub;

import android.util.Log;
import android.os.Handler;
import android.os.Looper;
import android.app.Activity;

import com.pubnub.api.PNConfiguration;
import com.pubnub.api.PubNub;
import com.pubnub.api.callbacks.PNCallback;
import com.pubnub.api.callbacks.SubscribeCallback;
import com.pubnub.api.enums.PNLogVerbosity;
import com.pubnub.api.enums.PNStatusCategory;
import com.pubnub.api.models.consumer.PNStatus;
import com.pubnub.api.models.consumer.pubsub.PNMessageResult;
import com.pubnub.api.models.consumer.pubsub.PNPresenceEventResult;
import com.pubnub.api.models.consumer.history.PNHistoryItemResult;
import com.pubnub.api.models.consumer.history.PNHistoryResult;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map; 
import java.util.List;
import java.util.ArrayList;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import cultivandofuturo.com.pubnub.util.DateTimeUtil;
import cultivandofuturo.com.pubnub.helpers.MainThreadResult;
import cultivandofuturo.com.pubnub.helpers.MainThreadEventSink;

import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;

public class PubnubPlugin implements MethodCallHandler {

  private PubNub pubnub;
  private String channelName = "test";
  
	private static EventChannel.EventSink statusSender;
  private String uuid = "";
  private final Activity activity;

  public static void registerWith(Registrar registrar) {

    MethodChannel channel = new MethodChannel(registrar.messenger(), "pubnub");
    channel.setMethodCallHandler(new PubnubPlugin(registrar.activity()));

    EventChannel statusChannel = new EventChannel(registrar.messenger(), "plugins.flutter.io/pubnub_status");

    statusChannel.setStreamHandler(new EventChannel.StreamHandler(){

			@Override public void onListen(Object o, EventChannel.EventSink eventSink){
				System.out.println( "statusSender.onListen");
				statusSender = eventSink;
			}

			@Override public void onCancel(Object o){
				System.out.println( "statusSender.onCancel");
			}
		});

  }

  private PubnubPlugin(Activity activity) {
    this.activity = activity;
  }

  @Override
  public void onMethodCall(MethodCall call, Result result) {

    switch (call.method){
      case "create":
				createChannel(call, result);
			break;
			case "subscribe":
				subscribeToChannel(call, result);
			break;
			case "message":
				sendMessageToChannel(call, result);
      break;
      case "history":
        getHistoryChannel(call, result);
			break;
			default:
				result.notImplemented();
    }
    
  }

  private void createChannel(MethodCall call, Result result){
  
    String publishKey = call.argument("publishKey");
    String subscribeKey = call.argument("subscribeKey");
    String secretKey = call.argument("secretKey");
  
    // uuid = java.util.UUID.randomUUID().toString();
    uuid = "cultivando-futuro-app";
  
    System.out.println( "Create pubnub with publishKey " + publishKey + ", subscribeKey " + subscribeKey + " uuid" + uuid);
  
    if ((publishKey != null && !publishKey.isEmpty()) && (subscribeKey != null && !subscribeKey.isEmpty())){

      PNConfiguration pnConfiguration = new PNConfiguration();
      pnConfiguration.setPublishKey(publishKey);
      pnConfiguration.setSubscribeKey(subscribeKey);
      pnConfiguration.setUuid(uuid);
      pnConfiguration.setSecretKey(secretKey);
      pnConfiguration.setSecure(true);
      pnConfiguration.setLogVerbosity(PNLogVerbosity.BODY);
  
      pubnub = new PubNub(pnConfiguration);
      System.out.println( "PubNub configuration created");
      result.success("PubNub configuration created");
    }
    else{
      System.out.println( "Keys should not be null");
      result.success("Keys should not be null");
    }
  }

  private void sendStream(final HashMap<String, String> data,final EventChannel.EventSink sender){

    activity.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        sender.success(data);
      }
    });

  }
  
  private void subscribeToChannel(MethodCall call, final Result result){
  
    /* Subscribe to the demo_tutorial channel */
    channelName = call.argument("channelName");
  
    System.out.println( "Attempt to Subscribe to channel: " + channelName);
  
    try{
  
      pubnub.addListener(new SubscribeCallback(){
  
        @Override public void status(PubNub pubnub, PNStatus status){
  
          if (status.getCategory() == PNStatusCategory.PNConnectedCategory){
            System.out.println( "Subscription was successful at channel " + channelName);

            HashMap<String, String> map = new HashMap<>(); 

            map.put("type","status");
            map.put("data","Subscription was successful at channel " + channelName);

            statusSender.success(map);
          }else{
            System.out.println( "Subscription failed at channe l" + channelName);
            System.out.println( status.getErrorData().getInformation());
            statusSender.success("Subscription failed at channel " + channelName + "'\n" + status.getErrorData().getInformation());
            result.success(false);
          }
        }
  
        @Override 
        public void message(PubNub pubnub, PNMessageResult message){

          System.out.println( "Pubnub: message " + message.getMessage());

          try {
            
            String dataString = message.getMessage().toString();

            HashMap<String, String> map = new HashMap<>(); 

            map.put("type","message");
            map.put("data",dataString);

            sendStream(map,statusSender);

          } catch (Exception e) {
            System.out.println(e);
            e.printStackTrace();
          }

        }
  
        @Override public void presence(PubNub pubnub, PNPresenceEventResult presence)
        {
          System.out.println( "Presence: getChannel " + presence.getChannel() + "getEvent " + presence.getEvent() + "getSubscription " + presence.getSubscription() + "getUuid " + presence.getUuid());
        }
      });
  
      pubnub.subscribe().channels(Arrays.asList(channelName)).execute();
  
    }
    catch (Exception e){
  
      System.out.println( e.getMessage());
      result.success(false);
  
    }
  }
  
  private void sendMessageToChannel(MethodCall call, final Result result){
  
    JsonObject obj = new JsonObject();

    obj.addProperty("text",(String)call.argument("message"));
    obj.addProperty("timestamp",DateTimeUtil.getTimeStampUtc());

    JsonObject sender = new JsonObject();

    sender.addProperty("name", (String)call.argument("senderName"));
    sender.addProperty("idUsuario", (Integer)call.argument("senderIdUsuario"));

    obj.add("sender",sender);
  
    pubnub.publish().channel(channelName).message(obj).async(
  
      new PNCallback(){
        @Override public void onResponse(Object object, PNStatus status){
          try{
            if (!status.isError()){
              result.success(true);
            }
            else{
              result.success(false);
            }
          }
          catch (Exception e){
            e.printStackTrace();
            result.success(false);
          }
        }
  
      }
    );
      
  }

  private void getHistoryChannel(MethodCall call, final Result result){

    pubnub.history()
      .channel((String)call.argument("channel"))
      .count((Integer)call.argument("count"))
      .async(new PNCallback<PNHistoryResult>(){
        @Override
        public void onResponse(PNHistoryResult resultData, PNStatus status) {
          try{
            if (!status.isError()){

              List<PNHistoryItemResult> messagesPubnub = resultData.getMessages();
              JsonArray messages = new JsonArray();

              for (int i = 0; i < messagesPubnub.size(); i++) {
   
     
                String messageString = messagesPubnub.get(i).getEntry().toString();
                JsonObject message= new JsonParser().parse(messageString).getAsJsonObject();

                messages.add(message);
          
              }

              result.success(messages.toString());
              
            }
            else{
              
            }
          }
          catch (Exception e){
            e.printStackTrace();
            result.success(false);
          }
        }
      });
  
  }

}


