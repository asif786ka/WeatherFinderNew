package weathermodel;

public class CurrentWeatherModel {



    private String mWeatherDescription;
    private double mTemperature;
    private int mHumidity;
    private double mPrecipChance;
    private String mWeatherIconUrl;

    public CurrentWeatherModel() {

    }

    public CurrentWeatherModel(String weatherDescription, double temperature, int humidity, double precipChance, String weatherIconUrl) {
        mWeatherDescription = weatherDescription;
        mTemperature = temperature;
        mHumidity = humidity;
        mPrecipChance = precipChance;
        mWeatherIconUrl = weatherIconUrl;
    }

    public double getTemperature() {
        return mTemperature;
    }

    public void setTemperature(double temperature) {
        mTemperature = temperature;
    }

    public String getWeatherDescription() {
        return mWeatherDescription;
    }

    public void setWeatherDescription(String weatherDescription) {
        mWeatherDescription = weatherDescription;
    }

    public double getHumidity() {
        return mHumidity;
    }

    public void setHumidity(int humidity) {
        mHumidity = humidity;
    }

    public double getPrecipChance() {
        return mPrecipChance;
    }

    public void setPrecipChance(double precipChance) {
        mPrecipChance = precipChance;
    }

    public String getWeatherIconUrl() {
        return mWeatherIconUrl;
    }

    public void setWeatherIconUrl(String weatherIconUrl) {
        mWeatherIconUrl = weatherIconUrl;
    }



}
