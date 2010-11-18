package controllers;

import play.*;
import play.mvc.*;
import play.data.validation.*;

import java.util.*;
import javax.swing.JOptionPane;

import models.*;

public class Subscriptions extends Application {
    @Before
    static void checkUser() {
        if(connected() == null) {
            flash.error("Please log in first");
            Application.index();
        }
    }
    
    /*
     * 
     */
    public static void index() {
        List<Purchase> purchases = Purchase.find("byUser", connected()).fetch();
        render(purchases);
    }

    public static void list(String search, Integer size, Integer page) {
        List<Subscription> subscriptions = null;
        page = page != null ? page : 1;
        if(search.trim().length() == 0) {
        	subscriptions = Subscription.all().fetch(page, size);
        } else {
            search = search.toLowerCase();
            subscriptions = Subscription.find("lower(type) like ?", "%"+search+"%").fetch(page, size);
        }
        Integer cnt = 6;
        render(subscriptions, search, size, page, cnt);
    }
    
    public static void show(Long id) {
    	if (id == null)
    		return;
    	Subscription subscription = Subscription.findById(id);
        render(subscription);
    }
    
    public static void buy(Long id) {
    	Subscription subscription = Subscription.findById(id);
    	Purchase purchase = new Purchase(subscription,connected());
        render(purchase);
    }
    

	public static void confirmPurchase(Long id, Purchase purchase) {
		//these are already set ???
    	Subscription subscription = Subscription.findById(id);
    	purchase.subscription = subscription;
    	purchase.user = connected();
        validation.valid(purchase);
        
        // Errors or revise
        if(validation.hasErrors() || params.get("revise") != null) {
            render("@buy", purchase.subscription, purchase);
        }
        
        // Confirm
        if(params.get("confirm") != null) {
        	//if (purchase.id == null) 
        	//	purchase.id = (long) (purchase.subscription.id * (Math.random() * 100));
        	purchase.save();
            flash.success("Thank you, %s, your confimation number for %s is %s", connected().name, purchase.subscription.type, purchase.id);
            index();
        }
        
        // Display purchase
        render(purchase.subscription, purchase);
    }
    
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

