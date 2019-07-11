package cultivandofuturo.com.pubnub.helpers;

import android.os.Handler;
import android.os.Looper;

import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.Result;

public class MainThreadResult implements MethodChannel.Result {

  public MethodChannel.Result result;
  public Handler handler;

  MainThreadResult(MethodChannel.Result result) {
    this.result = result;
    handler = new Handler(Looper.getMainLooper());
  }

  @Override
  public void success(final Object data) {
    handler.post(
        new Runnable() {
          @Override
          public void run() {
            result.success(data);
          }
        });
  }

  @Override
  public void error(
      final String errorCode, final String errorMessage, final Object errorDetails) {
    handler.post(
        new Runnable() {
          @Override
          public void run() {
            result.error(errorCode, errorMessage, errorDetails);
          }
        });
  }

  @Override
  public void notImplemented() {
    handler.post(
        new Runnable() {
          @Override
          public void run() {
            result.notImplemented();
          }
        });
  }
}