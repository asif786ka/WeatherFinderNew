package com.stormy.weatherfinder;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Cache;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.NetworkImageView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import app.WeatherController;
import io.nlopez.smartlocation.OnLocationUpdatedListener;
import io.nlopez.smartlocation.SmartLocation;
import io.nlopez.smartlocation.location.config.LocationParams;
import io.nlopez.smartlocation.location.providers.LocationGooglePlayServicesWithFallbackProvider;
import util.WeatherConstants;
import weathermodel.CurrentWeatherModel;
import weathermodel.Day;
import weathermodel.Hour;

public class WeatherMainActivity extends AppCompatActivity implements OnClickListener {

    // Log tag
    private static final String TAG = WeatherMainActivity.class.getSimpleName();
    public static final String DAILY_FORECAST = "DAILY_FORECAST";
    public static final String HOURLY_FORECAST = "HOURLY_FORECAST";
    public static final String LOCATION_NAME = "LOCATION_NAME";
    private CurrentWeatherModel weatherModel;
    ImageLoader imageLoader = WeatherController.getInstance().getImageLoader();

    private TextView mSummaryLabel;
    private TextView mPrecipValue;
    private TextView mHumidityValue;

    private TextView mTemperatureLabel;
    private TextView mLocationLabel;
    private Button mHourlyButton;
    private NetworkImageView mIconimageView;
    private double mLatitude = 45.5132;
    private double mLongitude = -122.6711;
    private String mLocationName;
    private Hour[] mHours;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather_main);

        mSummaryLabel = (TextView) findViewById(R.id.summaryLabel);
        mPrecipValue = (TextView) findViewById(R.id.precipValue);
        mHumidityValue = (TextView) findViewById(R.id.humidityValue);
        mTemperatureLabel = (TextView) findViewById(R.id.temperatureLabel);
        mLocationLabel = (TextView) findViewById(R.id.locationLabel);
        mHourlyButton = (Button) findViewById(R.id.hourlyButton);
        mHourlyButton.setOnClickListener(this);
        mIconimageView = (NetworkImageView) findViewById(R.id.iconImageView);


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_weather_main, menu);
        MenuItem searchItem = menu.findItem(R.id.menu_item_search);
        final SearchView searchView = (SearchView) searchItem.getActionView();

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                Log.d(TAG, "QueryTextSubmit: " + s);
                if (isNetworkAvailable()) {
                    getWeatherData(s);
                    updateDisplay(s);
                    searchView.clearFocus();

                } else {
                    alertUserAboutError("Cannot get data,no network");
                }
                return true;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                Log.d(TAG, "QueryTextChange: " + s);
                return false;
            }
        });

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();

        //start location service
        SmartLocation
                .with(this)
                .location()
                .provider(new LocationGooglePlayServicesWithFallbackProvider(this))
                .config(LocationParams.LAZY)
                .start(new OnLocationUpdatedListener() {
                           @Override
                           public void onLocationUpdated(Location location) {
                               mLatitude = location.getLatitude();
                               mLongitude = location.getLongitude();
                               mLocationName = getLocationName(mLatitude, mLongitude);
                               if (mLocationName != null) {
                                   if (isNetworkAvailable()) {
                                       getWeatherData(mLocationName);


                                   } else {
                                       alertUserAboutError("Cannot get data,no network");
                                   }
                               } else {
                                   getWeatherData("Chicago");
                                   updateDisplay("Chicago");
                                   alertUserAboutError("This is default location,current location not detected.");
                               }

                           }
                       }

                );


    }

    @Override
    protected void onPause() {
        super.onPause();
        SmartLocation.with(this).location().stop();
    }

    /**
     * Parsing json reponse and passing the data to reddit adapter
     */

    private void parseJsonFeed(JSONObject response) {
        try {
            JSONObject weatherObj = response.getJSONObject("data");
            JSONArray weatherChildArray = weatherObj.getJSONArray("current_condition");
            JSONArray weatherChildHourlyArray = weatherObj.getJSONArray("weather").getJSONObject(2).getJSONArray("hourly");

            for (int i = 0; i < weatherChildArray.length(); i++) {
                JSONObject weatherChildObj = (JSONObject) weatherChildArray
                        .get(i);

                weatherModel = new CurrentWeatherModel();

                weatherModel.setWeatherDescription(weatherChildObj.getJSONArray("weatherDesc").getJSONObject(0).getString("value"));

                // Image might be null sometimes
                String image = weatherChildObj.getJSONArray("weatherIconUrl").getJSONObject(0).getString("value");
                weatherModel.setWeatherIconUrl(image);

                weatherModel.setHumidity(weatherChildObj.getInt("humidity"));
                weatherModel.setPrecipChance(weatherChildObj.getDouble("precipMM"));
                weatherModel.setTemperature(weatherChildObj.getDouble("temp_C"));


            }

            mHours = new Hour[weatherChildHourlyArray.length()];
            for (int i = 0; i < weatherChildHourlyArray.length(); i++) {
                JSONObject jsonHour = weatherChildHourlyArray.getJSONObject(i);
                Hour hour = new Hour();
                hour.setSummary(jsonHour.getJSONArray("weatherDesc").getJSONObject(0).getString("value"));
                hour.setTime(jsonHour.getString("time"));
                String image = jsonHour.getJSONArray("weatherIconUrl").getJSONObject(0).getString("value");
                hour.setIcon(image);
                hour.setTemperature(jsonHour.getDouble("tempC"));

                mHours[i] = hour;
            }

            //This code is commented as the api sends an hourly array but not a daily array.
        /*    Day[] days = new Day[data.length()];
            for (int i = 0; i < data.length(); i++) {
                JSONObject jsonDay = data.getJSONObject(i);
                Day day = new Day();
                day.setSummary(jsonDay.getString("summary"));
                day.setTime(jsonDay.getLong("time"));
                day.setIcon(jsonDay.getString("icon"));
                day.setTemperatureMax(jsonDay.getDouble("temperatureMax"));


                days[i] = day;
            }*/

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    public void updateDisplay(String locationName) {
        mTemperatureLabel.setText(weatherModel.getTemperature() + "");
        mHumidityValue.setText(weatherModel.getHumidity() + "");
        mPrecipValue.setText(weatherModel.getPrecipChance() + "");
        mSummaryLabel.setText(weatherModel.getWeatherDescription());
        mIconimageView.setImageUrl(
                weatherModel.getWeatherIconUrl(), imageLoader);
        mLocationLabel.setText(locationName);
    }


    private String getFormattedUrl(String location) {

        Uri builtUri = Uri.parse(WeatherConstants.WEATHER_BASE_URL)
                .buildUpon()
                .appendQueryParameter(WeatherConstants.QUERY_PARAM, location)
                .appendQueryParameter(WeatherConstants.FORMAT_PARAM, "json")
                .appendQueryParameter(WeatherConstants.DAYS_PARAM, "5")
                .appendQueryParameter(WeatherConstants.KEY_PARAM, WeatherConstants.WEATHER_KEY)
                .build();
        String formattedUrl = builtUri.toString();
        return formattedUrl;

    }

    public void getWeatherData(final String location) {
        // We first check for cached request
        Cache cache = WeatherController.getInstance().getRequestQueue().getCache();
        Cache.Entry entry = cache.get(getFormattedUrl(location));
        if (entry != null) {
            // fetch the data from cache
            try {
                String data = new String(entry.data, "UTF-8");
                try {
                    parseJsonFeed(new JSONObject(data));
                    updateDisplay(location);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

        } else {

            // making fresh volley request and getting json
            JsonObjectRequest jsonReq = new JsonObjectRequest(Request.Method.GET,
                    getFormattedUrl(location), null, new Response.Listener<JSONObject>() {

                @Override
                public void onResponse(JSONObject response) {

                    VolleyLog.d(TAG, "Response: " + response.toString());
                    if (response != null) {
                        parseJsonFeed(response);
                        updateDisplay(location);
                    }

                }
            }, new Response.ErrorListener() {

                @Override
                public void onErrorResponse(VolleyError error) {

                    VolleyLog.d(TAG, "Error: " + error.getMessage());
                    alertUserAboutError("Bad response from Server");
                }

            });

            // Adding request to volley request queue
            WeatherController.getInstance().addToRequestQueue(jsonReq);

        }
    }

    public void alertUserAboutError(String error) {
        AlertDialogFragment dialog = new AlertDialogFragment();
        dialog.show(getFragmentManager(), error);
    }

    /**
     * Get the name of the city at the given map coordinates.
     *
     * @param latitude  Latitude of the location.
     * @param longitude Longitude of the location.
     * @return The localized name of the city.  If a geocoder isn't implemented on the device,
     * returns "Not Available". If the geocoder is implemented but fails to get an address,
     * returns "Not Found".
     */
    public String getLocationName(double latitude, double longitude) {

        String cityName = "Not Found";
        if (Geocoder.isPresent()) {
            Geocoder geocoder = new Geocoder(getBaseContext(), Locale.getDefault());
            try {
                List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);

                if (addresses.size() > 0) {
                    Address address = addresses.get(0);
                    cityName = address.getLocality(); // + ", " + address.getAdminArea();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            cityName = "Not Available";
        }
        return cityName;
    }

    public boolean isNetworkAvailable() {
        ConnectivityManager manager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = manager.getActiveNetworkInfo();
        boolean isAvailable = false;
        if (networkInfo != null && networkInfo.isConnected()) {
            isAvailable = true;
        }
        return isAvailable;
    }


    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.hourlyButton:
                if (mHours != null) {
                    Intent intent = new Intent(this, HourlyForecastActivity.class);
                    intent.putExtra(HOURLY_FORECAST, mHours);
                    startActivity(intent);
                }
                break;
        }
    }
}
