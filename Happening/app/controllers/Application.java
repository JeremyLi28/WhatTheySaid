package controllers;

import org.json.JSONException;
import play.*;
import play.libs.Json;
import play.mvc.*;
import play.mvc.WebSocket;
import akka.actor.*;
import views.html.*;
import actors.*;
import java.io.Serializable;
import com.fasterxml.jackson.databind.JsonNode;
import javax.inject.Inject;
import play.mvc.*;
import play.libs.ws.*;
import play.libs.F.Function;
import play.libs.F.Promise;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import static java.util.stream.Collectors.toList;
import static utils.Streams.stream;
import java.util.*;

import twitter4j.conf.ConfigurationBuilder;
import twitter4j.auth.OAuthAuthorization;
import twitter4j.TwitterFactory;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.Query;
import twitter4j.GeoLocation;
import twitter4j.QueryResult;
import twitter4j.Status;

import org.json.JSONObject;








public class Application extends Controller {


    private static Twitter twitter;

    private static final Set<String> posWords = new HashSet<String>();
    private static final Set<String> negWords = new HashSet<String>();
    private static final Set<String> stopWords = new HashSet<String>();
    private static final Set<Long> tweetId = new HashSet<>();

    public static class fetchTweetsMessage implements Serializable {}

    public Result index() {
        return ok(index.render("Happening"));
    }

    public WebSocket<String> ws() {

        return WebSocket.withActor(UserActor::props);
    }

//    public static Promise<JsonNode> fetchTweets(String place) throws TwitterException, JSONException{
//        fetchTweet();
//        Promise<WSResponse> responsePromise = WS.url("http://twitter-search-proxy.herokuapp.com/search/tweets").setQueryParameter("q", place).get();
//        return responsePromise
//                .filter(response -> response.getStatus() == Http.Status.OK)
//                .map(response -> filterTweetsWithLocation(response.asJson()))
//                .recover(Application::errorResponse);
//    }
//
//    private static JsonNode filterTweetsWithLocation(JsonNode jsonNode) {
////create a stream view of the jsonNode iterator
//        List<JsonNode> newJsonList = stream(jsonNode.findPath("statuses"))
//                //map the stream of json to update the values to have the geo-info
//                .map(json -> setCoordinates((ObjectNode) json))
//                .collect(toList());
//
//        ObjectNode objectNode = JsonNodeFactory.instance.objectNode();
//        objectNode.putArray("statuses").addAll(newJsonList);
//        return objectNode;
//
//    }
//
//    private static ObjectNode setCoordinates(ObjectNode nextStatus) {
//        nextStatus.putArray("coordinates").add(randomLat()).add(randomLon());
//        return nextStatus;
//    }
//
//    private static Random rand = new java.util.Random();
//
//    private static double randomLat() {
//        return (rand.nextDouble() * (-117.66820907592773+117.96175003051756)) -117.96175003051756;
//    }
//
//    private static double randomLon() {
//        return (rand.nextDouble() * (33.75174787568194 - 33.637489243170826)) + 33.637489243170826;
//
//    }
//
//    private static JsonNode errorResponse(Throwable ignored) {
//        return Json.newObject().put("error", "Could not fetch the tweets");
//    }


    public static List<JSONObject> fetchTweet() throws TwitterException, JSONException {
        setUpTwitter();

        double latitude = 40.712784;
        double longtitude =  -74.005941;
        double radius = 50;
        int count = 30;
        List<JSONObject> jsons = new ArrayList<>();
        List<twitter4j.Status> tweets = searchTwitter(latitude, longtitude, radius, count);
        for(int i=0; i<tweets.size(); i++) {
            if(tweets.get(i).getGeoLocation() != null && !tweetId.contains(tweets.get(i).getId())) {
                tweetId.add(tweets.get(i).getId());
                jsons.add(twitterToJSON(tweets.get(i)));
                System.out.println(tweets.get(i).getId() + "," + tweets.get(i).getGeoLocation().getLatitude() + "," + tweets.get(i).getGeoLocation().getLongitude());
            }
        }
        return jsons;
    }

    private static void setUpTwitter() {
        String accessToken = "734546468573347840-rkY68hk9rQ2m2CRF3ltyqYwoJshJjz2";
        String accessTokenSecret = "EkoziulDpZ8VePBEZvhBWPsApj9KA53HMvMaxUNkOAd2p";
        String consumerKey = "4yn6CjzxHwKmDyY6TJTvrpCYP";
        String consumerSecret = "gWlgiHbM1q7tL5k8LLQ7vaCDYboSZQt4EKXMkHNq1ZAZoz7Z4C";
        ConfigurationBuilder build = new ConfigurationBuilder();

        build.setOAuthAccessToken(accessToken);
        build.setOAuthAccessTokenSecret(accessTokenSecret);
        build.setOAuthConsumerKey(consumerKey);
        build.setOAuthConsumerSecret(consumerSecret);
        OAuthAuthorization auth = new OAuthAuthorization(build.build());
        twitter = new TwitterFactory().getInstance(auth);
    }

    private static List<twitter4j.Status> searchTwitter(double latitude, double longtitude, double radius, int count) throws TwitterException {
        Query query = new Query();
        query.setGeoCode(new GeoLocation(latitude, longtitude), radius, Query.MILES);
        query.setCount(count);
//        System.out.println(query.toString());

        QueryResult queryRes = twitter.search(query);
        List<twitter4j.Status> tweets = queryRes.getTweets();
//		for (Status tweet : tweets) {
//            System.out.println("@" + tweet.getUser().getScreenName() + " - " + tweet.getText());
//        }
        return tweets;
    }

    private static JSONObject twitterToJSON(twitter4j.Status twitter) throws JSONException{
        JSONObject json = new JSONObject();
        //add all fields we need
        //text
        json.append("text", twitter.getText().toLowerCase().replaceAll("[^0-9a-zA-Z\\s]+", ""));
        //geoLocation
        if (twitter.getGeoLocation() == null) {
            json.append("location", null);
        } else {
            json.append("location", Double.toString(twitter.getGeoLocation().getLatitude()));
            json.append("location", Double.toString(twitter.getGeoLocation().getLongitude()));
        }

        return json;
    }

}
