package controllers;

import play.*;
import play.mvc.*;
import play.data.validation.*;

import java.util.*;
import javax.swing.JOptionPane;

import com.braintreegateway.BraintreeGateway;
import com.braintreegateway.CreditCardRequest;
import com.braintreegateway.Customer;
import com.braintreegateway.CustomerRequest;
import com.braintreegateway.Environment;
import com.braintreegateway.Result;
import com.braintreegateway.SubscriptionRequest;
import com.braintreegateway.ValidationErrors;

import models.*;

/**
 * Validates that user is logged in.<br>
 * Displays the user's purchases.<br>
 * Allows searching subscriptions and purchasing one.
 */
public class BrainTree extends Application {
	
	public static String GetTransparentRedirectUrl(){
		return gateway.transparentRedirect().url();
	}
	
	public static String GetCreditCardTrData(String sRedirectUrl) {
		CreditCardRequest trParams = new CreditCardRequest().
			customerId("637084")//.token("bcr_cc")  //steve allan
			;
		return gateway.trData(trParams, sRedirectUrl);
	}
	
	public static Result<com.braintreegateway.Subscription> BuySubscription(Purchase purchase) {
    	//bcr_cc
    	SubscriptionRequest request = new SubscriptionRequest().
        paymentMethodToken("bcr_cc").
        planId("Platinum");
    	
    	Result<com.braintreegateway.Subscription> result = null;
    	try{
    		result = BrainTree.gateway.subscription().create(request);
    		DisplayResult(result,purchase);
    		return result;
    	} catch(Exception e) {
    		//JOptionPane.showMessageDialog(null, "Unable to reach "+e.getMessage()+"\nTry again later", "EXCEPTION", JOptionPane.WARNING_MESSAGE);
    		flash.error("Unable to reach "+e.getMessage()+"\nTry again later");
    		return result;
    	}
	}

	public static Result<Customer> MakeCustomer(User user) {
        CustomerRequest request = new CustomerRequest().
        firstName(user.firstName).
        lastName(user.lastName).
        phone(user.phone);
        /******
        //actually all fields are optional
        company("Jones Co.").
        email("mark.jones@example.com").
        fax("419-555-1234").
        website("http://example.com");
        *******/
        
    	Result<Customer> result = null;
    	try{
    		result = BrainTree.gateway.customer().create(request);
    		DisplayResult(result);
    		return result;
    	} catch(Exception e) {
    		//JOptionPane.showMessageDialog(null, "Unable to reach "+e.getMessage()+"\nTry again later", "EXCEPTION", JOptionPane.WARNING_MESSAGE);
    		flash.error("Unable to reach "+e.getMessage()+"\nTry again later");
    		return result;
    	}

	}
	
	
	private static void DisplayResult(Result<com.braintreegateway.Subscription> result, Purchase purchase) {
		String ss = result.isSuccess()+"\n";
		
        if (!result.isSuccess()) {
        	ValidationErrors ve = result.getErrors();
        	for(int i = 0; i < ve.size();i++)
        		ss += ve.getAllValidationErrors().get(i)+"\n";
        	flash.error(ss);
            JOptionPane.showMessageDialog(null, ss, result.isSuccess()+"", JOptionPane.WARNING_MESSAGE);
        }
        
        ss += result.getTarget().getId()+"\n";
        ss += result.getTarget().getPaymentMethodToken()+"\n";
        ss += result.getTarget().getPlanId()+"\n";
        ss += result.getTarget().getPrice()+"\n";
        ss += result.getTarget().getFirstBillingDate()+"\n";
        ss += result.getTarget().getNextBillingDate()+"\n";
        flash.success(ss);
        JOptionPane.showMessageDialog(null, ss, result.isSuccess()+"", JOptionPane.INFORMATION_MESSAGE);
	}
	
	private static void DisplayResult(Result<Customer> result) {
		String ss = result.isSuccess()+"\n";
		
        if (!result.isSuccess()) {
        	ValidationErrors ve = result.getErrors();
        	for(int i = 0; i < ve.size();i++)
        		ss += ve.getAllValidationErrors().get(i)+"\n";
        	flash.error(ss);
            JOptionPane.showMessageDialog(null, ss, result.isSuccess()+"", JOptionPane.WARNING_MESSAGE);
        }
        
        ss += result.getTarget().getId()+"\n";
        ss += result.getTarget().getFirstName()+"\n";
        ss += result.getTarget().getLastName()+"\n";
        ss += result.getTarget().getPhone()+"\n";
        flash.success(ss);
        JOptionPane.showMessageDialog(null, ss, result.isSuccess()+"", JOptionPane.INFORMATION_MESSAGE);
	}


	static BraintreeGateway gateway = new BraintreeGateway(
            Environment.SANDBOX,
            "z94gpxrrf7k8nyzh",
            "pgcmsjwbn47scbd9",
            "xn33q2r2chttyxyb");

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
	    //	if (id == null)
	    //		return;
	    	if (id == null)
	    		id = (long) 1;
	    	Subscription subscription = Subscription.findById(id);
	    	Purchase purch = new Purchase(subscription, connected());
	    	
	    	//confirmCreditCard(purch);
	       // render("@creditCard",purch);
	    	render();
	    }
	    
	    
	    public static void confirmCreditCard(Purchase purch) {
	    	//flash.success("Thank you, %s, your confimation number for %s is %s", connected().firstName, purchase.subscription.type, purchase.id);
	    	flash.success("Thank you, %s, your confimation number for %s ", connected().firstName, purch.ccResult);
	    	//purch.ccResult = "Submitted  confirmCreditCard";
			index();
	    	//purch.id = "1";
			//buy(purch.id);
	    	//render(purch);
	    }

	    /**
	     * Completes purchase if data is valid.<br>
	     * Displays Subscriptions comfirmPurchase.html
	     * @param id	subscription key
	     * @param purchase	purchase object
	     */
		public static void creditCard() {
			Subscription subscription = Subscription.findById(1);
	    	Purchase purchase = new Purchase(subscription, connected());
	        validation.valid(purchase);
	        
	        // Errors or user wants to change something
	        if(validation.hasErrors() || params.get("revise") != null) {
	            render("@buy", purchase.subscription, purchase);
	        }
	        
	        // Confirm
	        if(params.get("confirm") != null) {

	        	Result<com.braintreegateway.Subscription> result = BrainTree.BuySubscription(purchase);
	        	if (result != null)
	        	if (result.isSuccess()) {
	        		purchase.save();
	        		flash.success("Thank you, %s, your confimation number for %s is %s", connected().firstName, purchase.subscription.type, purchase.id);
	        		index();
	        	}	
	        }
	        // Display purchase
	        render(purchase.subscription, purchase);
	    }

	    
	    
}
