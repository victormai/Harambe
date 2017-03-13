//package com.example.moodly;
//
//import java.util.ArrayList;
//
///**
// * Created by MinhNguyen on 06/03/2017.
// */
//
//
//// https://www.youtube.com/watch?v=NZaXM67fxbs singleton design pattern Mar 06
//public class MoodController {
//    // this isn't safe from synchronization, does it need to be?
//    // i don't know how to verify that, but i guess we will find out
//    // soon
//    private static MoodController instance = null;
//    private ArrayList<Mood> moodList;
//    private ArrayList<Mood> moodFollowList;
//    private ArrayList<Mood> filteredList;
//
//    private MoodController() {
//        moodList = new ArrayList<Mood>();
//        moodFollowList = new ArrayList<Mood>();
//        filteredList = new ArrayList<Mood>();
//    }
//
//    public static MoodController getInstance() {
//
//        if(instance == null) {
//            instance = new MoodController();
//        }
//
//        return instance;
//    }
//
//    public void addMood(Mood m){
//        instance.moodList.add(m);
//    }
//
//    public void editMood (int position, Mood newMood) {
//        moodList.remove(position);
//        moodList.add(position, newMood);
//    }
//
//    public void deleteMood(int position) {
//        moodList.remove(position);
//    }
//
//
//    public String getLocation(int position) {
//        Mood m = moodList.get(position);
//        return m.getLocation();
//    }
//
//
//
//    public ArrayList<Mood> getFiltered() {
//        this.filter();
//        return this.filteredList;
//    }
//    // ??? make helper functions ?
//    // only filters and sets the filtered list as filteredList
//    // moodList should be the full list
//    // filtering person, good luck
//    // so, currently all this does is set filteredList to reference
//    // each of its elemnts from moodList, this is not a deep copy
//    private void filter(){
//        filteredList = (ArrayList<Mood>) moodList.clone();
//    }
//}
//
//


















package com.example.moodly.Controllers;

import java.util.ArrayList;

import android.os.AsyncTask;
import android.util.Log;

import com.example.moodly.Models.Mood;
import com.searchly.jestdroid.DroidClientConfig;
import com.searchly.jestdroid.JestClientFactory;
import com.searchly.jestdroid.JestDroidClient;

import java.util.List;

import io.searchbox.core.Delete;
import io.searchbox.core.DocumentResult;
import io.searchbox.core.Index;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;

import static junit.framework.Assert.assertEquals;

/**
 * Created by MinhNguyen on 06/03/2017.
 */


// https://www.youtube.com/watch?v=NZaXM67fxbs singleton design pattern Mar 06
public class MoodController extends ElasticSearchController {

    // this isn't safe from synchronization, does it need to be?
    // i don't know how to verify that, but i guess we will find out
    // soon
    private static MoodController instance = null;
    private Mood tempMood;
    private static ArrayList<Mood> moodList;
    private static ArrayList<Mood> moodHistoryList;
    private static ArrayList<Mood> moodFollowList;
    private ArrayList<Mood> filteredList;


    // constructor for our mood controller
    private MoodController() {
        // replace when we do offline, load from file etc
        moodList = new ArrayList<Mood>();
        moodHistoryList = new ArrayList<Mood>();
        moodFollowList = new ArrayList<Mood>();
        filteredList = new ArrayList<Mood>();
        tempMood = new Mood();
    }

    // gets an instance of our controller
    public static MoodController getInstance() {

        if(instance == null) {
            instance = new MoodController();
        }

        return instance;
    }


    /* ---------- Controller Functions ---------- */

    // adds the current mood in the controller
    public void addMood(int position, Mood m){
        if (position == -1) {
            // add to offline temporary list of moods
            moodHistoryList.add(0, m);
        } else {
            // maybe do a check for out of range here?
            moodHistoryList.set(position, m);
        }
        // add to elastic search
        MoodController.AddMoodTask addMoodTask = new MoodController.AddMoodTask();
        addMoodTask.execute(m);

    }


    public void deleteMood(int position) {

        Mood m = moodHistoryList.get(position);

        instance.moodHistoryList.remove(position);
        MoodController.DeketeMoodTask deleteMoodTask = new MoodController.DeketeMoodTask();
        deleteMoodTask.execute(m);

    }

    // not used
    public void editMood (int position, Mood m) {

//        instance.moodList.set(position, m);
//
//        MoodController.AddMoodTask addMoodTask = new MoodController.AddMoodTask();
//        addMoodTask.execute(m);

    }

    public ArrayList<Mood> getMoodList (ArrayList<String> userList) {

        MoodController.GetMoodTask getMoodTask = new MoodController.GetMoodTask();
        getMoodTask.execute(userList);
        // do I need to construct the array list or can i just declare it?
        ArrayList<Mood> tempMoodList = new ArrayList<Mood>();
        try {
            tempMoodList = getMoodTask.get();
        } catch (Exception e) {
            Log.i("Error", "Failed to get mood out of async object");
        }
        return tempMoodList;
    }

    public Mood getMood() {
        return tempMood;
    }
    public void setMood(Mood mood) { tempMood = mood;}


    /* ---------- Elastic Search Requests ---------- */

    // this adds the mood onto elastic search server
    private static class AddMoodTask extends AsyncTask<Mood, Void, Void> {

