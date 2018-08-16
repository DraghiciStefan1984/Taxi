package com.parse.taxi;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.List;

public class ViewRequestsActivity extends AppCompatActivity implements LocationListener
{
    private ListView listView;
    private ArrayList<String> listViewContent;
    private ArrayList<String> usernames;
    private ArrayList<Double> latitudes;
    private ArrayList<Double> longitudes;
    private ArrayAdapter arrayAdapter;
    private LocationManager locationManager;
    private String provider;
    private Location location;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_requests);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        listView = (ListView) findViewById(R.id.listView);
        listViewContent = new ArrayList<>();
        usernames = new ArrayList<>();
        latitudes = new ArrayList<>();
        longitudes = new ArrayList<>();
        listViewContent.add("Finding nerby usernames...");
        arrayAdapter = new ArrayAdapter<String>(ViewRequestsActivity.this, android.R.layout.simple_list_item_1, listViewContent);
        listView.setAdapter(arrayAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                Intent i = new Intent(ViewRequestsActivity.this, ViewRiderLocationActivity.class);
                i.putExtra("username", usernames.get(position));
                i.putExtra("latitude", latitudes.get(position));
                i.putExtra("longitude", longitudes.get(position));
                i.putExtra("userLatitude", location.getLatitude());
                i.putExtra("userLongitude", location.getLongitude());
                startActivity(i);
            }
        });
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        provider = locationManager.getBestProvider(new Criteria(), false);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            return;
        }

        locationManager.requestLocationUpdates(provider, 400, 1, this);
        location = locationManager.getLastKnownLocation(provider);
        if (location != null)
        {
            updateLocation();
        }
    }

    @Override
    public void onLocationChanged(Location location)
    {
        updateLocation();
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras)
    {

    }

    @Override
    public void onProviderEnabled(String provider)
    {

    }

    @Override
    public void onProviderDisabled(String provider)
    {

    }

    @Override
    protected void onPause()
    {
       super.onPause();
//        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
//        {
//            return;
//        }
//        locationManager.removeUpdates(this);
    }

    public void updateLocation()
    {
        final ParseGeoPoint userLocation = new ParseGeoPoint(location.getLatitude(), location.getLatitude());
        ParseUser.getCurrentUser().put("location", userLocation);
        ParseUser.getCurrentUser().saveInBackground();

        ParseQuery<ParseObject> query = ParseQuery.getQuery("Requests");
        query.whereNear("requesterLocation", userLocation);
        query.setLimit(100);
        query.findInBackground(new FindCallback<ParseObject>()
        {
            @Override
            public void done(List<ParseObject> objects, ParseException e)
            {
                if (e == null)
                {
                    if (objects.size() > 0)
                    {
                        listViewContent.clear();
                        usernames.clear();
                        latitudes.clear();
                        longitudes.clear();

                        for (ParseObject object : objects)
                        {
                            if (object.get("driverUsename") == null)
                            {
                                Double distanceInKm = userLocation.distanceInKilometersTo((ParseGeoPoint) object.get("requesterLocation"));
                                Double distanceOneDP = (double) Math.round(distanceInKm * 10) / 10;
                                listViewContent.add(distanceOneDP.toString() + " km");
                                usernames.add(object.getString("requesterUsername"));
                                latitudes.add(object.getParseGeoPoint("requesterLocation").getLatitude());
                                longitudes.add(object.getParseGeoPoint("requesterLocation").getLongitude());
                            }
                        }
                        arrayAdapter.notifyDataSetChanged();
                    }
                }
            }
        });
    }
}
