package com.example.lbsdemo;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.BDLocation;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.search.geocode.GeoCodeOption;
import com.baidu.mapapi.search.geocode.GeoCodeResult;
import com.baidu.mapapi.search.geocode.GeoCoder;
import com.baidu.mapapi.search.geocode.OnGetGeoCoderResultListener;
import com.baidu.mapapi.search.geocode.ReverseGeoCodeResult;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    TextView locationInfo;
    LocationClient mLocationClient;
    MapView mMapView;
    BaiduMap mBaiduMap = null;
    boolean isFirstLocate = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SDKInitializer.setAgreePrivacy(getApplicationContext(), true);
        SDKInitializer.initialize(getApplicationContext());
        setContentView(R.layout.activity_main);

        EditText editText = findViewById(R.id.et_address);
        Button button = findViewById(R.id.btn_locate);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String address = editText.getText().toString();
                if (TextUtils.isEmpty(address)) {
                    Toast.makeText(MainActivity.this, "请输入地址", Toast.LENGTH_SHORT).show();
                    return;
                }

                // 根据地址获取经纬度
                GeoCoder geoCoder = GeoCoder.newInstance();
                geoCoder.setOnGetGeoCodeResultListener(new OnGetGeoCoderResultListener() {
                    @Override
                    public void onGetGeoCodeResult(GeoCodeResult geoCodeResult) {
                        if (geoCodeResult == null || geoCodeResult.getLocation() == null) {
                            Toast.makeText(MainActivity.this, "地址解析失败", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        double latitude = geoCodeResult.getLocation().latitude;
                        double longitude = geoCodeResult.getLocation().longitude;

                        // 移动地图视角到目标位置
                        LatLng latLng = new LatLng(latitude, longitude);
                        MapStatusUpdate update = MapStatusUpdateFactory.newLatLng(latLng);
                        mBaiduMap.animateMapStatus(update);
                        update = MapStatusUpdateFactory.zoomTo(16f);
                        mBaiduMap.animateMapStatus(update);
                    }

                    @Override
                    public void onGetReverseGeoCodeResult(ReverseGeoCodeResult reverseGeoCodeResult) {}
                });
                geoCoder.geocode(new GeoCodeOption().city("").address(address));
            }
        });

        locationInfo = (TextView) findViewById(R.id.locationInfo);
        try {
            LocationClient.setAgreePrivacy(true);
            mLocationClient = new LocationClient(getApplicationContext());
            mLocationClient.registerLocationListener(new MyLocationListener());
        } catch (Exception e) {
            e.printStackTrace();
        }

        mMapView = (MapView) findViewById(R.id.bmapView);
        mBaiduMap = mMapView.getMap();
        mBaiduMap.setMapType(BaiduMap.MAP_TYPE_SATELLITE);


        mBaiduMap.setMyLocationEnabled(true);


        List<String> permissionList = new ArrayList<String>();
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            permissionList.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }

        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED){
            permissionList.add(Manifest.permission.READ_PHONE_STATE);
        }

        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            permissionList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);//外部存储权限
        }

        if (!permissionList.isEmpty()){
            String [] permissions = permissionList.toArray(new String[permissionList.size()]);
            ActivityCompat.requestPermissions(MainActivity.this,permissions,1);
        }else{
            requestLocation();
        }

    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0) {
                    for (int result : grantResults) {
                        if (result != PackageManager.PERMISSION_GRANTED) {
                            Toast.makeText(this, "必须同意所有的权限才能使用本程序", Toast.LENGTH_SHORT).show();
                            finish();
                            return;
                        }
                    }
                    requestLocation();
                } else {
                    Toast.makeText(this, "发生未知错误", Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
        }
    }

    private void requestLocation(){
        initLocation();
        mLocationClient.start();
    }

    private void initLocation() {
        LocationClientOption option = new LocationClientOption();
        option.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);
        //LocationMode.Hight_Accuracy;高精度
        //LocationMode.Battery_Saving;低能耗
        //LocationMode.Device_Sensors;仅使用设备
        option.setCoorType("bd09ll");
        //GCJ02;国测局坐标
        //BD09ll;百度经纬度坐标
        //bd09;百度墨卡托坐标
        option.setScanSpan(1000);
        //发送请求间隔,int类型,单位ms
        //为0仅定位一次
        //非零需设置1000ms以上才有效
        option.setOpenGps(true);
        //设置是否使用gps,默认为false
        option.setLocationNotify(true);
        //设置是否当gps有效按照1s/1次频率输出gps
        option.setIgnoreKillProcess(false);
        option.SetIgnoreCacheException(false);
        //设置是否收集Crash信息,默认收集
        option.setWifiCacheTimeOut(5 * 60 * 1000);
        option.setEnableSimulateGps(false);
        option.setIsNeedAddress(true);
        mLocationClient.setLocOption(option);
    }

    private class MyLocationListener extends BDAbstractLocationListener {
        @Override
        public void onReceiveLocation(BDLocation location) {//BDLocation包含定位的各种信息

            navigateTo(location);
//            StringBuilder currentPosition =new StringBuilder();
//            currentPosition.append("维度:").append(location.getLatitude()).append("\n");
//            currentPosition.append("经度:").append(location.getLongitude()).append("\n");
//            currentPosition.append("国家:").append(location.getCountry()).append("\n");
//            currentPosition.append("省:").append(location.getProvince()).append("\n");
//            currentPosition.append("市:").append(location.getCity()).append("\n");
//            currentPosition.append("区:").append(location.getDistrict()).append("\n");
//            currentPosition.append("村镇:").append(location.getTown()).append("\n");
//            currentPosition.append("街道:").append(location.getStreet()).append("\n");
//            currentPosition.append("地址:").append(location.getAddrStr()).append("\n");
//            currentPosition.append("定位方式:");
//            if (location.getLocType()==BDLocation.TypeGpsLocation){
//                currentPosition.append("GPS");
//            }else if (location.getLocType()==BDLocation.TypeNetWorkLocation){
//                currentPosition.append("网络");
//            }
//            locationInfo.setText(currentPosition);
        }
    }

    private void navigateTo(BDLocation location) {
        if (isFirstLocate){
            LatLng ll=new LatLng(location.getLatitude(),location.getLongitude());
            MapStatusUpdate  update = MapStatusUpdateFactory.newLatLng(ll);
            mBaiduMap.animateMapStatus(update);
            update=MapStatusUpdateFactory.zoomTo(16f);
            mBaiduMap.animateMapStatus(update);
            isFirstLocate=false;
        }
        MyLocationData.Builder locationBuilder=new MyLocationData.Builder();
        locationBuilder.longitude(location.getLongitude());
        locationBuilder.latitude(location.getLatitude());
        MyLocationData locationData=locationBuilder.build();
        mBaiduMap.setMyLocationData(locationData);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mLocationClient.stop();
        mMapView.onDestroy();
        mBaiduMap.setMyLocationEnabled(false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mMapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mMapView.onPause();
    }
}