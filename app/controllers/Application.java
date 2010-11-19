package controllers;

import play.mvc.*;
import play.data.validation.*;
import models.*;

public class Application extends Controller {
    
	/*
	 * Called when a user attempts to login.
	 * If a valid user add him to the rendering hash map
	 */
    @Before
    static void addUser() {
        User user = connected();
        if(user != null) 
            renderArgs.put("user", user);
    }
    
    /*
     * If a user is currently logged in, then return that user object,
     * otherwise if a user is currently logged in, return that user object
     */
    static User connected() {
        if(renderArgs.get("user") != null) 
            return renderArgs.get("user", User.class);
        
        String username = session.get("user");
        if(username != null) 
            return User.find("byUsername", username).first();
       
        return null;
    }
    
    // ~~

    /*
     * If a user is logged in then display the Subscriptions main page (Subscriptions index.html), 
     * otherwise display the application login page (Application index.html)
     */
    public static void index() {
        if(connected() != null) 
            Subscriptions.index();
        render();
    }
    
    /*
     * Display the register user page (Application register.html)
     */
    public static void register() {
        render();
    }
    
    /*
     * If the entered password matches the registered user then display the Subscriptions main page (Subscriptions index.html), 
     * otherwise display the register user page (Application register.html).
     */
    public static void saveUser(@Valid User user, String verifyPassword) {
        validation.required(verifyPassword);
        validation.equals(verifyPassword, user.password).message("Your password doesn't match");
        if(validation.hasErrors()) 
            render("@register", user, verifyPassword);
        
        user.save();
        session.put("user", user.username);
        flash.success("Welcome, " + user.name);
        Subscriptions.index();
    }
    
    /*
     * Find the first registered user that matches the login attempt.
     * If successful, save user name in session hashmap & display the Subscriptions main page (Subscriptions index.html), 
     * otherwise display the application login page (Application index.html).
     * INPUTS:
     * 		String username		Entered on login page 
     * 		String password		Entered on login page
     */		
    public static void login(String username, String password) {
        User user = User.find("byUsernameAndPassword", username, password).first();
        if(user != null) {
            session.put("user", user.username);
            flash.success("Welcome, " + user.name);
            Subscriptions.index();         
        }
        // Oops
        flash.put("username", username);
        flash.error("Login failed");
        index();
    }
    
    /*
     * Delete the session and goto login page (Application index.html)
     */
    public static void logout() {
        session.clear();
        index();
    }

}