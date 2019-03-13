package comp.junchao.android.location;
import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;


public class LocationService extends IntentService {

    private ArrayList<String> PROVIDER_ARRAY;
    private boolean isDestory;
    private String locationProvider;
    private LocationManager locationManager;
    private final int MAX_DIVIDER = 600000;
    private final int MIN_DIVIDER = 15000;
    private final int PROVIDER_DISABLED = 3000;


    public static double latitude = 0d;//用于记录当前的纬度
    public static double longitude = 0d;//用于记录当前的经度



    private LocationListener gpsLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {

        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {
            getBestLocationProvider();
        }

        @Override
        public void onProviderDisabled(String provider) {
            getBestLocationProvider();
        }
    };

    private LocationListener networkLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {

        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {
             getBestLocationProvider();
        }

        @Override
        public void onProviderDisabled(String provider) {
              getBestLocationProvider();
        }

    };

    private LocationListener passiveLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

        @Override
        public void onProviderEnabled(String provider) {
            getBestLocationProvider();
        }

        @Override
        public void onProviderDisabled(String provider) {
            getBestLocationProvider();
        }
    };

    public LocationService() {
        super("LocationService");
        PROVIDER_ARRAY = new ArrayList<>();
        PROVIDER_ARRAY.add(LocationManager.GPS_PROVIDER);
        PROVIDER_ARRAY.add(LocationManager.NETWORK_PROVIDER);
        PROVIDER_ARRAY.add(LocationManager.PASSIVE_PROVIDER);
        isDestory = false;
    }


    private synchronized void getBestLocationProvider() {
        if (locationManager == null) {
            locationProvider = null;
            return;
        }
        List<String> providers = locationManager.getAllProviders();
        //遍历所有的传感器
        for (String provider:providers) {
            Toast.makeText(this, "", Toast.LENGTH_SHORT).show();
        }
        if (providers == null || providers.size() <= 0) {
            locationProvider = null;
            return;
        }

        String bestProvider = null;
        Location bestLocation = null;//android原生location对象
        for (String provider : providers) {
            if ((provider != null) && (PROVIDER_ARRAY.contains(provider))) {
                Location location = locationManager.getLastKnownLocation(provider);
                if (location == null) {
                    continue;
                }
                //筛选出最精准的传感器
                if (bestLocation == null) {
                    bestLocation = location;
                    bestProvider = provider;
                    continue;
                }
                if (Float.valueOf(location.getAccuracy()).compareTo(bestLocation.getAccuracy()) >= 0) {
                    bestLocation = location;
                    bestProvider = provider;
                }
            }
        }
        locationProvider = bestProvider;
    }

    /**
     * 给locationManager实例化
     * @param intent
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        locationProvider = null;
        locationManager = null;

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (locationManager == null) {
            return;
        }

        List<String> allProviders = locationManager.getAllProviders();
        if (allProviders != null) {
            for (String provider : allProviders) {
                Log.i("Alex","全部传感器列表："+provider);
                if ((provider != null) && (PROVIDER_ARRAY.contains(provider))) {
                    if (LocationManager.GPS_PROVIDER.equals(provider)) {
                        Log.i("gps","正在使用gps传感器");
                        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10000, 0, gpsLocationListener);
                    } else if (LocationManager.NETWORK_PROVIDER.equals(provider)) {
                        Log.i("gps","正在使用流基站感器");
                        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 10000, 0, networkLocationListener);
                    } else if (LocationManager.PASSIVE_PROVIDER.equals(provider)) {
                        Log.i("gps","正在使用被动传感器");
                        locationManager.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER, 10000, 0, passiveLocationListener);
                    }
                }
            }
        }

        while (!isDestory) {
            getBestLocationProvider();

            updateLocation();

            if ((locationProvider != null) && (PROVIDER_ARRAY.contains(locationProvider))) {
                try {
                    if ((compare(latitude, 0.0) == 0)//看看现在有没有具体的定位，并让传感器休息一段时间
                            && (compare(longitude(), 0.0) == 0)) {
                        Thread.sleep(MIN_DIVIDER);
                    } else {
                        Thread.sleep(MAX_DIVIDER);
                    }
                } catch (InterruptedException ex) {
                }
            } else {
                try {
                    Thread.sleep(PROVIDER_DISABLED);
                } catch (Exception ex) {
                }
            }
        }
    }

    private void updateLocation() {
        if ((locationProvider != null) && (!locationProvider.equals("")) && (PROVIDER_ARRAY.contains(locationProvider))) {
            try {
                Location currentLocation = locationManager.getLastKnownLocation(locationProvider);//获取最新位置
                if (currentLocation != null) {
                    //获取经纬度
                    final double newLatitude = currentLocation.getLatitude();
                    final double newLongitude = currentLocation.getLongitude();
                    latitude = newLatitude;
                    longitude = newLongitude;
                    Log.i("gps","当前纬度是:"+newLatitude+"当前经度是："+newLongitude);


                }
            } catch (Exception ex) {
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isDestory = true;

        if ((locationManager != null) && (gpsLocationListener != null)) {
            locationManager.removeUpdates(gpsLocationListener);
        }

        if ((locationManager != null) && (networkLocationListener != null)) {
            locationManager.removeUpdates(networkLocationListener);
        }

        if ((locationManager != null) && (passiveLocationListener != null)) {
            locationManager.removeUpdates(passiveLocationListener);
        }
    }

    /**
     * 用于对传入的数据进行限幅操作
     * @param left
     * @param right
     * @return
     */
    public static int compare(double left, double right) {
        if (Double.isInfinite(left) && Double.isInfinite(right)) {
            return 0;
        }
        if (Double.isInfinite(left)) {
            return 1;
        }
        if (Double.isInfinite(right)) {
            return -1;
        }

        if (Double.isNaN(left) && Double.isNaN(right)) {
            return 0;
        }
        if (Double.isNaN(left)) {
            return -1;
        }
        if (Double.isNaN(right)) {
            return 1;
        }

        BigDecimal val1 = new BigDecimal(left);
        BigDecimal val2 = new BigDecimal(right);
        int result = -2;
        if (val1.compareTo(val2) < 0) {
            result = -1;
        } else if (val1.compareTo(val2) == 0) {
            result = 0;
        } else if (val1.compareTo(val2) > 0) {
            result = 1;
        }
        return result;
    }
}

