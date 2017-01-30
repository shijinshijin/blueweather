package hblj.blueweather;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.litepal.crud.DataSupport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import hblj.blueweather.db.City;
import hblj.blueweather.db.County;
import hblj.blueweather.db.Province;
import hblj.blueweather.util.HttpUtil;
import hblj.blueweather.util.Utility;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

import static org.litepal.crud.DataSupport.findAll;

/**
 * Created by shijin on 2017/1/29.
 */

public class ChooseAreaFragment extends Fragment {

    private static final int LEVEL_PROVINCE = 0;

    private static final int LEVEL_CITY = 1;

    private static final int LEVEL_COUNTY = 2;

    private int currentLevel;

    private TextView title_text;

    private ListView list_view;

    private Button back_button;

    private ProgressDialog progressDialog;

    private ArrayAdapter adapter;

    private ArrayList<String> dataList = new ArrayList<String>();

    private List<Province> provinceList;

    private List<City> cityList;

    private List<County> countyList;

    private Province selectProvince;

    private City selectCity;

    private SharedPreferences sharedPreferences;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.choose_area,container,false);
        title_text = (TextView) view.findViewById(R.id.title_text);
        list_view = (ListView) view.findViewById(R.id.list_view);
        back_button = (Button) view.findViewById(R.id.back_button);

        sharedPreferences = getActivity().getSharedPreferences("weather_info", Context.MODE_PRIVATE);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        adapter = new ArrayAdapter(getActivity(),android.R.layout.simple_list_item_1,dataList);
        queryProvince();
        list_view.setAdapter(adapter);

        list_view.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if(currentLevel == LEVEL_PROVINCE){
                    //当前页面在选择省份
                    selectProvince = provinceList.get(position);
                    queryCities();
                }else if(currentLevel == LEVEL_CITY){
                    //当前页面在选择城市
                    selectCity = cityList.get(position);
                    queryCounties();
                }else if(currentLevel == LEVEL_COUNTY){
                    //当前页面在选择乡镇
                    //将当前选择的城市写入sharedpreference 避免重复进入
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    String cityid = countyList.get(position).getWeatherId();
                    editor.putString("cityid",cityid);
                    editor.commit();

                    if(getActivity() instanceof MainActivity){

                        Intent intent = new Intent(getActivity(),WeatherActivity.class);
                        intent.putExtra("cityid",cityid);
                        startActivity(intent);
                        getActivity().finish();

                    }else if(getActivity() instanceof WeatherActivity){

                        WeatherActivity weatherActivity = (WeatherActivity) getActivity();
                        weatherActivity.drawerlayout.closeDrawers();
                        weatherActivity.swipe_refresh.setRefreshing(true);
                        String url = "http://guolin.tech/api/weather?cityid="+cityid+"&key=066cab6185734a03acd518652b46c773";
                        weatherActivity.initWeather(url);

                    }


                }
            }
        });

        back_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(currentLevel == LEVEL_CITY){
                    queryProvince();
                }else if(currentLevel == LEVEL_COUNTY){
                    queryCities();
                }
            }
        });

    }

    /**
     * 查询所有的省，如果没有就去服务器上查询
     */
    private void queryProvince(){
        back_button.setVisibility(View.GONE);
        title_text.setText("中国");
        provinceList = findAll(Province.class);

        if(provinceList.size()>0){
            dataList.clear();
            //表示数据库里面存在数据
            for(Province province:provinceList){
                dataList.add(province.getProvinceName());
            }
            adapter.notifyDataSetChanged();
            currentLevel = LEVEL_PROVINCE;
        }else{
            queryFromService("http://guolin.tech/api/china/","province");
        }
    }

    /**
     * 查询所有的城市，如果没有就去服务器上查询
     */
    private void queryCities(){
        back_button.setVisibility(View.VISIBLE);
        title_text.setText(selectProvince.getProvinceName());
        cityList = DataSupport.where("provinceid=?",""+selectProvince.getProvinceCode()).find(City.class);

        if(cityList.size()>0){
            dataList.clear();
            for(City city:cityList){
                dataList.add(city.getCityName());
            }
            adapter.notifyDataSetChanged();
            currentLevel = LEVEL_CITY;
        }else{
            String address = "http://guolin.tech/api/china/"+selectProvince.getProvinceCode();
            queryFromService(address,"city");
        }
    }

    /**
     * 查询所有的县，如果没有就去服务器上查询
     */
    private void queryCounties(){
        back_button.setVisibility(View.VISIBLE);
        title_text.setText(selectCity.getCityName());
        countyList = DataSupport.where("cityid=?",""+selectCity.getCityCode()).find(County.class);

        if(countyList.size()>0){
            dataList.clear();
            for(County county:countyList){
                dataList.add(county.getCountyName());
            }
            adapter.notifyDataSetChanged();
            currentLevel = LEVEL_COUNTY;
        }else{
            String address = "http://guolin.tech/api/china/"+selectProvince.getProvinceCode()+
                              "/"+selectCity.getCityCode();
            queryFromService(address,"county");
        }
    }

    /**
     * 从服务器查询数据
     * @param address
     * @param type
     */
    private void queryFromService(String address,final String type){
        showProgressDialog();

        HttpUtil.sendOkHttpRequest(address, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                //回到主线程 处理逻辑
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        closeProgressDialog();
                        Toast.makeText(getActivity(),"加载失败",Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                boolean result = false;
                if(type.equals("province")){

                    result = Utility.handleProvinceResponse(response.body().string());

                }else if(type.equals("city")){

                    result = Utility.handleCityResponse(response.body().string(),selectProvince.getProvinceCode());

                }else if(type.equals("county")){

                    result = Utility.handleCountyResponse(response.body().string(),selectCity.getCityCode());

                }
                //如果请求成功
                if(result){
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            closeProgressDialog();
                            if(type.equals("province")){
                                queryProvince();
                            }else if(type.equals("city")){
                                queryCities();
                            }else if(type.equals("county")){
                                queryCounties();
                            }
                        }
                    });
                }

            }
        });

    }

    /**
     * 显示 加载的 圈圈
     */
    private void showProgressDialog(){
        if(progressDialog == null){
            progressDialog = new ProgressDialog(getActivity());
            progressDialog.setMessage("正在加载...");
            progressDialog.setCanceledOnTouchOutside(false);
        }
        progressDialog.show();
    }

    /**
     * 取消 加载的 圈圈
     */
    private void closeProgressDialog(){
        if(progressDialog != null){
            progressDialog.dismiss();
        }
    }
}
