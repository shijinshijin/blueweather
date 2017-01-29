package hblj.blueweather.util;

import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;

/**
 * Created by shijin on 2017/1/28.
 */

public class HttpUtil {

    public static void sendOkHttpRequest(String address, Callback callback){

        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                            .url(address)
                            .build();
        //注册一个回调来处理服务器的响应
        client.newCall(request).enqueue(callback);
    }
}
