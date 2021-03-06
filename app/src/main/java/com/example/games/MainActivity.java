package com.example.games;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    //42 globalna premenna pre handler volani JSON
    private RequestQueue mQueue;
    private ArrayList<Game> games = new ArrayList<Game>();
    private Pages pages;
    private String mainAddress = "https://rawg.io/api/games";
    private ArrayList<String> favorites;
    RecyclerView recyclerView;
    RecyclerViewAdapter adapter;
    private SwipeRefreshLayout refreshLayout;
    int visibleItemCount;
    int totalItemCount;
    int pastVisiblesItems;
    LinearLayoutManager mLayoutManager;
    boolean loading = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
                startActivity(new Intent(getApplicationContext(), GameDetailActivity.class));
            }
        });
        pages = new Pages("null", "null");
        initialRecycleItems();
        //42 init handleru s tym ze tento si pripravy frontu na volania
        mQueue = Volley.newRequestQueue(this);
        favorites = new ArrayList<String>();
        refLayout();
        jsonParse();
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener()
        {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy)
            {
                if(dy > 0) //check for scroll down
                {
                    visibleItemCount = mLayoutManager.getChildCount();
                    totalItemCount = mLayoutManager.getItemCount();
                    pastVisiblesItems = mLayoutManager.findFirstVisibleItemPosition();

                    if (loading)
                    {
                        if ( (visibleItemCount + pastVisiblesItems) >= totalItemCount-1)
                        {
                            loading = false;
                            jsonParse(pages.getNext(), false);
                        }
                    }
                }
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        int a = 5;
       String id = "";
        boolean favorite = false;
        int no = 0;
        if(resultCode==777){
            try {
                id = data.getStringExtra("id");
                no = data.getIntExtra("no",0);
                favorite = data.getBooleanExtra("favorite", false);
                if(favorite){
                    favorites.add(id);
                }else{
                    for(int i = 0; i<favorites.size(); i++){
                        if(favorites.get(i).equals(id)){
                            favorites.remove(i);
                            break;
                        }
                    }
                }
                games.get(no).setFavorite(favorite);
                addAndNotify();
            }catch (Exception e){

            }
        }else{

        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onStop() {

        super.onStop();
    }

    private void initialRecycleItems() {
        recyclerView = findViewById(R.id.recycle_view);
        adapter = new RecyclerViewAdapter(this, games);
        recyclerView.setAdapter(adapter);
        mLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(mLayoutManager);

    }

    private void refLayout() {
        refreshLayout = findViewById(R.id.swipe_refresh_layout);
        refreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                jsonParse();
            }
        });
    }

    private void jsonParse(String url, final boolean deleteOld){
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    if(deleteOld){
                        games.removeAll(new ArrayList<>(games));
                    }
                    pages = new Pages(response.getString("next"), response.getString("previous"));
                    JSONArray results = response.getJSONArray("results");
                    if(results.length()>0){
                        for (int i = 0; i < results.length(); i++) {

                            ArrayList<String> screenshots = new ArrayList<String>();
                            ArrayList<String> platforms = new ArrayList<String>();

                            JSONObject game = results.getJSONObject(i);

                            JSONArray short_screenshots = (JSONArray) results.getJSONObject(i).get("short_screenshots");
                            JSONArray parent_platforms = (JSONArray) results.getJSONObject(i).get("parent_platforms");

                            for (int j = 0; j<short_screenshots.length(); j++){
                                JSONObject screen = short_screenshots.getJSONObject(j);
                                screenshots.add(screen.getString("image"));
                            }

                            for (int j = 0; j<parent_platforms.length(); j++){
                                JSONObject platform = parent_platforms.getJSONObject(j);
                                platforms.add(platform.getJSONObject("platform").getString("name"));
                            }

                            Game adding = new Game(
                                    game.getString("id"),
                                    game.getString("name"),
                                    game.getString("background_image"),
                                    game.getString("rating"),
                                    platforms,
                                    screenshots,
                                    game.getString("released")
                            );
                            games.add(adding);
                        }
                        Toast.makeText(getApplicationContext(),"Games succesfully loaded",Toast.LENGTH_SHORT).show();

                    }
                    else {
                        Toast.makeText(getApplicationContext(),"Error at games feed",Toast.LENGTH_SHORT).show();
                    }
                    addAndNotify();

                } catch (JSONException e) {
                    Toast.makeText(getApplicationContext(),"Error at games feed",Toast.LENGTH_SHORT).show();

                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(getApplicationContext(),"Error at games feed",Toast.LENGTH_SHORT).show();
                error.printStackTrace();
            }
        });
        mQueue.add(request);
    }

    private void jsonParse() {
        StringBuilder sb1 = new
                StringBuilder(mainAddress);
        String url;
       // sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        //String name = sharedPreferences.getString("category", "");
        sb1.append("?page_size=6");
        url = sb1.toString();

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    games.removeAll(new ArrayList<>(games));
                    pages = new Pages(response.getString("next"), response.getString("previous"));
                   JSONArray results = response.getJSONArray("results");
                    if(results.length()>0){
                        for (int i = 0; i < results.length(); i++) {

                            ArrayList<String> screenshots = new ArrayList<String>();
                            ArrayList<String> platforms = new ArrayList<String>();

                            JSONObject game = results.getJSONObject(i);

                            JSONArray short_screenshots = (JSONArray) results.getJSONObject(i).get("short_screenshots");
                            JSONArray parent_platforms = (JSONArray) results.getJSONObject(i).get("parent_platforms");

                            for (int j = 0; j<short_screenshots.length(); j++){
                                JSONObject screen = short_screenshots.getJSONObject(j);
                                screenshots.add(screen.getString("image"));
                            }

                            for (int j = 0; j<parent_platforms.length(); j++){
                                JSONObject platform = parent_platforms.getJSONObject(j);
                                platforms.add(platform.getJSONObject("platform").getString("name"));
                            }

                            Game adding = new Game(
                                    game.getString("id"),
                                    game.getString("name"),
                                    game.getString("background_image"),
                                    game.getString("rating"),
                                    platforms,
                                    screenshots,
                                    game.getString("released")
                            );
                           games.add(adding);
                        }
                        Toast.makeText(getApplicationContext(),"Games succesfully loaded",Toast.LENGTH_SHORT).show();

                    }
                    else {
                        Toast.makeText(getApplicationContext(),"Error at games feed",Toast.LENGTH_SHORT).show();
                    }
                    addAndNotify();

                } catch (JSONException e) {
                    Toast.makeText(getApplicationContext(),"Error at games feed",Toast.LENGTH_SHORT).show();

                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(getApplicationContext(),"Error at games feed",Toast.LENGTH_SHORT).show();
                error.printStackTrace();
            }
        });
        mQueue.add(request);
    }

    private void addAndNotify(){
        loading = true;
        adapter.notifyDataSetChanged();
        refreshLayout.setRefreshing(false);
    }

//    @Override
//    protected void onPostResume() {
//        Intent mIntent = getIntent();
//        int no = mIntent.getIntExtra("no", 0);
//        Toast.makeText(getApplicationContext(),"Games succesfully loaded"+no,Toast.LENGTH_SHORT).show();
//        super.onPostResume();
//    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
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
        if (id == R.id.favorites) {
            Intent intent = new Intent(this, GameFavoritesActivity.class);
            if(favorites.size()>0){
                favorites = removeDuplicates(favorites);
                intent.putExtra("favorites", favorites);
                    startActivityForResult(intent,888);
            }else{
                Toast.makeText(getApplicationContext(),"You have 0 favorite games",Toast.LENGTH_SHORT).show();
            }

        }

        return super.onOptionsItemSelected(item);
    }
    public static <T> ArrayList<T> removeDuplicates(ArrayList<T> list)
    {

        // Create a new ArrayList
        ArrayList<T> newList = new ArrayList<T>();

        // Traverse through the first list
        for (T element : list) {

            // If this element is not present in newList
            // then add it
            if (!newList.contains(element)) {

                newList.add(element);
            }
        }

        // return the new list
        return newList;
    }
}
