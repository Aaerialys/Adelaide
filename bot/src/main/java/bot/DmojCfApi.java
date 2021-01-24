package bot;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class DmojCfApi { //connects to the dmoj and codeforces api and caches all available problems
    private static final int DMOJLIMIT = 90, DMOJTIME = 60000; //dmoj api calls are limited to 90 calls/60000m(1 minute)
    private static DmojQueue dmojPrev = new DmojQueue(); //a queue of the times of previous dmoj api calls
    private static JSONArray dmojProblemCache, cfProblemCache; //problem caches for dmoj and codeforces
    
    public static JSONObject query(String str) throws IOException, ParseException, InterruptedException { //queries api endpoint "str" and returns the json object recieved
        if (str.contains("dmoj.")) { //if the api is from dmoj, ensure it does not exceed the rate limit
            while (dmojPrev.size() >= DMOJLIMIT) {
                if (dmojPrev.front() >= new Date().getTime() - DMOJTIME) { //if there are more than 90 calls in the last minute, wait until a minute has passed since the oldest call
                    Thread.sleep(dmojPrev.front() - (new Date().getTime() - DMOJTIME));
                }
                dmojPrev.pop(); //remove the oldest api call from the queue
            }
            long time = new Date().getTime(); //add the current time for the new api call to the queue
            dmojPrev.push(time);
        }
        URL url = new URL(str); //converts the link from string to url object
        HttpURLConnection conn = (HttpURLConnection) url.openConnection(); //connects to url
        JSONParser parse = new JSONParser();
        InputStream stream = conn.getInputStream(); //gets api data to input stream
        return (JSONObject) parse.parse(new InputStreamReader(stream)); //parses stream as a json object and returns it
    }
    
    public static void updateCache() { //updates the dmoj and codeforces problem caches
        try {
            JSONObject temp;
            dmojProblemCache = new JSONArray(); //resets dmoj cache
            int page = 1;
            do { //look through each page of dmoj problems
                temp = (JSONObject) query("https://dmoj.ca/api/v2/problems?page=" + page).get("data"); //gets all problems on that page and adds it to cache
                dmojProblemCache.addAll((JSONArray)(temp).get("objects"));
                System.out.println("loading cache " + page + "/5"); //prints progress
                page++;
            } while ((boolean) temp.get("has_more"));
            cfProblemCache = (JSONArray)((JSONObject) query("https://codeforces.com/api/problemset.problems").get("result")).get("problems"); //caches all codeforces problems
        } catch (Exception e) { //exit if an error occurs while updating cache
            System.err.println("API error while updating cache");
            e.printStackTrace();
        }
    }
    
    public static ArrayList < JSONObject > getDmojProblems() { //returns cache of dmoj problems
        return new ArrayList < JSONObject > (dmojProblemCache);
    }
    
    public static ArrayList < JSONObject > getCfProblems() { //returns cache of codeforces problems
        return new ArrayList < JSONObject > (cfProblemCache);
    }
    
    public static JSONObject dmojProblemInfo(String problem) { //searches for info about a given dmoj problem in the cache
        int binarySearch = 1, pos = -1;
        while (binarySearch <= dmojProblemCache.size()) binarySearch *= 2;
        for (; binarySearch > 0; binarySearch /= 2) { //use binary search to find the problem in the cache, which is sorted in alphabetical order by problem code
            if (pos + binarySearch >= dmojProblemCache.size()) continue;
            int cmp = problem.compareTo((String)((JSONObject) dmojProblemCache.get(pos + binarySearch)).get("code"));
            if (cmp == 0) return (JSONObject)((JSONObject) dmojProblemCache.get(pos + binarySearch)); //return the element if it matches the problem being searched for
            if (cmp > 0) pos += binarySearch;
        }
        return null; //if the problem has not been found, return null
    }
    
    public static JSONObject cfProblemInfo(int contest, String index) { //searches for codeforces problem info about a problem given its contest and index
        int binarySearch = 1, pos = -1;
        while (binarySearch <= cfProblemCache.size()) binarySearch *= 2;
        for (; binarySearch > 0; binarySearch /= 2) { //uses binary search on the cache which is sorted in descending contest number and index
            if (pos + binarySearch >= cfProblemCache.size()) continue;
            int cmp = index.compareTo((String)((JSONObject) cfProblemCache.get(pos + binarySearch)).get("index"));
            Long id = (Long)((JSONObject) cfProblemCache.get(pos + binarySearch)).get("contestId");
            if (cmp == 0 && contest == id) return (JSONObject)((JSONObject) cfProblemCache.get(pos + binarySearch)); //return the element if it matches
            if (contest < id || id == contest && cmp < 0) pos += binarySearch;
        }
        return null; //if the problem has not been found, return null
    }
}