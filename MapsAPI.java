package map;


import data.Location;
import util.Singleton;
import util.Util;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * <p>A set of methods using URL-requests to the Google Maps Geocode API and Elevation API,
 * to gather information about latitude, longitude and altitude of locations, from the URLs' JSON-responses.
 * <p>Latitude, longitude and altitude are returned as decimal (double) numbers. Altitude is given as
 * <b>metres above sea leavel</b>, commonly known as <b>"MASL"</b>, which is often referenced in this segment of WADA Bloodworks' JavaDoc.</p>
 * <p>Note: The GeoCode API and the Elevation API are limited to 2500 free daily requests.</p>
 *
 * @author Eirik Ovesen, Fredrik Bakke
 */

public class MapsAPI implements Singleton {


    private static final String API_KEY = "YOUR API KEY HERE";
	
    // URL Examples:
    // https://maps.googleapis.com/maps/api/elevation/json?locations=LATITUDE,LONGITUDE&key=YOUR_API_KEY
    // https://maps.googleapis.com/maps/api/geocode/json?address=1600+Amphitheatre+Parkway,+Mountain+View,+CA&key=YOUR_API_KEY
    // "adress=trondheim" returns the first search result for Trondheim on Google Maps

    private MapsAPI(){}
    private static MapsAPI instance;

    public static MapsAPI get(){
        if (instance == null){
            instance = new MapsAPI();
        }
        return instance;
    }


	/** Ignore this function - it's part of a project we used this code for.
	
    public Location getLocation(String where) {
        if (Util.isEmpty(where)) return null;
        Double[] lla = getLatLongAlt(where);

        if (lla == null) return null;

        return new Location(where, lla);
    }
	
	*/
	

    /**
     * <p>Gets a {@code Double} containing altitude (MASL) of a location given by a {@code String}.</p>
     *
     * @param where {@code String} describing a location
     * @return {@code null} if the {@code getLatLong(Double[])} is {@code null}, otherwise {@code getAltitude(Double[])}
     */
    public Double getAltitude(String where) {
            Double[] latlong = getLatLong(where);
            if(latlong == null){
                return null;
            }
            return getAltitude(latlong);
        }

    /**
     * <p>Creates a {@code Double[]} containing latitude, longitude & altitude of a location described by a {@code String} parameter.</p>
     * <p>Note: This method uses one API-request to both GeoCode and Elevation APIs.</p>
     *
     * @param where the {@code String} describing a location
     * @return {@code null} if {@code getLatLong(String} or {@code getAltitude(Doulbe[]} is {@code null}, otherwise
     * the {@code Double[]} containing latitude followed by longitude followed by altitude (MASL)
     */
    public Double[] getLatLongAlt(String where){
            Double[] latlong = getLatLong(where);
            Double alt = getAltitude(latlong);
            if (latlong == null || alt == null){
                return null;
            }
            return new Double[]{latlong[0], latlong[1], alt};
        }

    /**
     * Creates a {@code Double[]} containing latitude followed by longitude of a location described by a {@code String} parameter.
     * <p>Note: This method uses one API-request.</p>
     *
     * @param where the {@code String} describing a location
     * @return {@code null} if the JSON-object reads "ZERO_RESULTS" (location doesn't exist in Google Maps),
     * otherwise the {@code Double[]} containing latitude follwed by longitude of the location
     */
    public Double[] getLatLong(String where) {
            URL url = createGeoCodeURL(where);
            Double[] latlong = new Double[2];

        try (InputStream is = url.openStream(); JsonReader rdr = Json.createReader(is)) {
            JsonObject obj = rdr.readObject();
            JsonArray results = obj.getJsonArray("results");

            if(obj.getJsonString("status").getString().equals("ZERO_RESULTS")){
                return null;
            }

            for (JsonObject test : results.getValuesAs(JsonObject.class)) {
                latlong[0] = test.getJsonObject("geometry").getJsonObject("location").getJsonNumber("lat").doubleValue();
                latlong[1] = test.getJsonObject("geometry").getJsonObject("location").getJsonNumber("lng").doubleValue();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

            return latlong;
        }

    /**
     * Creates a {@code Double} containing the altiude (MASL) of a location at coordinates latitude followed by longitude.
     * <p>Note: This method uses one API-request.</p>
     *
     * @param latlong the {@code Double[]} containing latitude and longitude of a location
     * @return {@code null} if the parameter is {@code null}
     * or the JSON-object reads "INVALID_REQUEST" (latitude and/or longitude coordinates are invalid),
     * otherwise the {@code Double} containing altitude (MASL) of the location.
     */

    public Double getAltitude(Double... latlong){
            if(latlong == null){
                return null;
            }

            URL url = createElevationURL(latlong);
            double altitude = Double.MAX_VALUE;


            try (InputStream is = url.openStream(); JsonReader rdr = Json.createReader(is)) {
                JsonObject obj = rdr.readObject();
                JsonArray results = obj.getJsonArray("results");
                if(obj.getJsonString("status").getString().equals("INVALID_REQUEST")){
                    return null;
                }
                for (JsonObject result : results.getValuesAs(JsonObject.class)) {
                    altitude = result.getJsonNumber("elevation").doubleValue();
                }
            }catch(Exception e) {
                e.printStackTrace();
            }
            return altitude;
        }
    /**
     * Creates an URL request to the Google GeoCode API using the {@code String} parameter describing the location.
     * <p>The URL request returns a JSON-object.</p>
     *
     * @param where the {@code String} describing the location
     * @return the {@code URL}-object containing the API request
     */
        private URL createGeoCodeURL(String where) {
            URL url = null;
            where = where.replace(" ", "+");
            try {
                url = new URL("https://maps.googleapis.com/maps/api/geocode/json?address="
                        + where + "&key=" + API_KEY);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
            return url;
        }

    /**
     * Creates an URL request to the Google Elevation API using the given latitude and longitude.
     * <p>The URL request returns a JSON Object.</p>
     *
     * @param latlong the {@code Double[]} containing latitude followed by the longitude
     * @return the {@code URL}-object containing the API-request
     */
    private URL createElevationURL(Double... latlong) {
            URL url = null;
            try{
                url = new URL("https://maps.googleapis.com/maps/api/elevation/json?locations=" + latlong[0] + "," + latlong[1] +
                        "&key=" + API_KEY);
            }catch(MalformedURLException e){
                e.printStackTrace();
            }
            return url;
        }
}

