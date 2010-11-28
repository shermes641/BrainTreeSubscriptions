package controllers;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JOptionPane;

import com.braintreegateway.BraintreeGateway;
import com.braintreegateway.Customer;
import com.braintreegateway.CustomerRequest;
import com.braintreegateway.Environment;
import com.braintreegateway.Result;
import com.braintreegateway.ValidationErrors;

import play.Play;
import play.i18n.Messages;
import play.mvc.*;
import play.data.validation.*;
import play.data.validation.Error;
import models.*;

/**
 * This app is designed to be stand alone or accessed from other websites.<br>
 * It uses the BrainTree payment service via the JAVA API<br>
 * Currently supports:<br>
 * 1) 	Registering Customers <br>
 * 2)   Entering credit card info into the BrainTree vault. <br>
 * 3)   Purchasing subscriptions using vault credit card.
 * <br><br>!!!!!!!!!!!! NO CC INFO IS STORED IN THIS APP !!!!!!!!!!!!!!!<br>
 * This makes PCI compliance very simple.<br><br>
 * Typical use would be for a website to log in a user and use this site to add customers and make purchases via BrainTree.<br>
 * The website passes the username as an html parameter and this app links subscription purchases to the user.<br>
 * If a "username" param is passed, we know a user is logged in. If the user has a customerId then he is a BrainTree customer. <br>
 * Play supports sharing data models enabling this app to access user data from the website,<br>
 * and the website to access subscription data.
 * <br><br>
 * To support stand alone operation this app can create users and uses the built in PLAY user authentication.<br.<br>
 * 
 * TODO:<br>
 * 1)	Transaction history<br>
 * 2)	Multiple credit cards<br>
 * 3)	Switch to MySQL db<br>
 * 
 */


public class Application extends Controller {
	
	public static final boolean PROD_MODE 	= Play.id == "prod"? true : false;
	public static final String ACTION 		= "action";
	public static final String BUY 			= "buy";
	public static final String REGISTER 	= "register";

	public static final String SUB_ID 		= "subId";
	public static final String SUB_TYPE 	= "subType";
	public static final String SUB_DESC 	= "subDescr";

	public static final String CUSTOMER 	= "customerId";
	public static final String USER 		= "user";
	public static final String LOGOUT 		= "logOut";
	
	public static final String FLASH 		= "flash";

	
	/**
	 * Called to validate if a user is logged in.<br>
	 * If a username html param was passed in then he is validated and logged in.<br>
	 * If true rendering hash map.
	 */
	@Before
    static void addUser() {
		String sCust = session.get(CUSTOMER);
        User user = connected();
        if(user != null) {
        	if ((user.customerId == null) && sCust != null)
        		user.customerId = sCust;
        	renderArgs.put(USER, user);
        } else {
        	//html param passed, if existing user then login
        	String username = Http.Request.current().querystring;				//gets from cookie ?????? params.get("username");
        	if (username != null && username != ""){
        		String[] ss = username.split("=");
        		if (ss.length < 1)
        			return;
        		if (ss[0].toLowerCase().equals("username"))
        			username = ss[1];
        		user = User.find("byUsername", username).first();
        		if(user != null) {
                	if ((user.customerId == null) && sCust != null)
                		user.customerId = sCust;
        			session.put(CUSTOMER, user.customerId);
                    renderArgs.put(USER, user);
                    session.put(USER, user.userName);
                    flash.success("Welcome, " + user.firstName);
                    Subscriptions.index();
        		}    
        	}
        }	
    }
    
    /**
     * If a user is currently logged in, then return that user object from the db,<br>
     * Users get validated at the page level and the session level.<br>
     * @return		Logged in user or null if no user validated.
     */
    static User connected() {
    	User user = null;
    	user = renderArgs.get(USER, User.class);
        if(user != null) {
            return user;
        }
    	
        if(session.get(LOGOUT) != null) 
            session.put(LOGOUT, null);

        String username = session.get(USER);
        if(username != null) {
            return User.find("byUsername", username).first();
        } 
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
        List<Subscription> subscriptions = null;
       	subscriptions = Subscription.all().fetch();
       	Subscription subPlat = subscriptions.get(0);
       	Subscription subGold = subscriptions.get(1);
       	Subscription subSilv = subscriptions.get(2);
       	render(subPlat,subGold,subSilv);
    }
    
