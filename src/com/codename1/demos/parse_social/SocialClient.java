/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.demos.parse_social;

import com.codename1.io.ConnectionRequest;
import com.codename1.io.JSONParser;
import com.codename1.io.MultipartRequest;
import com.codename1.io.NetworkManager;
import com.codename1.io.Util;
import com.codename1.ui.Display;
import com.codename1.ui.EncodedImage;
import com.codename1.ui.Image;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author shannah
 */
public class SocialClient {
    private String token;
    private static String URL = "http://cn-social-network-demo.weblite.ca";
    private String username;
    
    
    
    
    private ConnectionRequest sendRequest(Object[] params) throws IOException {
        boolean isMultipart = false;
        int plen = params.length;
        for (int i=0; i < params.length; i+=2) {
            if (params[i+1] instanceof Image) {
                isMultipart = true;
                break;
            }
        }
        ConnectionRequest req = isMultipart ? new MultipartRequest() : new ConnectionRequest();
        req.setUrl(URL+"/index.php");
        req.setPost(true);
        req.setHttpMethod("POST");
        req.addArgument("-action", "friends_api");
        
        for (int i=0; i<plen; i+=2) {
            if (isMultipart && params[i+1] instanceof Image) {
                Image img = (Image)params[i+1];
                EncodedImage enc = null;
                if (img instanceof EncodedImage) {
                    enc = (EncodedImage)img;
                } else {
                    enc = EncodedImage.createFromImage(img, false);
                }
                ((MultipartRequest)req).addData((String)params[i], enc.getImageData(), "image/png");
                
            } else {
                //req.addArgumentNoEncoding(Util.encodeUrl((String)params[i]), Util.encodeUrl((String)params[i+1]));
                req.addArgumentNoEncoding((String)params[i], (String)params[i+1]);
            }
        }
        NetworkManager.getInstance().addToQueueAndWait(req);
        return req;
    }
    
    
    
    
    
    private Map getResponse(Object[] params) throws IOException {
        ConnectionRequest req = sendRequest(params);
        if (req.getResponseCode() == 200) {
            System.out.println(new String(req.getResponseData(), "UTF-8"));
            Map out = new HashMap();
            IOException[] err = new IOException[1];
            Display.getInstance().invokeAndBlock(() -> {
                JSONParser p = new JSONParser();
                try (InputStreamReader r = new InputStreamReader(new ByteArrayInputStream(req.getResponseData()))) {
                    out.putAll(p.parseJSON(r));
                } catch (IOException ex) {
                    System.out.println("Failed to parse JSON ");
                    err[0] = ex;
                }
            });
            
            if (out != null) {
                return out;
            } else {
                throw err[0];
            }
        } else {
            throw new IOException("Request failed with response "+req.getResponseCode());
        }
    }
    
    public void register(String username, String password) throws IOException {
        Map res = getResponse(new String[]{
            "-do" , "register",
            "username", username,
            "password", password
        });
        
        int code = (int)(double)res.get("code");
        if (code != 200) {
            throw new IOException((String)res.get("message"));
        }
    }
    
    public void login(String username, String password) throws IOException {
        Map res = getResponse(new String[]{
            "-do" , "login",
            "username", username,
            "password", password
        });
        
        int code = (int)(double)res.get("code");
        if (code != 200) {
            throw new IOException((String)res.get("message"));
        } else {
            token = (String)res.get("token");
            this.username = username;
            if (token == null) {
                throw new IOException("No token received after login");
            }
        }
    }
    
    public void logout() throws IOException {
        Map res = getResponse(new String[]{
            "-do" , "logout",
            "token", token
        });
        
        int code = (int)(double)res.get("code");
        if (code != 200) {
            throw new IOException((String)res.get("message"));
        } else {
            token = null;
            username = null;
        }
    }
    
    public List<Map> getFriends() throws IOException {
        Map res = getResponse(new String[]{
            "-do" , "get_friends",
            "token", token
        });
        
        int code = (int)(double)res.get("code");
        if (code != 200) {
            throw new IOException((String)res.get("message"));
        } else {
            return (List<Map>)res.get("friends");
        }
    }
    
