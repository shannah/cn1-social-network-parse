/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.demos.parse_social;

import ca.weblite.codename1.json.JSONArray;
import ca.weblite.codename1.json.JSONException;
import ca.weblite.codename1.json.JSONObject;
import com.codename1.io.Log;
import com.codename1.io.NetworkManager;
import com.codename1.ui.EncodedImage;
import com.codename1.ui.Image;
import com.parse4cn1.Parse;
import com.parse4cn1.ParseCloud;
import com.parse4cn1.ParseException;
import com.parse4cn1.ParseFile;
import com.parse4cn1.ParseObject;
import com.parse4cn1.ParseUser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;





/**
 *
 * @author shannah
 */
public class SocialClientParse {
    private String token;
    private static String URL = "http://cn-social-network-demo.weblite.ca";
    private ParseUser user;
    
    public SocialClientParse() {
        //NetworkManager.getInstance().setThreadCount(10);
        Parse.initialize("zY7cu3JyYsCresgZ0P3WMALCNHN8JsqWtTWWNZU7", "mbWtI78kobm5j5fjFLqPOKcZWVE3YXiPAn0LqkbL");
        
        
    }
    
    
    
    public void register(String username, String password) throws IOException {
        try {
            ParseUser user = ParseUser.create(username, password);
            user.put("screen_name", username);
            user.signUp();
            
            
        } catch (ParseException ex) {
            Log.e(ex);
            throw new IOException(ex.getMessage());
        }
    }
    
    public void login(String username, String password) throws IOException {
        try {
            long start = System.currentTimeMillis();
            System.out.println("Before user create "+ start);
            user = ParseUser.create(username, password);
            System.out.println("After usr create "+(System.currentTimeMillis()-start));
            user.login();
            System.out.println("After login() "+(System.currentTimeMillis()-start));
            token = user.getSessionToken();
            
            
        } catch (ParseException ex) {
            Log.e(ex);
            throw new IOException(ex.getMessage());
        }
       
    }
    
    public void logout() throws IOException {
        try {
            user.logout();
            user = null;
            token = null;
        } catch (ParseException ex) {
            Log.e(ex);
            throw new IOException(ex.getMessage());
        }
    }
    
    public List<Map> getFriends() throws IOException {
        return getList("get_friends",  "friends");
    }
    
    public List<Map> findUsers(String query) throws IOException {
        
        return getList("find_users", new Object[]{"screen_name", query}, "results");
        
        
    }
    
    public List<Map> getPendingFriendRequests() throws IOException {
        return getList("get_pending_friend_requests", "requests");
    }
    
    
    
    public void sendFriendRequest(String username) throws IOException {
        callFunc("send_friend_request", new Object[]{"username", username});
        
    }
    
    public void acceptFriendRequest(String username) throws IOException {
        callFunc("accept_friend_request", new Object[]{"username", username});
    }
    
    public void declineFriendRequest(String username) throws IOException {
        callFunc("decline_friend_request", new Object[]{"username", username});
    }
    
    public Map getProfile(String username) throws IOException {
        return getMap("get_profile", "profile");
    }
    
    public void updateProfile(Map values) throws IOException {
        values = new HashMap(values);
        if (values.containsKey("avatar")) {
            try {
                Image img = (Image)values.get("avatar");
                EncodedImage encImg = null;
                if (img instanceof EncodedImage) {
                    encImg = (EncodedImage)img;
                } else {
                    encImg = EncodedImage.createFromImage(img, false);
                }
                ParseFile imgFile = new ParseFile("avatar.png", encImg.getImageData(), "image/png");
                imgFile.save();
                
                ParseObject uploadObject = ParseObject.create("Upload");
                uploadObject.put("file", imgFile);
                uploadObject.save();
                
                values.put("avatar", uploadObject.getObjectId());
                //values.put("avatar", com.codename1.util.Base64.encode(encImg.getImageData()));
            } catch (ParseException ex) {
                Log.e(ex);
                ex.printStackTrace();
                throw new IOException(ex.getMessage());
            }

        }
        callFunc("update_profile", values);
    }
    
