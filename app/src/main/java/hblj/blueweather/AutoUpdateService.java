package hblj.blueweather;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import hblj.blueweather.gson.Weather;
import hblj.blueweather.util.HttpUtil;
import hblj.blueweather.util.Utility;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class AutoUpdateService extends Service {

    private SharedPreferences sharedPreferences;

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sharedPreferences = getSharedPreferences("weather_info",Context.MODE_PRIVATE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //更新数据
        updateWeather();
        updateBingPic();

        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        Intent it = new Intent(this,AutoUpdateService.class);
        PendingIntent pendingIntent = PendingIntent.getService(this,0,it,0);
        int anhour = 8 * 60 * 60 * 1000;
        //int anhour = 8 * 1000;
        long triggerTime = SystemClock.elapsedRealtime() + anhour;
        alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,triggerTime,pendingIntent);

        return START_NOT_STICKY;
    }

    /**
     * 更新天气信息
     */
    private void updateWeather(){
        String cityid = sharedPreferences.getString("cityid","");
        String url = "http://guolin.tech/api/weather?cityid=" + cityid + "&key=066cab6185734a03acd518652b46c773";
        if(TextUtils.isEmpty(url)){
            return;
        }
        HttpUtil.sendOkHttpRequest(url, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {

            }

            @Override
            public void onResponse(Call call, final Response response) {
                try {

                    String content = response.body().string();
                    final Weather weather = parseWeather(content);

                    if(weather != null && weather.getStatus().equals("ok")){
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putString("response",content);
                        editor.commit();
                        Log.e("tag",content);
                    }


                    Log.e("tag","weather:"+weather);

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * 更新 每天的图片
     */
    private void updateBingPic(){
        loadImage("http://guolin.tech/api/bing_pic");
    }

    private Weather parseWeather(String response) {
        try {
            JSONObject jsonObject = new JSONObject(response);
            JSONArray jsonArray = jsonObject.getJSONArray("HeWeather");
            String weatherContent = jsonArray.getJSONObject(0).toString();
            Weather weather = Utility.getWeatherInfo(weatherContent);
            return  weather;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void loadImage(String requestBingPic) {
        //空的则必须 加载
        HttpUtil.sendOkHttpRequest(requestBingPic, new Callback() {

            @Override
            public void onFailure(Call call, IOException e) {

            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String bingPic = response.body().string();
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("imageContent",bingPic);
                editor.commit();
            }

        });
    }
}
