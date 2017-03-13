package com.example.moodly.Activities;

/**
 * Created by jkc1 on 2017-03-05.
 */


import android.content.Intent;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.support.design.widget.FloatingActionButton;

import com.example.moodly.Adapters.FollowingMoodAdapter;
import com.example.moodly.Adapters.MoodAdapter;
import com.example.moodly.Controllers.MoodController;
import com.example.moodly.Controllers.UserController;
import com.example.moodly.Models.Mood;
import com.example.moodly.Models.User;
import com.example.moodly.R;

import java.util.ArrayList;


/**
 * This class is a fragment to display moods from followed users
 */
public class TabBase extends Fragment {

    protected User currentUser;
    protected ArrayList<String> userList;

    // we want to move the arraylist onto the controller
    protected Mood mood;
    protected FollowingMoodAdapter adapter;
    protected ListView displayMoodList;
    protected ArrayList<Mood> moodList = new ArrayList<Mood>();
    protected View rootView;

    protected MoodController moodController = MoodController.getInstance();
    protected UserController userController = UserController.getInstance();


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        currentUser = userController.getCurrentUser();
        userList = currentUser.getFollowing();

        refreshOnline(userList);
        setViews(inflater, container);
        hideViews();

        setListeners();

        return rootView;
    }

    /**
     * Sets the views in the activities
     * @param inflater the layout inflater
     * @param container the view group
     */
    protected void setViews(LayoutInflater inflater, ViewGroup container) {
        rootView = inflater.inflate(R.layout.mood_history, container, false);
        displayMoodList = (ListView) rootView.findViewById(R.id.display_mood_list);
        adapter = new FollowingMoodAdapter(getActivity(), R.layout.following_mood_list_item, moodList);
        displayMoodList.setAdapter(adapter);

    }

    /**
     * Hides views specific to the history list (Such as the add button)
     */
    protected void hideViews() {
        FloatingActionButton fab = (FloatingActionButton) rootView.findViewById(R.id.fab);
        fab.hide();

    }

    // Used for project part 5 to set the listeners for the filter button
    protected void setListeners() {

    }


    /* ---------- Refreshing Moods ---------- */
    // by part 5 of the project these two will be reduced to a single method

    /**
     * Gets our mood list from elastic search
     * @param tempUserList list of users (Strings) to match with the moods we want to get
     */
    protected void refreshOnline(ArrayList<String> tempUserList) {
        moodList = moodController.getMoodList(tempUserList);
    }

    /**
     * Gets the mood list from our controller and updates the adapters
     */
    protected void refreshOffline() {
        moodList = MoodController.getInstance().getFollowMoods();

        adapter = new FollowingMoodAdapter(getActivity(), R.layout.following_mood_list_item, moodList);
        displayMoodList.setAdapter(adapter);
        // needed ?
        adapter.notifyDataSetChanged();
    }

}
