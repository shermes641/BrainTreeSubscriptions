package controllers;

import play.*;
import play.mvc.*;
import play.data.validation.*;

import java.math.BigDecimal;
import java.util.*;

import javax.swing.JOptionPane;

import com.braintreegateway.BraintreeGateway;
import com.braintreegateway.CreditCard;
import com.braintreegateway.CreditCardRequest;
import com.braintreegateway.CreditCardVerification;
import com.braintreegateway.Customer;
import com.braintreegateway.CustomerRequest;
import com.braintreegateway.Environment;
import com.braintreegateway.Result;
import com.braintreegateway.SubscriptionRequest;
import com.braintreegateway.Transaction;
import com.braintreegateway.TransactionRequest;
import com.braintreegateway.ValidationError;
import com.braintreegateway.ValidationErrors;

import models.*;

/**
 * Validates that user is logged in.<br>
 * Displays the user's purchases.<br>
 * Allows searching subscriptions and purchasing one.
 */
public class BrainTree extends Application {
	
	/**
	 * Define the BrainTree payment gateway<br>
	 * TODO enter real params when we get approved<br>
	 * TODO move config values to DB
	 */
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
     * URL to POST to for TR
     * @return		url string
     */
	public static String GetTransparentRedirectUrl(){
		return gateway.transparentRedirect().url();
	}
	
	/**
	 * Builds the TrDate field for Create CC TR transactions (put cc in the vault)
	 * @param sCustId		The The BrainTree customerId. See @code MakeCustomer 
	 * @param sRedirectUrl	The url BrainTree will redirect us to after we POST the TR
	 * @param bVerify		True means verify the card before placing it in the vault
	 * @return				trData string for the hidden tddata field 
	 */
	public static String GetTrDataCreateCC(String sCustId, String sRedirectUrl, boolean bVerify) {
		CreditCardRequest trParams = new CreditCardRequest().
			customerId(sCustId).options().
			verifyCard(bVerify).
			done();
		return gateway.trData(trParams, sRedirectUrl);
	}
	
	
	/**
	 * TODO test
	 * Builds the TrDate field for Create CC TR transactions (buy something using a cc in the vault)	 
	 * @param sToken		The vault cc token
	 * @param sAmount		The $ amount of the transaction
	 * @return				The result of the transaction
	 */
	public static Result<com.braintreegateway.Transaction> SaleTransaction(String sToken, String sAmount) {
		TransactionRequest request = new TransactionRequest().
			paymentMethodToken(sToken).
			amount(new BigDecimal(sAmount));

		Result<Transaction> result = null;
		try{
			result = gateway.transaction().sale(request);
    		DisplayResult(result);
    		return result;
    	} catch(Exception e) {
    		//JOptionPane.showMessageDialog(null, "Unable to reach "+e.getMessage()+"\nTry again later", "EXCEPTION", JOptionPane.WARNING_MESSAGE);
    		flash.error("Unable to reach "+e.getMessage()+"\nTry again later");
    		return result;
    	}
	}
	
	
	/**
	 * Purchase a subscription using a vault cc
	 * @param purchase	The purchase we are making
	 * @return			The results of the transaction
	 */
	public static Result<com.braintreegateway.Subscription> BuySubscription(Purchase purchase) {
    	//TODO hand in cc token bcr_cc
		purchase.token = "bcr_cc";
		purchase.plan = "Platinum";
    	SubscriptionRequest request = new SubscriptionRequest().
        paymentMethodToken(purchase.token).
        //TODO get subscription from purchase
        planId(purchase.plan);
    	
    	Result<com.braintreegateway.Subscription> result = null;
    	try{
    		result = BrainTree.gateway.subscription().create(request);
    		DisplayResult(result);
    		return result;
    	} catch(Exception e) {
    		//JOptionPane.showMessageDialog(null, "Unable to reach "+e.getMessage()+"\nTry again later", "EXCEPTION", JOptionPane.WARNING_MESSAGE);
    		flash.error("Unable to reach "+e.getMessage()+"\nTry again later");
    		return result;
    	}
	}

	/**
	 * Create a BrainTree customer
	 * @param user	The currently logged on user
	 * @return		Result of the transaction
	 */
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
    	
    /**********************************	
    	if (id == null)
    		id = (long) 1;
    	Subscription subscription = Subscription.findById(id);
    	Purchase purch = new Purchase(subscription, connected());
    	
    	//confirmCreditCard(purch);
       // render("@creditCard",purch);
    ***********************************/	
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

	static String DisplayResult(Result<?> result) {
		String ss = result.isSuccess()+"\n";
        
        if (!result.isSuccess()) {
        	for (ValidationError error : result.getErrors().getAllDeepValidationErrors()) {
        		ss += error.getCode()+": "+error.getMessage()+"\n";
        	}

        	Map<String, String> param = result.getParameters();
    		ss += param.toString();
        	flash.error(ss);
            JOptionPane.showMessageDialog(null, ss, result.isSuccess()+"", JOptionPane.WARNING_MESSAGE);
            return "";
        }        
        
        String sRes = result.getTarget().toString().toLowerCase();
        
        if (sRes.contains("com.braintreegateway.customer")) {
        	Result<com.braintreegateway.Customer> cust = (Result<com.braintreegateway.Customer>) result;
	        ss += cust.getTarget().getId()+"\n";
	        ss += cust.getTarget().getFirstName()+"\n";
	        ss += cust.getTarget().getLastName()+"\n";
	        ss += cust.getTarget().getPhone()+"\n";
        } else if (sRes.contains("com.braintreegateway.subscription")) {
        	Result<com.braintreegateway.Subscription> sub = (Result<com.braintreegateway.Subscription>) result;
        	ss += sub.getTarget().getId()+"\n";
            ss += sub.getTarget().getPaymentMethodToken()+"\n";
            ss += sub.getTarget().getPlanId()+"\n";
            ss += sub.getTarget().getPrice()+"\n";
            ss += sub.getTarget().getFirstBillingDate()+"\n";
            ss += sub.getTarget().getNextBillingDate()+"\n";
        } else if (sRes.contains("com.braintreegateway.creditcard")){
        	Result<com.braintreegateway.CreditCard> cc = (Result<com.braintreegateway.CreditCard>) result;
        	ss += cc.getTarget().getBin()+"\n";
	        ss += cc.getTarget().getToken()+"\n";
	        ss += cc.getTarget().getCreatedAt()+"\n";
	        ss += cc.getTarget().getUpdatedAt()+"\n";
	        //stick the token on the front
	        ss = cc.getTarget().getToken() + "\n" + ss;
        }
        flash.success(ss);
        JOptionPane.showMessageDialog(null, ss, result.isSuccess()+"", JOptionPane.INFORMATION_MESSAGE);
        return ss;
	}
}
