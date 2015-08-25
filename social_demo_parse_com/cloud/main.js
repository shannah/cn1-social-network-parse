
// Use Parse.Cloud.define to define as many cloud functions as you want.
// For example:
Parse.Cloud.define("hello", function(request, response) {
  response.success("Hello world!");
});

Parse.Cloud.define("send_friend_request", function(request, response) {
    Parse.Cloud.useMasterKey();
    
    (new Parse.Query(Parse.User)).equalTo("username", request.params.username).each(function(friend) {
        friend.relation("pendingFriendRequests").add(Parse.User.current());
        friend.save().then(function(result) {
            response.success({code: 200, message: "Successfully sent friend request"});
        });
    });

});

Parse.Cloud.define("accept_friend_request", function(request, response) {
    Parse.Cloud.useMasterKey();
    
    var currentUser = Parse.User.current();
    var pendingRequests = currentUser.relation("pendingFriendRequests");
    
    pendingRequests.query().equalTo("username", request.params.username).each(function(friend) {
        currentUser.relation("friends").add(friend);
        pendingRequests.remove(friend);
        currentUser.save().then(function(result) {
            friend.relation("friends").add(currentUser);
            return friend.save();
            
        }, function(error) {
            response.success({code : 500, message : error});
        }).then(function(result) {
            response.success({code: 200, message: "Friend request accepted"});
        }, function(error) {
            response.success({code : 500, message : error});
        });
    });

});

Parse.Cloud.define("decline_friend_request", function(request, response) {
    Parse.Cloud.useMasterKey();
    
    var currentUser = Parse.User.current();
    var pendingRequests = currentUser.relation("pendingFriendRequests");
    
    pendingRequests.query().equalTo("username", request.params.username).each(function(friend) {
        pendingRequests.remove(friend);
        currentUser.save().then(function(result) {
            response.success({code: 200, 'message' : "The friend request has been declined"});
        });
    });

});

Parse.Cloud.define("get_pending_friend_requests", function(request, response) {
    Parse.Cloud.useMasterKey();
    
    var out = [];
    var user = Parse.User.current();
    user.relation("pendingFriendRequests").query().each(function(friend) {
        out.push({
            sender : friend.get("username"),
            receiver : user.get("username"),
            avatar : friend.get("avatar") ? friend.get("avatar").url() : null,
            screen_name : friend.get("screen_name")
            
        });
    })
    .then(function(result) {
        response.success({code: 200, requests: out});
    });
    
    //response.success("getting friends");
    
});


Parse.Cloud.define("get_friends", function(request, response) {
    Parse.Cloud.useMasterKey();
    console.log(Parse.User.current());
    var out = [];
    Parse.User.current().relation("friends").query().each(function(friend) {
        out.push({
            username : friend.get("username"),
            screen_name : friend.get("screen_name"),
            avatar : friend.get('avatar') ? friend.get('avatar').url() : null
            
        });
    })
    .then(function(result) {
        response.success({code : 200, friends: out});
    });
    
    //response.success("getting friends");
    
});

Parse.Cloud.define("post", function(request, response) {
    Parse.Cloud.useMasterKey();
    
    var Post = Parse.Object.extend("Post");
    var Upload = Parse.Object.extend("Upload");
    
    var post = new Post();
    post.set("comment", request.params.comment);
    post.set("postedBy", Parse.User.current());
    if (request.params.photo) {
        var query = new Parse.Query(Upload);
        query.get(request.params.photo).then(function(object) {
            post.set("photo", object.get("file"));
            savePost();
            object.destroy();
        });
    } else {
        savePost();
    }
    
    
    function savePost() {
        post.save().then(function(result) {
            response.success({code : 200, post_id : result.id});
        }, function(error) {
            response.success({code : 500, message : error});
        });
    }
    
});

Parse.Cloud.define("update_profile", function(request, response) {
    Parse.Cloud.useMasterKey();
    var Upload = Parse.Object.extend("Upload");
    var user = Parse.User.current();
    if (request.params.screen_name) {
        user.set('screen_name', request.params.screen_name);
    }
    if (request.params.avatar) {
        var query = new Parse.Query(Upload);
        query.get(request.params.avatar).then(function(object) {
            user.set('avatar', object.get("file"));
            saveUser();
            object.destroy();
        });
        //user.set('avatar', new Parse.File("avatar.png", {base64 : request.params.avatar}));
    } else {
        saveUser();
    }
    
    function saveUser() {
        user.save().then(function(res) {
            response.success({code : 200, message : "Successfully updated profile"});
        }, function(error) {
            response.success({code : 500, message : error});
        });
    }
});

Parse.Cloud.define("get_profile", function(request, response) {
    Parse.Cloud.useMasterKey();
    
    var user = Parse.User.current();
    response.success({code : 200, profile : {
        username : user.get('username'),
        screen_name : user.get('screen_name'),
        avatar : user.get('avatar') ? user.get('avatar').url() : null
    }});
    
});

Parse.Cloud.define("get_feed", function(request, response) {
    Parse.Cloud.useMasterKey();
    
    var Post = Parse.Object.extend('Post');
    var postQuery = new Parse.Query(Post);
    
    var friendQuery = new Parse.Query(Parse.User);
    friendQuery.equalTo("friends", Parse.User.current());
    
    
    postQuery.matchesQuery("postedBy", friendQuery);
    postQuery = Parse.Query.or(postQuery, (new Parse.Query(Post)).equalTo('postedBy', Parse.User.current()));
    postQuery.descending("createdAt");
    postQuery.include('postedBy');
    var out = [];
    
    postQuery.find().then(function(results) {
        for (var i=0; i<results.length; i++) {
            var post = results[i];
            out.push({
                post_id : post.get("objectId"),
                username : post.get("postedBy").get("username"),
                screen_name : post.get("postedBy").get("screen_name"),
                comment : post.get("comment"),
                photo : post.get("photo") ? post.get("photo").url() : null,
                avatar : post.get("postedBy").get("avatar") ? post.get("postedBy").get("avatar").url() : null,
                date_posted : post.createdAt.getTime()/1000
            });
        }
    }).then(function(obj) {
        response.success({code : 200, posts : out}); 
    }, function(error) {
        response.error(error);
    });
    
    
});

Parse.Cloud.define("find_users", function(request, response) {
    Parse.Cloud.useMasterKey();
    
    var Post = Parse.Object.extend('Post');
    var postQuery = new Parse.Query(Post);
    
    
    
    var userQuery = new Parse.Query(Parse.User);
    userQuery.startsWith("screen_name", request.params.screen_name);
    
    var userToMap = function(user) {
        return {
            username : user.get('username'),
            screen_name : user.get('screen_name'),
            avatar : user.get('avatar') ? user.get('avatar').url() : null,
            is_friend : 0,
            has_pending_invite : 0
        };
    };
    
    var out = [];
    var foundNames = {};
    Parse.User.current().relation("friends").query().each(function(friend) {
        var o = userToMap(user);
        foundNames[o.username] = o.username;
        o.is_friend = 1;
        out.push(o);
    })
    .then(function(result) {
        var pendingQuery = new Parse.Query(Parse.User);
        pendingQuery.equalTo("pendingFriendRequests", Parse.User.current());
        pendingQuery.each(function(user) {
            var o = userToMap(user);
            foundNames[o.username] = o.username;
            o.has_pending_invite = 1;
            out.push(o);
        });
    })
    .then(function(result) {
        return userQuery.each(function(user) {
            if (!foundNames[user.get('username')]) {
                out.push(userToMap(user));
            }
        });
    })
    .then(function(result) {
        response.success({code : 200, results : out});
    });
});




