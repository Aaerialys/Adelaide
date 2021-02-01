package bot;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.stream.Collectors;

import javax.net.ssl.HttpsURLConnection;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class DmojCfApi { //connects to the dmoj and codeforces api and caches all available problems
    private static final int DMOJLIMIT = 29, DMOJTIME = 20000; //dmoj api calls are limited to 90 calls/60000m(1 minute)
    private static DmojQueue dmojPrev = new DmojQueue(); //a queue of the times of previous dmoj api calls
    private static ArrayList<Problem> dmojProblemCache, cfProblemCache; //problem caches for dmoj and codeforces
    
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
        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection(); //connects to url
        JSONParser parse = new JSONParser();
        InputStream stream = conn.getInputStream(); //gets api data to input stream
        return (JSONObject) parse.parse(new InputStreamReader(stream)); //parses stream as a json object and returns it
    }
    public static JSONObject query(String str,String key,String value) throws IOException, ParseException, InterruptedException { //queries api endpoint "str" and returns the json object recieved
        while (dmojPrev.size() >= DMOJLIMIT) {
            if (dmojPrev.front() >= new Date().getTime() - DMOJTIME) { //if there are more than 90 calls in the last minute, wait until a minute has passed since the oldest call
                Thread.sleep(dmojPrev.front() - (new Date().getTime() - DMOJTIME));
            }
            dmojPrev.pop(); //remove the oldest api call from the queue
        }
        long time = new Date().getTime(); //add the current time for the new api call to the queue
        dmojPrev.push(time);
        URL url = new URL(str); //converts the link from string to url object
        HttpURLConnection conn = (HttpURLConnection) url.openConnection(); //connects to url
        conn.addRequestProperty(key, value);
        JSONParser parse = new JSONParser();
        InputStream stream = conn.getInputStream(); //gets api data to input stream
        return (JSONObject) parse.parse(new InputStreamReader(stream)); //parses stream as a json object and returns it
    }
    public static String dmojSubmission(String str,String key,String value) throws InterruptedException, IOException {
        while (dmojPrev.size() >= DMOJLIMIT) {
            if (dmojPrev.front() >= new Date().getTime() - DMOJTIME) { //if there are more than 90 calls in the last minute, wait until a minute has passed since the oldest call
                Thread.sleep(dmojPrev.front() - (new Date().getTime() - DMOJTIME));
            }
            dmojPrev.pop(); //remove the oldest api call from the queue
        }
        long time = new Date().getTime(); //add the current time for the new api call to the queue
        dmojPrev.push(time);
        URL url = new URL(str); //converts the link from string to url object
        HttpURLConnection conn = (HttpURLConnection) url.openConnection(); //connects to url
        conn.addRequestProperty(key, value);
        return new BufferedReader(new InputStreamReader(conn.getInputStream()))
        		  .lines().collect(Collectors.joining("\n"));
    }
    
    public static void updateCache() { //updates the dmoj and codeforces problem caches
        try {
            JSONObject temp;
            dmojProblemCache = new ArrayList<Problem>(); //resets dmoj cache
            int page = 1;
            do { //look through each page of dmoj problems
                temp = (JSONObject) query("https://dmoj.ca/api/v2/problems?page=" + page).get("data"); //gets all problems on that page and adds it to cache
                for(int i=0;i<((Long)temp.get("current_object_count")).intValue();i++) dmojProblemCache.add(new Problem((JSONObject) ((JSONArray) temp.get("objects")).get(i),"dmoj"));
                System.out.println("loading cache " + page + "/" + temp.get("total_pages")); //prints progress
                page++;
            } while ((boolean) temp.get("has_more"));
            cfProblemCache = new ArrayList<Problem>();
            ArrayList<JSONObject> temp2=(JSONArray)((JSONObject) query("https://codeforces.com/api/problemset.problems").get("result")).get("problems"); //caches all codeforces problems
            for(JSONObject cur:temp2) cfProblemCache.add(new Problem(cur,"codeforces"));
        } catch (Exception e) { //exit if an error occurs while updating cache
            System.err.println("API error while updating cache");
            e.printStackTrace();
        }
    }
    
    public static ArrayList < Problem > getDmojProblems() { //returns cache of dmoj problems
        return new ArrayList < Problem > (dmojProblemCache);
    }
    
    public static ArrayList < Problem > getCfProblems() { //returns cache of codeforces problems
        return new ArrayList < Problem > (cfProblemCache);
    }
    
    public static Problem dmojProblemInfo(String problem) { //searches for info about a given dmoj problem in the cache
        int binarySearch = 1, pos = -1;
        while (binarySearch <= dmojProblemCache.size()) binarySearch *= 2;
        for (; binarySearch > 0; binarySearch /= 2) { //use binary search to find the problem in the cache, which is sorted in alphabetical order by problem code
            if (pos + binarySearch >= dmojProblemCache.size()) continue;
            int cmp = problem.compareTo(dmojProblemCache.get(pos + binarySearch).getCode());
            if (cmp == 0) return dmojProblemCache.get(pos + binarySearch); //return the element if it matches the problem being searched for
            if (cmp > 0) pos += binarySearch;
        }
        return null; //if the problem has not been found, return null
    }

    public static Problem cfProblemInfo(int contest, String index) { //searches for codeforces problem info about a problem given its contest and index
        int binarySearch = 1, pos = -1;
        index=contest+index;
        for(;contest<10000;contest*=10) index="-"+index;
        while (binarySearch <= cfProblemCache.size()) binarySearch *= 2;
        for (; binarySearch > 0; binarySearch /= 2) { //uses binary search on the cache which is sorted in descending contest number and index
            if (pos + binarySearch >= cfProblemCache.size()) continue;
            int cmp = index.compareTo(cfProblemCache.get(pos + binarySearch).getCode());
            if (cmp == 0) return cfProblemCache.get(pos + binarySearch); //return the element if it matches
            if (cmp < 0) pos += binarySearch;
        }
        return null; //if the problem has not been found, return null
    }
}