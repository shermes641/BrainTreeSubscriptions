package controllers;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JOptionPane;

import com.braintreegateway.BraintreeGateway;
import com.braintreegateway.Customer;
import com.braintreegateway.CustomerRequest;
import com.braintreegateway.Environment;
import com.braintreegateway.Result;
import com.braintreegateway.ValidationErrors;

import play.mvc.*;
import play.data.validation.*;
import play.data.validation.Error;
import models.*;

/**
 * This app is designed to be stand alone or accessed from other websites.<br>
 * It uses the BrainTree payment service via the JAVA API<br>
 * Currently supports Entering credit card info and purchasing subscriptions.
 * <br><br>!!!!!!!!!!!! NO CC INFO IS STORED IB THIS APP !!!!!!!!!!!!!!!<br>
 * This makes PCI compliance very simple.<br><br>
 * Typical use would be for a website to use this site to purchase subscriptions.<br>
 * Login would be handled by the website and this app would link subscription purchases to the user.<br>
 * Play supports sharing data models enabling this app to access user data from the website,<br>
 * and the website to access subscription data.
 * <br><br>
 * To support stand alone operation this app can create users and uses the built in PLAY user authentication.
 * 
 */
public class Application extends Controller {
    
	/**
	 * Called when a user attempts to login.<br>
	 * If a valid user add him to the rendering hash map
	 */
    @Before
    static void addUser() {
        User user = connected();
        if(user != null) 
            renderArgs.put("user", user);
    }
    
    /**
     * If a user is currently logged in, then return that user object,<br>
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

    /**
     * If a user is logged in then display the Subscriptions main page (Subscriptions index.html),<br> 
     * otherwise display the application login page (Application index.html)
     */
    public static void index() {
        if(connected() != null) 
            Subscriptions.index();
        render();
    }
    
    /**
     * Display the register user page (Application register.html)
     * @param user 
     */
    public static void register(User user) {
        //render();
    	user.userName = "";
    	user.password = "";
    	if (user.toolTips == null)
    		user.toolTips = "Hide ToolTips";
        render("@register", user);
    }
    public static void setToolTip(User user) {
        //render();
    	user.userName = "";
    	user.password = "";
    	if (user.toolTips == null)
    		user.toolTips = "Show ToolTips";
    	if (user.toolTips.equals("Show ToolTips"))
    		user.toolTips = "Hide ToolTips";
    	else
    		user.toolTips = "Show ToolTips";
        render("@register", user);
    }
    
    /**
     * If the entered password matches the registered user then display the Subscriptions main page (Subscriptions index.html),<br> 
     * otherwise display the register user page (Application register.html).<br>
     * @param user 				user object from db
     * @param verifyPassword	pw enter by user
     */
    public static void saveUser(@Valid User user, String verifyPassword, String phonenum) {

    	validation.required(verifyPassword);
        validation.equals(verifyPassword, user.password).message("Your passwords don't match");

        validation.required(phonenum);
        if (!isPhoneNumberValid(phonenum))
        	validation.equals(phonenum, "").message("Phone number format incorrect");
        if(validation.hasErrors()) {
        	try{
        	if (user.toolTips == null)
        		user.toolTips = "Hide ToolTips";
        	} catch(Exception e){
        		user.toolTips = "Hide ToolTips";	
        	}
            //JOptionPane.showMessageDialog(null, "all: " + validation.errors().toString(), "Bad Input", JOptionPane.WARNING_MESSAGE);
        	render("@register", user, phonenum);
        } else
        	user.phone = phonenum;

        Result<Customer> result = BrainTree.MakeCustomer(user);
      
        if (result != null)
	    if (result.isSuccess()) {
	        user.customerId = result.getTarget().getId();
	        user.save();
	        session.put("user", user.userName);
	        //flash.success("Welcome, " + user.firstName);
	        Subscriptions.index();
	    }  
        user.toolTips = "Hide ToolTips";
        render("@register", user, phonenum, verifyPassword);

    }
    
    /** isPhoneNumberValid: Validate phone number using Java reg ex. 
    * This method checks if the input string is a valid phone number. 
    * @param email String. Phone number to validate 
    * @return boolean: true if phone number is valid, false otherwise. 
    */
    public static boolean isPhoneNumberValid(String phoneNumber){  
    	boolean isValid = false;  
    	/* Phone Number formats: (nnn)nnn-nnnn; nnnnnnnnnn; nnn-nnn-nnnn 
    	    ^\\(? : May start with an option "(" . 
    	    (\\d{3}): Followed by 3 digits. 
    	    \\)? : May have an optional ")" 
    	    [- ]? : May have an optional "-" after the first 3 digits or after optional ) character. 
    	    (\\d{3}) : Followed by 3 digits. 
    	     [- ]? : May have another optional "-" after numeric digits. 
    	     (\\d{4})$ : ends with four digits. 
    	 
    	         Examples: Matches following <a href="http://mylife.com">phone numbers</a>: 
    	         (123)456-7890, 123-456-7890, 1234567890, (123)-456-7890 
    	 
    	*/  
    	//Initialize reg ex for phone number.   
    	String expression = "^\\(?(\\d{3})\\)?[- ]?(\\d{3})[- ]?(\\d{4})$";  
    	CharSequence inputStr = phoneNumber;  
    	Pattern pattern = Pattern.compile(expression);  
    	Matcher matcher = pattern.matcher(inputStr);  
    	if(matcher.matches()){  
    	isValid = true;  
    	}  
    	return isValid;  
    	}
    
    
    /**
     * Find the first registered user that matches the login attempt.<br>
     * If successful, save user name in session hashmap & display the Subscriptions main page (Subscriptions index.html),<br> 
     * otherwise display the application login page (Application index.html).
     * @param		username		Entered on login page 
     * @param		password		Entered on login page
     */		
    public static void login(String username, String password) {
        User user = User.find("byUsernameAndPassword", username, password).first();
        if(user != null) {
            session.put("user", user.userName);
            flash.success("Welcome, " + user.firstName);
            Subscriptions.index();         
        }
        // Oops
        flash.put("username", username);
        flash.error("Login failed");
        index();
    }
    
    /**
     * Delete the session and go to login page (Application index.html)
     */
    public static void logout() {
        session.clear();
        index();
    }

}