package hblj.blueweather;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.WindowManager;
import android.widget.FrameLayout;

public class MainActivity extends AppCompatActivity {

    private FrameLayout main_framelayput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //当系统版本为4.4或者4.4以上时可以使用沉浸式状态栏
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            //透明状态栏
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            //透明导航栏
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        }


        main_framelayput = (FrameLayout) findViewById(R.id.main_framelayput);

        SharedPreferences sharedPreferences = getSharedPreferences("weather_info", Context.MODE_PRIVATE);
        String cityid = sharedPreferences.getString("cityid","");
        if(!TextUtils.isEmpty(cityid)){
            Intent intent = new Intent(this, WeatherActivity.class);
            intent.putExtra("cityid",cityid);
            startActivity(intent);
            finish();
        }
    }
}
