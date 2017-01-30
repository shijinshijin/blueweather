package hblj.blueweather;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import hblj.blueweather.gson.Weather;
import hblj.blueweather.util.HttpUtil;
import hblj.blueweather.util.Utility;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class WeatherActivity extends AppCompatActivity {

    private Unbinder unbinder;

    private SharedPreferences sharedPreferences;

    private ScrollView weather_scrollview;

    @BindView(R.id.drawerlayout) DrawerLayout drawerlayout;

    @BindView(R.id.bing_pic_img) ImageView bing_pic_img;
    //当前显示的城市
    @BindView(R.id.title_city) TextView title_city;
    //最新更新的时间
    @BindView(R.id.title_update_time) TextView title_update_time;
    //当前显示的温度
    @BindView(R.id.degree_text) TextView degree_text;
    //当前的天气情况(晴，雨 等)
    @BindView(R.id.weather_info_text) TextView weather_info_text;
    //加载最近几天的天气的布局
    @BindView(R.id.forecast_layout) LinearLayout forecast_layout;
    //当前空气的 aqi 指数
    @BindView(R.id.aqi_text) TextView aqi_text;
    //当前空气的 pm2.5 指数
    @BindView(R.id.aqi_pm) TextView aqi_pm;
    //舒适指数
    @BindView(R.id.comfort_text) TextView comfort_text;
    //洗车指数
    @BindView(R.id.car_wash_text) TextView car_wash_text;
    //运动指数
    @BindView(R.id.sport_text) TextView sport_text;

    @BindView(R.id.swipe_refresh) SwipeRefreshLayout swipe_refresh;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather);
        unbinder = ButterKnife.bind(this);

        //开启服务
        Intent intent = new Intent(WeatherActivity.this,AutoUpdateService.class);
        startService(intent);

        //当系统版本为4.4或者4.4以上时可以使用沉浸式状态栏
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            //透明状态栏
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            //透明导航栏
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        }

        sharedPreferences = getSharedPreferences("weather_info", Context.MODE_PRIVATE);
        String response = sharedPreferences.getString("response","");
        weather_scrollview = (ScrollView) findViewById(R.id.weather_scrollview);

        if(TextUtils.isEmpty(response)){
            //发送联网请求
            //"http://guolin.tech/api/weather?cityid=CN101190401&key=066cab6185734a03acd518652b46c773"
            String cityid = getIntent().getStringExtra("cityid");
            String url = "http://guolin.tech/api/weather?cityid=" + cityid + "&key=066cab6185734a03acd518652b46c773";
            weather_scrollview.setVisibility(View.GONE);
            initWeather(url);
        }else{
            //直接读取
            Weather weather = parseWeather(response);
            updateWindow(weather);
        }

        getBingPicImg();

        swipe_refresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                swipe_refresh.setColorSchemeResources(R.color.colorPrimary);

                String cityid = sharedPreferences.getString("cityid","");
                String url = "http://guolin.tech/api/weather?cityid="+cityid+"&key=066cab6185734a03acd518652b46c773";
                initWeather(url);
            }
        });

    }

    @OnClick({R.id.choose_area_btn})
    public void choosePlace(View view){
        drawerlayout.openDrawer(GravityCompat.START);
    }

    public void initWeather(String url) {
        HttpUtil.sendOkHttpRequest(url, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {

            }

            @Override
            public void onResponse(Call call, final Response response) {
                try {
                    String content = response.body().string();
                    final Weather weather = parseWeather(content);

                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString("response",content);
                    editor.commit();

                    Log.e("tag","weather:"+weather);

                    //在主界面进行更新操作
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if(weather != null && weather.getStatus().equals("ok")){
                                Log.e("tag","weather ==run():"+weather);
                                updateWindow(weather);
                            }
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }


    /**
     * 解析返回的 天气有关的字符串
     * @param response
     */
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

    //用来更新界面的操作
    private void updateWindow(Weather weather) {
        //当前的城市
        String city = weather.getBasic().getCity();
        //更新时间
        String updateTime = weather.getBasic().getUpdate().getLoc();
        //当前显示的温度
        String degree = weather.getNow().getTmp() + "℃";
        //当前的天气情况
        String weatherInfo = weather.getNow().getCond().getTxt();
        //最近几天的天气
        List<Weather.DailyForecastBean> futureList = weather.getDaily_forecast();

        if(weather.getAqi() != null){
            //当前空气的 aqi 指数
            String aqi = weather.getAqi().getCity().getAqi();
            //当前空气的 pm2.5 指数
            String pm = weather.getAqi().getCity().getPm25();

            aqi_text.setText(aqi);
            aqi_pm.setText(pm);
        }
        //舒适指数
        String comfort = weather.getSuggestion().getComf().getBrf() + ":" +
                        weather.getSuggestion().getComf().getTxt();
        //洗车指数
        String carwash = weather.getSuggestion().getCw().getBrf() + ":" +
                        weather.getSuggestion().getCw().getTxt();
        //运动指数
        String sport = weather.getSuggestion().getSport().getBrf() + ":" +
                        weather.getSuggestion().getSport().getTxt();

        title_city.setText(city);
        title_update_time.setText(updateTime);
        degree_text.setText(degree);
        weather_info_text.setText(weatherInfo);

        if(forecast_layout.getChildCount()>0){
            forecast_layout.removeAllViews();
        }

        for(Weather.DailyForecastBean forecast:futureList){
            View view = LayoutInflater.from(this).inflate(R.layout.forecast_item,forecast_layout,false);
            TextView date_text = (TextView) view.findViewById(R.id.date_text);
            TextView info_text = (TextView) view.findViewById(R.id.info_text);
            TextView max_text = (TextView) view.findViewById(R.id.max_text);
            TextView min_text = (TextView) view.findViewById(R.id.min_text);

            date_text.setText(forecast.getDate());
            info_text.setText(forecast.getCond().getTxt_n());
            max_text.setText(forecast.getTmp().getMax());
            min_text.setText(forecast.getTmp().getMin());

            forecast_layout.addView(view);
        }


        comfort_text.setText(comfort);
        car_wash_text.setText(carwash);
        sport_text.setText(sport);
        Log.e("tag","refresh");
        weather_scrollview.setVisibility(View.VISIBLE);
        swipe_refresh.setRefreshing(false);
    }

    /**
     * 获取每日一图
     */
    private void getBingPicImg(){
        String imageContent = sharedPreferences.getString("imageContent","");
        String requestBingPic = "http://guolin.tech/api/bing_pic";
        if(TextUtils.isEmpty(imageContent)){
            loadImage(requestBingPic);
        }else{
            Glide.with(WeatherActivity.this).load(imageContent).into(bing_pic_img);
        }
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
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Glide.with(WeatherActivity.this).load(bingPic).into(bing_pic_img);
                    }
                });
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbinder.unbind();
    }

}
