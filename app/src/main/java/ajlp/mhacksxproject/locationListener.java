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
    @Override
    public void onLocationChanged(Location location) {
        Log.d("Latitude", Double.toString(location.getLatitude()));
        Log.d("Longitude", Double.toString(location.getLongitude()));
        Log.d("Time", new Date(location.getTime()).toString());
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