    public static void buySubcription(Subscription sub) {
		if(connected() != null) 
			BrainTree.index();

		Subscriptions.SubEnum type = Subscriptions.subType.plat12;
		
		if (sub.descr.contains("Platinum") ) 
			type = Subscriptions.subType.plat12;

		if (sub.descr.contains("Gold")  ) 
			type = Subscriptions.subType.gold12;

		if (sub.descr.contains("Silver")  ) 
			type = Subscriptions.subType.silv12;

		session.put(ACTION, BUY);
		session.put(SUB_DESC, sub.descr);
		session.put(SUB_ID, type.ordinal());
    	switch(type) {
    		case silv12:
    			session.put(SUB_TYPE, type.toString());
   				flash.success("Register with our Subscription service before purchasing the Silver Subscription");
   				register(connected());
    			break;
    		case gold12:	
    			session.put(SUB_TYPE, type.toString());
   				flash.success("Register with our Subscription service before purchasing the Gold Subscription");
   				register(connected());
    			break;
    		case plat12:
    			session.put(SUB_TYPE, type.toString());
   				flash.success("Register with our Subscription service before purchasing the Platinum Subscription");
   				register(connected());
    			break;
    		default:
    			flash.error("This never happened to me before");
    			session.clear();
    			index();
    			break;
    	}
    }
    /**
     * Display the register user page (Application register.html)
     * @param user 	data to fill form in
     */
    public static void register(User user) {
        //render();
    	User connected = connected();
    	if (connected != null) {
    		user.copy(connected);
    		user.id = connected.id;
    	}	
    	if (user.website == null)
    		user.website = "http://";
    	if (user.lang == null)
    		user.lang = "en";
    	session.put(REGISTER, REGISTER);
        render("@register", user);
    }
    
    /**
     * Called when the register user page posts.<br>
     * If the entered password matches the registered user then display the Subscriptions main page (Subscriptions index.html),<br> 
     * otherwise display the register user page (Application register.html).<br>
     * @param user 				user object from db
     * @param phonenum			phone number entered by user
     */
    public static void saveUser(@Valid User user, String phonenum) {
        validation.required(phonenum);
        if (!isPhoneNumberValid(phonenum))
        	validation.equals(phonenum, "").message("Phone number format incorrect");
        if(validation.hasErrors()) {
            //JOptionPane.showMessageDialog(null, "all: " + validation.errors().toString(), "Bad Input", JOptionPane.WARNING_MESSAGE);
        	render("@register", user, phonenum);
        } else
        	user.phone = phonenum;

        boolean bSuccess = false;
        
        if (PROD_MODE) {
	        Result<Customer> result = BrainTree.MakeCustomer(user);
	        if (result != null)
	        	bSuccess = result.isSuccess();  
	        if (bSuccess)
	        	user.customerId = result.getTarget().getId();
        } else {
        	user.customerId = "666";
        	bSuccess = true;
        }
        
	    if (bSuccess) {
	    	session.put(REGISTER, null);
	    	user.setMessages(true);
	        session.put(CUSTOMER, user.customerId);
	        session.put(USER, user.userName);
	        User connected = connected();
	        if (connected != null) {
	        	connected.UserCopy(user,connected);
	        	connected.save();
	        } else {
	        	user.save();
	        }

	        flash.success("Welcome, " + user.firstName+"    "+UserMessages.messages.get("0", "  ID: "+session.get(CUSTOMER)));
	        Subscriptions.index();
	    }  
	    render("@register", user, phonenum);


    }
    
    /** isPhoneNumberValid: Validate phone number using Java reg ex. 
    * This method checks if the input string is a valid phone number. 
    * @param phoneNumber 	Phone number to validate 
    * @return boolean: 		true if phone number is valid, false otherwise. 
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
        if(user == null) {
        	user = User.find("byUsername", username).first();
        	if(user == null) 
        		flash.error("Login Failed: user %s  Does not exist",username);
        	else 
        		flash.error("Login Failed: Password incorrect");
        	user = null;
        }
        if(user != null) {
        	session.put(ACTION, 	null);
        	session.put(REGISTER, 	null);
        	session.put(SUB_DESC, 	null);
        	session.put(SUB_ID, 	null);
            
        	session.put(USER, 		user.userName);
            session.put(CUSTOMER, 	user.customerId);
            renderArgs.put(USER, 	user);
            flash.success("Welcome, %s  cust ID: %s", user.firstName, user.customerId);
            Subscriptions.index();         
        }
        index();
    }
    
    /**
     * Delete the session and go to login page (Application index.html)
     */
    public static void logout() {
        session.clear();
        session.put(LOGOUT, LOGOUT);
        index();
    }

}