package hblj.blueweather.util;

import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import hblj.blueweather.db.City;
import hblj.blueweather.db.County;
import hblj.blueweather.db.Province;

/**
 * Created by shijin on 2017/1/28.
 */

public class Utility {

    /**
     * 解析和处理服务器返回的省级数据
     */
    public static boolean handleProvinceResponse(String response){
        if(TextUtils.isEmpty(response)){
            return false;
        }

        try {
            JSONArray array = new JSONArray(response);
            for(int i = 0;i<array.length();i++){
                JSONObject obj = array.getJSONObject(i);
                int provinceCode = obj.getInt("id");
                String provinceName = obj.getString("name");

                Province province = new Province();
                province.setProvinceCode(provinceCode);
                province.setProvinceName(provinceName);

                //存储到数据库
                province.save();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return true;
    }

    /**
     * 解析和处理服务器返回的市级数据
     */
    public static boolean handleCityResponse(String response,int provinceId){
        if(TextUtils.isEmpty(response)){
            return false;
        }

        try {
            JSONArray array = new JSONArray(response);
            for(int i = 0;i<array.length();i++){
                JSONObject obj = array.getJSONObject(i);

                String cityName = obj.getString("name");
                int cityCode = obj.getInt("id");

                City city = new City();
                city.setCityCode(cityCode);
                city.setCityName(cityName);
                city.setProvinceId(provinceId);

                city.save();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return true;
    }

    /**
     * 解析和处理服务器返回的县级数据
     */
    public static boolean handleCountyResponse(String response,int cityId){
        if(TextUtils.isEmpty(response)){
            return false;
        }

        try {
            JSONArray array = new JSONArray(response);
            for(int i = 0;i<array.length();i++){
                JSONObject obj = array.getJSONObject(i);

                int countyId = obj.getInt("id");
                String countyName = obj.getString("name");
                String weatherId = obj.getString("weather_id");

                County county = new County();
                county.setCityId(cityId);
                county.setCountyId(countyId);
                county.setCountyName(countyName);
                county.setWeatherId(weatherId);

                county.save();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return true;
    }
}
