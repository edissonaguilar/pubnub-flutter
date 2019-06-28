package cultivandofuturo.com.pubnub;

import android.util.Log;

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
import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;

public class PubnubPlugin implements MethodCallHandler {

  private PubNub pubnub;
	private String channelName = "test";
	private static EventChannel.EventSink messageSender;
	private static EventChannel.EventSink statusSender;
  private String uuid = "";
  
  public static void registerWith(Registrar registrar) {

    PubnubPlugin plugin = new PubnubPlugin();

    final MethodChannel channel = new MethodChannel(registrar.messenger(), "pubnub");
    channel.setMethodCallHandler(plugin);

    final EventChannel messageChannel = new EventChannel(registrar.messenger(),"messageStream");

    messageChannel.setStreamHandler(new EventChannel.StreamHandler(){
			@Override
			public void onListen(Object arguments, EventChannel.EventSink events){
				messageSender = events;
			}

			@Override
			public void onCancel(Object arguments)
			{
				Log.d(getClass().getName(), "messageChannel.onCancel");
			}
    });
    
    EventChannel statusChannel = new EventChannel(registrar.messenger(), "plugins.flutter.io/pubnub_status");

    statusChannel.setStreamHandler(new EventChannel.StreamHandler(){

			@Override public void onListen(Object o, EventChannel.EventSink eventSink){
				Log.d(getClass().getName(), "statusChannel.onListen");
				statusSender = eventSink;
			}

			@Override public void onCancel(Object o){
				Log.d(getClass().getName(), "statusChannel.onCancel");
			}
		});

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
  
    Log.d(getClass().getName(), "Create pubnub with publishKey " + publishKey + ", subscribeKey " + subscribeKey + " uuid" + uuid);
  
    if ((publishKey != null && !publishKey.isEmpty()) && (subscribeKey != null && !subscribeKey.isEmpty())){

      PNConfiguration pnConfiguration = new PNConfiguration();
      pnConfiguration.setPublishKey(publishKey);
      pnConfiguration.setSubscribeKey(subscribeKey);
      pnConfiguration.setUuid(uuid);
      pnConfiguration.setSecretKey(secretKey);
      pnConfiguration.setSecure(true);
      pnConfiguration.setLogVerbosity(PNLogVerbosity.BODY);
  
      pubnub = new PubNub(pnConfiguration);
      Log.d(getClass().getName(), "PubNub configuration created");
      result.success("PubNub configuration created");
    }
    else{
      Log.d(getClass().getName(), "Keys should not be null");
      result.success("Keys should not be null");
    }
  }
  
  private void subscribeToChannel(MethodCall call, final Result result){
  
    /* Subscribe to the demo_tutorial channel */
    channelName = call.argument("channelName");
  
    Log.d(getClass().getName(), "Attempt to Subscribe to channel: " + channelName);
  
    try{
  
      pubnub.addListener(new SubscribeCallback(){
  
        @Override public void status(PubNub pubnub, PNStatus status){
  
          if (status.getCategory() == PNStatusCategory.PNConnectedCategory){
            Log.d(getClass().getName(), "Subscription was successful at channel " + channelName);
            statusSender.success("Subscription was successful at channel " + channelName);
            result.success(true);
          }else{
            Log.d(getClass().getName(), "Subscription failed at channe l" + channelName);
            Log.d(getClass().getName(), status.getErrorData().getInformation());
            statusSender.success("Subscription failed at channel " + channelName + "'\n" + status.getErrorData().getInformation());
            result.success(false);
          }
        }
  
        @Override 
        public void message(PubNub pubnub, PNMessageResult message){

          try {
            if(messageSender!=null){

              String dataString=message.getMessage().toString();

              messageSender.success(dataString);

            }
          } catch (Exception e) {
            messageSender.success("Failed to parse message");
            e.printStackTrace();
          }

        }
  
        @Override public void presence(PubNub pubnub, PNPresenceEventResult presence)
        {
          Log.d(getClass().getName(), "Presence: getChannel " + presence.getChannel() + "getEvent " + presence.getEvent() + "getSubscription " + presence.getSubscription() + "getUuid " + presence.getUuid());
        }
      });
  
      pubnub.subscribe().channels(Arrays.asList(channelName)).execute();
  
    }
    catch (Exception e){
  
      Log.d(getClass().getName(), e.getMessage());
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