        int completion = 0;
        @Override
        protected Void doInBackground(Mood... moods){
            verifySettings();

            for(Mood mood : moods) {

                Index index = new Index.Builder(mood).index("cmput301w17t20").type("mood").build();

                try {
                    DocumentResult result = client.execute(index);
                    if (result.isSucceeded()) {
                        if (mood.getId() == null) {
                            mood.setId(result.getId());
                            // assumption method addMood always runs before this
                            // if the id is not set, set it
                            if(moodHistoryList.get(0).getId() == null) {
                                moodHistoryList.get(0).setId(result.getId());
                            }

                        }


                    } else {
                        Log.i("Error", "Elasticsearch was not able to add the mood");
                    }
                    // where is the client?
                }
                catch (Exception e) {
                    Log.i("Error", "The application failed to build and send the mood");
                }

            }

            return null;
        }
    }

    private static class DeketeMoodTask extends AsyncTask<Mood, Void, Boolean> {

        int completion = 0;
        @Override
        protected Boolean doInBackground(Mood... moods){
            verifySettings();

            for(Mood mood : moods) {

                // Did I include id twice?
                // if it works don't change it?
                Delete delete = new Delete.Builder(mood.getId()).index("cmput301w17t20").type("mood").id(mood.getId()).build();

                try {
                    DocumentResult result = client.execute(delete);
                    if (result.isSucceeded()) {
                        return true;
                    } else {
                        Log.i("Error", "Elasticsearch was not able to delete the mood");
                    }
                    // where is the client?
                }
                catch (Exception e) {
                    Log.i("Error", "The application failed to build and delete the mood");
                }

            }

            return false;
        }
    }


    private static class GetMoodTask extends AsyncTask<ArrayList<String>, Void, ArrayList<Mood>> {
        @Override
        protected ArrayList<Mood> doInBackground(ArrayList<String>... search_parameters) {
            verifySettings();

            ArrayList<String> usernames = search_parameters[0];
            // make a string to query with
            // for example "Jacky OR Melvin"
            String userNameString = usernames.get(0);
            for (int i = 1; i < usernames.size(); i++) {
                userNameString += " OR ";
                userNameString += usernames.get(i);
            }

            ArrayList<Mood> currentMoodList = new ArrayList<Mood>();
            // hahaha how do i even make a query string?????
            String query = "{ \n\"query\" : {\n" +
                                "\"query_string\" : { \n" +
                                    "\"fields\" : [\"owner\"],\n" +
                                    "\"query\" : \"" + userNameString + "\"\n" +
                                "}" +
                            "\n},";
            String sort = "\n\"sort\": { \"date\": { \"order\": \"desc\" } }";
            query += sort;
            query += " \n} ";

            // TODO Build the query
            Search search = new Search.Builder(query)
                    .addIndex("cmput301w17t20")
                    .addType("mood")
                    .build();

            try {
                // get the results of our query
                SearchResult result = client.execute(search);
                if(result.isSucceeded()) {
                    // hits
                    List<SearchResult.Hit<Mood, Void>> foundMoods = result.getHits(Mood.class);

                    for(int i = 0; i < foundMoods.size(); i++) {
                        Mood temp = foundMoods.get(i).source;
                        currentMoodList.add(temp);

                    }
                    moodList = currentMoodList;
                    // for your own list of moods
                    if (usernames.size() == 1) {
                        moodHistoryList = currentMoodList;
                    } else {
                        moodFollowList = currentMoodList;
                    }
                } else {
                    Log.i("Error", "Search query failed to find any moods that matched");
                }
            }
            catch (Exception e) {
                Log.i("Error", "Something went wrong when we tried to communicate with the elasticsearch server!");
            }
            // ??? not needed?
            return currentMoodList;
        }
    }



//    public String getLocation(int position) {
//        Mood m = moodList.get(position);
//        return m.getLocation();
//    }

    /* ---------- Helpers ---------- */

    public ArrayList<Mood> getHistoryMoods () {
        return moodHistoryList;
    }

    public ArrayList<Mood> getFollowMoods () {
        return moodFollowList;
    }

    public ArrayList<Mood> getFiltered() {
        this.filter();
        return this.filteredList;
    }

    private void filter(){
        filteredList = (ArrayList<Mood>) moodList.clone();
    }

    // we can do binary search on this btw
    // and don't we have to sort it too?
//    LEAVE THIS COMMENTED OUT FOR NOW, USE WHEN WE HAVE ABILITY TO FILTER IN GUI
//    protected ArrayList<Mood> filterByDate(Date startDate, Date endDate) {
//        ArrayList<Mood> result = new ArrayList<>();
//        for (Mood m: moodList) {
//            if (m.getDate().after(startDate) && m.getDate().before(endDate)){
//                result.add(m);
//            }
//        }
//        return result;
//    }
//
//    protected ArrayList<Mood> filterByEmoState(Emotion e) {
//        ArrayList<Mood> result = new ArrayList<>();
//        for (Mood m: moodList) {
//            if (m.getEmotion().equals(e)){
//                result.add(m);
//            }
//        }
//        return  result;
//    }
//
//    protected ArrayList<Mood> filterByTextReason(String reason) {
//        ArrayList<Mood> result = new ArrayList<>();
//        for(Mood m:moodList) {
//            if (m.getReasonText().contains(reason)) {
//                result.add(m);
//            }
//        }
//        return result;
//    }



}