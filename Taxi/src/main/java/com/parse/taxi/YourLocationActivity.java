package com.parse.taxi;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.parse.FindCallback;
import com.parse.ParseACL;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.util.ArrayList;
import java.util.List;

public class YourLocationActivity extends FragmentActivity implements OnMapReadyCallback, LocationListener
{
    private GoogleMap mMap;
    private LocationManager locationManager;
    private String provider;
    private Button requestTaxiButton;
    private TextView infoTextView;
    private Boolean requestActive = false;
    private String driverUsername="";
    private ParseGeoPoint driverLocation=new ParseGeoPoint(0,0);
    private Handler handler=new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_your_location);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        requestTaxiButton = (Button) findViewById(R.id.requestTaxiButton);
        infoTextView = (TextView) findViewById(R.id.infoTextView);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        provider = locationManager.getBestProvider(new Criteria(), false);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            return;
        }

        locationManager.requestLocationUpdates(provider, 400, 1, this);
        Location location=locationManager.getLastKnownLocation(provider);

        if(location!=null)
        {
            updateLocation(location);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap)
    {
        mMap = googleMap;
    }

    @Override
    public void onLocationChanged(Location location)
    {
        mMap.clear();
        updateLocation(location);
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
    protected void onResume()
    {
        super.onResume();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            return;
        }
        locationManager.requestLocationUpdates(provider, 400, 1, this);

    }

    @Override
    protected void onPause()
    {
        super.onPause();
        locationManager.removeUpdates(this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            return;
        }
    }

    public void requestTaxi(View view)
    {
        if(requestActive==false)
        {
            Log.i("AppInfo", "Request Taxi");
            ParseObject request=new ParseObject("Requests");
            request.put("requesterUsername", ParseUser.getCurrentUser().getUsername());
            ParseACL acl=new ParseACL();
            acl.setPublicWriteAccess(true);
            acl.setPublicReadAccess(true);
            request.setACL(acl);
            request.saveInBackground(new SaveCallback()
            {
                @Override
                public void done(ParseException e)
                {
                    if(e==null)
                    {
                        infoTextView.setText("Finding Taxi Drivers...");
                        requestTaxiButton.setText("Cancel Call");
                        requestActive=true;
                    }
                }
            });
        }
        else
        {
            infoTextView.setText("Taxi Cancelled");
            requestTaxiButton.setText("Call A Taxi");
            requestActive=false;

            ParseQuery<ParseObject> query=new ParseQuery<ParseObject>("Requests");
            query.whereEqualTo("requesterUsername", ParseUser.getCurrentUser().getUsername());
            query.findInBackground(new FindCallback<ParseObject>()
            {
                @Override
                public void done(List<ParseObject> objects, ParseException e)
                {
                    if(e==null)
                    {
                        if(objects.size()>0)
                        {
                            for (ParseObject object:objects)
                            {
                                object.deleteInBackground();
                            }
                        }
                    }
                }
            });
        }
    }

    private void updateLocation(final Location location)
    {
        mMap.clear();
        if(requestActive==false)
        {
            ParseQuery<ParseObject> query=ParseQuery.getQuery("Requests");
            query.whereEqualTo("requesterUsername", ParseUser.getCurrentUser().getUsername());
            query.findInBackground(new FindCallback<ParseObject>()
            {
                @Override
                public void done(List<ParseObject> objects, ParseException e)
                {
                    if(e==null)
                    {
                        if(objects.size()>0)
                        {
                            for(ParseObject object:objects)
                            {
                                requestActive=true;
                                infoTextView.setText("Finding Taxi Drivers...");
                                requestTaxiButton.setText("Cancel Call");
                                if(object.get("driverUsername")!=null)
                                {
                                    driverUsername=object.getString("driverUsername");
                                    infoTextView.setText("Your Taxi is on the way.");
                                    requestTaxiButton.setVisibility(View.INVISIBLE);
                                }
                            }
                        }
                    }
                }
            });
        }

        if(driverUsername.equals(""))
        {
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), 10));
            mMap.addMarker(new MarkerOptions().position(new LatLng(location.getLatitude(), location.getLatitude())).title("Your Location"));
        }

        if(requestActive==true)
        {
            if(!driverUsername.equals(""))
            {
                ParseQuery<ParseUser> userQuery=ParseUser.getQuery();
                userQuery.whereEqualTo("username", driverUsername);
                userQuery.findInBackground(new FindCallback<ParseUser>()
                {
                    @Override
                    public void done(List<ParseUser> objects, ParseException e)
                    {
                        if(e==null)
                        {
                            if(objects.size()>0)
                            {
                                for(ParseUser driver:objects)
                                {
                                    driverLocation=driver.getParseGeoPoint("location");
                                }
                            }
                        }
                    }
                });
                if(driverLocation.getLatitude()!=0 && driverLocation.getLongitude()!=0)
                {
                    Double distanceInKm = driverLocation.distanceInKilometersTo(new ParseGeoPoint(location.getLatitude(), location.getLongitude()));
                    Double distanceOneDP = (double) Math.round(distanceInKm * 10) / 10;
                    infoTextView.setText("You Taxi is "+distanceOneDP.toString()+" km away.");
                    LatLngBounds.Builder builder = new LatLngBounds.Builder();
                    ArrayList<Marker> markers = new ArrayList<Marker>();
                    markers.add(mMap.addMarker(new MarkerOptions().position(new LatLng(driverLocation.getLatitude(), driverLocation.getLongitude())).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)).title("Driver Location")));
                    markers.add(mMap.addMarker(new MarkerOptions().position(new LatLng(location.getLatitude(), location.getLongitude())).title("Your Location")));
                    for (Marker marker : markers)
                    {
                        builder.include(marker.getPosition());
                    }
                    LatLngBounds bounds = builder.build();
                    int padding = 100;
                    CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, padding);
                    mMap.animateCamera(cu);
                }
            }
            final ParseGeoPoint userLocation = new ParseGeoPoint(location.getLatitude(), location.getLatitude());
            ParseQuery<ParseObject> query=new ParseQuery<ParseObject>("Requests");
            query.whereEqualTo("requesterUsername", ParseUser.getCurrentUser().getUsername());
            query.findInBackground(new FindCallback<ParseObject>()
            {
                @Override
                public void done(List<ParseObject> objects, ParseException e)
                {
                    if (e == null)
                    {
                        if (objects.size() > 0)
                        {
                            for (ParseObject object : objects)
                            {
                                object.put("requesterLocation", userLocation);
                                object.saveInBackground();
                            }
                        }
                    }
                }
            });
        }
        handler.postDelayed(new Runnable()
        {
            @Override
            public void run()
            {
                updateLocation(location);
            }
        }, 5000);
    }
}
