package ajlp.mhacksxproject;

import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.util.Log;

import java.util.Date;

/**
 * Created by jborromeo on 2017-09-23.
 */

public class locationListener implements LocationListener {
    public double latitude=0;
    public double longitude=0;
    public double speed=0;

    @Override
    public void onLocationChanged(Location location) {
        Log.d("Latitude", Double.toString(location.getLatitude()));
        Log.d("Longitude", Double.toString(location.getLongitude()));
        Log.d("Time", new Date(location.getTime()).toString());
        latitude = location.getLatitude();
        longitude = location.getLongitude();
        speed = location.getTime();
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

    }
}