    public String post(Map post) throws IOException {
        try {
            HashMap params = new HashMap();
            
            
            if (post.containsKey("photo")) {
                Image img = (Image)post.get("photo");
                
                EncodedImage encImg = null;
                if (img instanceof EncodedImage) {
                    encImg = (EncodedImage)img;
                } else {
                    encImg = EncodedImage.createFromImage(img, false);
                }
                ParseFile imgFile = new ParseFile("photo.png", encImg.getImageData(), "image/png");
                imgFile.save();
                
                ParseObject uploadObject = ParseObject.create("Upload");
                uploadObject.put("file", imgFile);
                uploadObject.save();
                
                params.put("photo", uploadObject.getObjectId());
                
            }
            
            if (post.containsKey("comment")) {
                params.put("comment", post.get("comment"));
            }
            
            JSONObject response = (JSONObject)ParseCloud.callFunction("post", params);
            int code = response.getInt("code");
            if (code != 200) {
                throw new IOException(response.getString("message"));
            } else {
                return response.getString("post_id");
            }
        } catch (Throwable ex) {
            Log.e(ex);
            ex.printStackTrace();
            throw new IOException(ex.getMessage());
        }
    }
    
    public List<Map> getPosts(String username, Date olderThan) throws IOException {
        return null;
    }
    
    private void callFunc(String funcName) throws IOException {
        callFunc(funcName, (Map)null);
    }
    
    private void callFunc(String funcName, Object[] params) throws IOException {
        Map m = new HashMap();
        if (params != null) {
            int len = params.length;
            for (int i=0; i<len; i+=2) {
                m.put(params[i], params[i+1]);
            }
        }
        callFunc(funcName, m);
    }
    
    private void callFunc(String funcName, Map params) throws IOException {
        try {
            JSONObject response = (JSONObject)ParseCloud.callFunction(funcName, params);
            int code = response.getInt("code");
            if (code != 200) {
                throw new IOException(response.getString("message"));
            } 
        } catch (Throwable ex) {
            Log.e(ex);
            ex.printStackTrace();
            throw new IOException(ex.getMessage());
        }
    }
    
    private List<Map> getList(String funcName, String listKey) throws IOException {
        return getList(funcName, (Map)null, listKey);
    }
    
    private List<Map> getList(String funcName, Object[] params, String listKey) throws IOException {
        
        Map m = new HashMap();
        if (params != null) {
            int len = params.length;
            for (int i=0; i<len; i+=2) {
                m.put(params[i], params[i+1]);
            }
        }
        return getList(funcName, m, listKey);
    }
    
    private List<Map> getList(String funcName, Map params, String listKey) throws IOException {
        try {
            JSONObject response = (JSONObject)ParseCloud.callFunction(funcName, params);
            System.out.println(response);
            int code = response.getInt("code");
            if (code != 200) {
                throw new IOException(response.getString("message"));
            } else {
                ArrayList<Map> out = new ArrayList<Map>();
                JSONArray posts = response.getJSONArray(listKey);
                int len = posts.length();
                for (int i=0; i<len; i++) {
                    JSONObject row = posts.getJSONObject(i);
                    out.add(toMap(row));
                }
                return out;
            }
        } catch (Throwable ex) {
            Log.e(ex);
            ex.printStackTrace();
            throw new IOException(ex.getMessage());
        }
    }
    
    private Map getMap(String funcName, String mapKey) throws IOException {
        return getMap(funcName, (Map)null, mapKey);
    }
    
    private Map getMap(String funcName, Object[] params, String mapKey) throws IOException {
        Map m = new HashMap();
        if (params != null) {
            int len = params.length;
            for (int i=0; i<len; i+=2) {
                m.put(params[i], params[i+1]);
            }
        }
        return getMap(funcName, m, mapKey);
    }
    
    private Map getMap(String funcName, Map params, String mapKey) throws IOException {
        try {
            JSONObject response = (JSONObject)ParseCloud.callFunction(funcName, params);
            int code = response.getInt("code");
            if (code != 200) {
                throw new IOException(response.getString("message"));
            } else {
                
                JSONObject row = response.getJSONObject(mapKey);
                return toMap(row);
            }
        } catch (Throwable ex) {
            Log.e(ex);
            ex.printStackTrace();
            throw new IOException(ex.getMessage());
        }
    }
    
    public List<Map> getFeed(Date olderThan) throws IOException {
       return getList("get_feed", "posts");
    }
    
    Map toMap(JSONObject o) throws JSONException {
        HashMap out = new HashMap();
        Iterator it=o.keys();
        while (it.hasNext()) {
            String key = (String)it.next();
            Object val = o.get(key);
            if (JSONObject.NULL.equals(val)) {
                val = null;
            }
            out.put(key, val);
        }
        System.out.println("toMap "+out);
        return out;
    }
    
    public String getUsername() {
        return user.getUsername();
    }
    
}
