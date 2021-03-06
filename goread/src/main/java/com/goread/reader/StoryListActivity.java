/*
 * Copyright (c) 2013 Matt Jibson <matt.jibson@gmail.com>
 *
 * Permission to use, copy, modify, and distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
 * ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
 * OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package com.goread.reader;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.android.volley.Request;
import com.android.volley.Response;
import com.jakewharton.disklrucache.DiskLruCache;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

public class StoryListActivity extends ListActivity {

    private ArrayAdapter<String> aa;
    private ArrayList<JSONObject> sl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_storylist);
        aa = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
        setListAdapter(aa);
        try {
            JSONObject stories = MainActivity.stories;
            sl = new ArrayList<JSONObject>();

            Intent it = getIntent();
            int p = it.getIntExtra(MainActivity.K_FOLDER, -1);
            if (it.hasExtra(MainActivity.K_FEED)) {
                String f = it.getStringExtra(MainActivity.K_FEED);
                setTitle(MainActivity.feeds.get(f).getString("Title"));
                addFeed(stories, f);
            } else if (p >= 0) {
                JSONArray a = MainActivity.lj.getJSONArray("Opml");
                JSONObject folder = a.getJSONObject(p);
                setTitle(folder.getString("Title"));
                a = folder.getJSONArray("Outline");
                for (int i = 0; i < a.length(); i++) {
                    JSONObject f = a.getJSONObject(i);
                    addFeed(stories, f.getString("XmlUrl"));
                }
            } else {
                setTitle(R.string.all_items);
                Iterator<String> keys = stories.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    addFeed(stories, key);
                }
            }
            addStories();

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void addFeed(JSONObject stories, String feed) {
        try {
            JSONArray sa = stories.getJSONArray(feed);
            for (int i = 0; i < sa.length(); i++) {
                JSONObject s = sa.getJSONObject(i);
                s.put("feed", feed);
                sl.add(s);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void addStories() {
        try {
            Collections.sort(sl, new StoryComparator());
            for (JSONObject s : sl) {
                String t = null;
                t = s.getString("Title");
                if (t.length() == 0) t = getString(R.string.title_unknown);
                t += " - " + MainActivity.feeds.get(s.getString("feed")).getString("Title");
                aa.add(t);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public class StoryComparator implements Comparator<JSONObject> {
        @Override
        public int compare(JSONObject o1, JSONObject o2) {
            int c = 0;
            try {
                c = new Long(o1.getLong("Date")).compareTo(new Long(o2.getLong("Date")));
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return c;
        }
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        final JSONObject so = sl.get(position);
        final String feed;
        final String story;
        final Intent i = new Intent(this, StoryActivity.class);
        i.putExtra("story", so.toString());
        try {
            feed = so.getString("feed");
            story = so.getString("Id");
            String key = MainActivity.hashStory(feed, story);
            DiskLruCache.Snapshot s = MainActivity.storyCache.get(key);
            if (s != null) {
                String c = s.getString(0);
                i.putExtra("contents", c);
                Log.e("goread", "from cache");
                startActivity(i);
                return;
            }

            // if we didn't fetch from cache, just download it
            // todo: populate the cache

            JSONArray a = new JSONArray();
            JSONObject o = new JSONObject();
            o.put("Feed", feed);
            o.put("Story", story);
            a.put(o);


            MainActivity.rq.add(new com.goread.reader.JsonArrayRequest(Request.Method.POST, MainActivity.GOREAD_URL + "/user/get-contents", a, new Response.Listener<JSONArray>() {
                @Override
                public void onResponse(JSONArray jsonArray) {
                    try {
                        String r = jsonArray.getString(0);
                        i.putExtra("contents", r);
                        Log.e("goread", "NOT from cache");
                        startActivity(i);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }, null));
        } catch (JSONException e) {
            return;
        } catch (IOException e) {
            // todo: perhaps not return, since it's just the cache not being available
            return;
        }
    }
}