    public List<Map> findUsers(String query) throws IOException {
        Map res = getResponse(new String[]{
            "-do" , "find_users",
            "token", token,
            "filter", query
        });
        
        int code = (int)(double)res.get("code");
        if (code != 200) {
            throw new IOException((String)res.get("message"));
        } else {
            return (List<Map>)res.get("results");
        }
    }
    
    public List<Map> getPendingFriendRequests() throws IOException {
        Map res = getResponse(new String[]{
            "-do" , "get_pending_friend_requests",
            "token", token
        });
        
        int code = (int)(double)res.get("code");
        if (code != 200) {
            throw new IOException((String)res.get("message"));
        } else {
            return (List<Map>)res.get("requests");
        }
    }
    
    public void sendFriendRequest(String username) throws IOException {
        Map res = getResponse(new String[]{
            "-do" , "send_friend_request",
            "token", token,
            "friend", username
        });
        
        int code = (int)(double)res.get("code");
        if (code != 200) {
            throw new IOException((String)res.get("message"));
        } 
    }
    
    public void acceptFriendRequest(String username) throws IOException {
        Map res = getResponse(new String[]{
            "-do" , "accept_friend_request",
            "token", token,
            "friend", username
        });
        
        int code = (int)(double)res.get("code");
        if (code != 200) {
            throw new IOException((String)res.get("message"));
        } 
    }
    
    public void declineFriendRequest(String username) throws IOException {
        Map res = getResponse(new String[]{
            "-do" , "decline_friend_request",
            "token", token,
            "friend", username
        });
        
        int code = (int)(double)res.get("code");
        if (code != 200) {
            throw new IOException((String)res.get("message"));
        } 
    }
    
    public Map getProfile(String username) throws IOException {
        System.out.println("Getting profile for "+username);
        Map res = getResponse(new String[]{
            "-do" , "get_profile",
            "token", token,
            "username", username
        });
        
        int code = (int)(double)res.get("code");
        if (code != 200) {
            throw new IOException((String)res.get("message"));
        } else {
            return (Map)res.get("profile");
        }
    }
    
    public void updateProfile(Map profile) throws IOException {
        List params = new ArrayList();
        params.add("-do");
        params.add("update_profile");
        params.add("token");
        params.add(token);
        
        if (profile.containsKey("avatar")) {
            params.add("avatar");
            params.add(profile.get("avatar"));
        }
        
        if (profile.containsKey("screen_name")) {
            params.add("screen_name");
            params.add(profile.get("screen_name"));
        }
        
        Map res = getResponse(params.toArray(new Object[params.size()]));
        
        int code = (int)(double)res.get("code");
        if (code != 200) {
            throw new IOException((String)res.get("message"));
        } 
    }
    
    public long post(Map post) throws IOException {
        List params = new ArrayList();
        params.add("-do");
        params.add("post");
        params.add("token");
        params.add(token);
        
        if (post.containsKey("photo")) {
            params.add("photo");
            params.add(post.get("photo"));
        }
        
        if (post.containsKey("comment")) {
            params.add("comment");
            params.add(post.get("comment"));
        }
        
        Map res = getResponse(params.toArray(new Object[params.size()]));
        
        int code = (int)(double)res.get("code");
        if (code != 200) {
            throw new IOException((String)res.get("message"));
        } else {
            return (long)(double)res.get("post_id");
        }
    }
    
    public List<Map> getPosts(String username, Date olderThan) throws IOException {
        Map res = getResponse(new String[]{
            "-do" , "get_posts",
            "token", token,
            "username", username,
            "older_than", olderThan.getTime()/1000+""
        });
        
        int code = (int)(double)res.get("code");
        if (code != 200) {
            throw new IOException((String)res.get("message"));
        } else {
            return (List<Map>)res.get("posts");
        }
    }
    
    public List<Map> getFeed(Date olderThan) throws IOException {
        if (olderThan == null) {
            olderThan = new Date();
        }
        Map res = getResponse(new String[]{
            "-do" , "get_feed",
            "token", token,
            "older_than", olderThan.getTime()/1000+""
        });
        
        int code = (int)(double)res.get("code");
        if (code != 200) {
            throw new IOException((String)res.get("message"));
        } else {
            return (List<Map>)res.get("posts");
        }
    }
    
    public String getUsername() {
        return username;
    }
    
}
