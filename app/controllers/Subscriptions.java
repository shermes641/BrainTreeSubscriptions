package controllers;

import play.*;
import play.mvc.*;
import play.data.validation.*;

import java.util.*;
import javax.swing.JOptionPane;

import com.braintreegateway.Result;
import com.braintreegateway.SubscriptionRequest;

import models.*;

/**
 * Validates that user is logged in.<br>
 * Displays the user's purchases.<br>
 * Allows searching subscriptions and purchasing one.
 */
public class Subscriptions extends Application {
	/**
	 * check that someone is logged in before rendering,<br>
	 * if not then return to login page (Application index.html)
	 */
    @Before
    static void checkUser() {
        if(connected() == null) {
            flash.error("Please log in first");
            Application.index();
        }
    }
    
    /**
     * Executes when index.html page loads and displays any purchased subscriptions the user has
     */
    public static void index() {
        List<Purchase> purchases = Purchase.find("byUser", connected()).fetch();
        render(purchases);
    }

    /**
     * Displays the available subscriptions based on search criteria.<br>
     * @param search 	string entered in textbox
     * @param size   	# of line items to display per page
     * @param page		determines subset of selected line items
     * 
     */
    public static void list(String search, Integer size, Integer page) {
        List<Subscription> subscriptions = null;
        page = page != null ? page : 1;
        if(search.trim().length() == 0) {
        	subscriptions = Subscription.all().fetch(page, size);
        } else {
            search = search.toLowerCase();
            subscriptions = Subscription.find("lower(type) like ?", "%"+search+"%").fetch(page, size);
        }
        long cnt = (Subscription.count()/size) + 1;
        render(subscriptions, search, size, page, cnt);
    }
    
    /**
     * Displays one subscription (line item from search list)<br>
     * Subscriptions show.html 
     * @param id key for db
     */
    public static void show(Long id) {
    	if (id == null)
    		return;
    	Subscription subscription = Subscription.findById(id);
        render(subscription);
    }
    
    /**
     * User selected a subscription to buy.<br>
     * Display Subscriptions buy.html, where user enters info to complete purchase.<br>
     * @param id 	key for db
     */
    public static void buy(Long id) {
    	Subscription subscription = Subscription.findById(id);
    	Purchase purchase = new Purchase(subscription,connected());
        render(purchase);
    }
    
    /**
     * Completes purchase if data is valid.<br>
     * Displays Subscriptions comfirmPurchase.html
     * @param id	subscription key
     * @param purchase	purchase object
     */
	public static void confirmPurchase(Long id, Purchase purchase) {
    	Subscription subscription = Subscription.findById(id);
    	purchase.subscription = subscription;
    	purchase.user = connected();
        validation.valid(purchase);
        
        // Errors or user wants to change something
        if(validation.hasErrors() || params.get("revise") != null) {
            render("@buy", purchase.subscription, purchase);
        }
        
        // Confirm
        if(params.get("confirm") != null) {

        	Result<com.braintreegateway.Subscription> result = BrainTree.BuySubscription(purchase);
        	if (result.isSuccess()) {
        		purchase.save();
        		flash.success("Thank you, %s, your confimation number for %s is %s", connected().firstName, purchase.subscription.type, purchase.id);
        		index();
        	}	
        }
        // Display purchase
        render(purchase.subscription, purchase);
    }
    
	/**
	 * User wants to delete a subscription.<br>
	 *  
	 * @param id 	purchase key
	 */
    public static void cancelPurchase(Long id) {
    	Purchase purchase = Purchase.findById(id);
        if (purchase == null){
            JOptionPane.showMessageDialog(null, "Can not find Subscription ID: " + id, "Subscription not Found", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int iRet = JOptionPane.showConfirmDialog(null, purchase.subscription.type +"  "+purchase.purchaseDate+ "  "+purchase.getMonths()+"\nAre you sure you want to Delete this Subscription ?", "Delete Subscription", JOptionPane.YES_NO_OPTION);
        if (iRet == JOptionPane.YES_OPTION) {
        	purchase.delete();
	        flash.success("Subscription cancelled for confirmation number %s", purchase.id);
        }
        index();
    }
    
    /**
     * 
     */
    public static void settings() {
        render();
    }
    
    public static void saveSettings(String password, String verifyPassword) {
        User connected = connected();
        connected.password = password;
        validation.valid(connected);
        validation.required(verifyPassword);
        validation.equals(verifyPassword, password).message("Your password doesn't match");
        if(validation.hasErrors()) {
            render("@settings", connected, verifyPassword);
        }
        connected.save();
        flash.success("Password updated");
        index();
    }
    
}

