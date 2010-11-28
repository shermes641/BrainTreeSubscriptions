package controllers;

import play.*;
import play.mvc.*;
import play.mvc.Http.Request;
import play.data.validation.*;

import java.util.*;

import javax.swing.JOptionPane;

import com.braintreegateway.CreditCard;
import com.braintreegateway.CreditCardRequest;
import com.braintreegateway.Result;
import com.braintreegateway.SubscriptionRequest;

import models.*;

/**
 * Validates that user is logged in.<br>
 * Displays the user's purchases.<br>
 * Allows searching subscriptions and purchasing one.
 */
public class Subscriptions extends Application {

	public static enum SubEnum {
		nada, plat12, gold12, silv12, plat1, gold1, silv1
	};

	public static SubEnum subType;

	/**
	 * check that someone is logged in before rendering,<br>
	 * if not then return to login page (Application index.html)
	 */
	@Before
	static void checkUser() {
		if (connected() == null) {
			flash.error("Please log in first");
			Application.index();
		}
	}

	/**
	 * Executes when index.html page loads and displays any purchased
	 * subscriptions the user has
	 */
	public static void index() {
		if (params.get("cc") != null) {
			flash.success(params.get("cc") + "    credit card");
		} else {
			if (flash.get("success") == null)
				flash.success("Welcome %s", session.get(USER));
		}
		String ss = session.get(ACTION);
		String st = session.get(SUB_ID);
		if ((ss != null) && ss.equals(BUY)) {
			ss = session.get(SUB_DESC);
			if (st != null)
				flash.success("Now we need a valid credit card  %s", st);
			buy(Long.parseLong(session.get(SUB_ID)), ss);
		}
		List<Purchase> purchases = Purchase.find("byUser", connected()).fetch();
		render(purchases);
	}

	/**
	 * Displays the available subscriptions based on search criteria.<br>
	 * 
	 * @param search
	 *            string entered in textbox
	 * @param size
	 *            # of line items to display per page
	 * @param page
	 *            determines subset of selected line items
	 * 
	 */
	public static void list(String search, Integer size, Integer page) {
		List<Subscription> subscriptions = null;
		page = page != null ? page : 1;
		if (search.trim().length() == 0) {
			subscriptions = Subscription.all().fetch(page, size);
		} else {
			search = search.toLowerCase();
			subscriptions = Subscription.find("lower(type) like ?",
					"%" + search + "%").fetch(page, size);
		}
		long cnt = (Subscription.count() / size) + 1;
		render(subscriptions, search, size, page, cnt);
	}

	/**
	 * Displays one subscription (line item from search list)<br>
	 * Subscriptions show.html
	 * 
	 * @param id
	 *            key for db
	 */
	public static void show(Long id) {
		/*********** TODO remove redirect was not working, but is working now
		if (Http.Request.current().url.contains("/subscriptions/verifyCC")) 
			verifyCC();
		**************************/	
		//TODO not sure if I should render() or set id to a sub id
		if (id == null)
			return;
		Subscription subscription = Subscription.findById(id);
		render(subscription);
	}

	/**
	 * User selected a subscription to buy.<br>
	 * Display Subscriptions buy.html, where user enters info to complete
	 * purchase.<br>
	 * 
	 * @param subId
	 *            id of subscription in db
	 */
	public static void buy(Long subId, String subDescr) {
		Subscription subscription = Subscription.findById(subId);
		Purchase purch = new Purchase(subscription, connected());
		purch.user.customerId = session.get("customerId");
		if ((purch.user.customerId == null)
				|| (purch.user.customerId.equals("-999"))) {
			flash.error("You must register with our payment service before making purchases");
			register(purch.user);
		}
		purch.trData = BrainTree.GetTrDataCreateCC(purch.user.customerId,
				"localhost:8888/subscriptions/verifyCC", false);
		// BrainTree.GetCreditCardTrData("http://184.106.128.160:8888/subscriptions/1/buy");

		if (PROD_MODE)
			purch.transparentRedirectUrl = BrainTree.GetTransparentRedirectUrl();
		else
			purch.transparentRedirectUrl = "/subscriptions/verifyCC";
			
		purch.ccResult = "Not Submitted";
		purch.id = subId;
		purch.plan = purch.subscription.type;
		render(purch, Play.id);
	}

	public static void verifyCC() {
		String sParams = Http.Request.current().querystring;
		flash.success("verifyCC " + sParams);
		Result<CreditCard> result = null;
		String sToken = "";
		if (PROD_MODE) {
			if ((sParams != null) && (sParams != "")) {
				result = BrainTree.gateway.transparentRedirect().confirmCreditCard(sParams);
			} else {
				flash.put(FLASH,"querystring was empty, this is a problem");
			}
		} else {
			//TODO remove this as we will never do S2S credit card stuff
			/****************************************
			CreditCardRequest request = new CreditCardRequest()
					.customerId("565084").number("5555555555554444")
					.expirationDate("05/2012");
			result = BrainTree.gateway.creditCard().create(request);
			*****************************************/
			result = new Result();
		}

		try {
			sToken = BrainTree.DisplayResult(result);
			String[] sParse = sToken.split("\n");
			if (sParse.length > 0)
				sToken = sParse[0];
			else {
				if (result != null)
					flash.error(result.toString()+"\nSomething went wrong token:\n%s", sToken);
				else
					flash.error("Result null\nSomething went wrong token:\n%s", sToken);

			}
		} catch (Exception e) {
			flash.error("Unable to reach " + e.getMessage()
					+ "\nTry again later");
			render();
		}
		if (result.isSuccess()) {
			if (PROD_MODE)
				sParams = "Your credit card is securely stored.";
			flash.success("Thank you, %s\n%s ", connected().firstName, sParams);
			// purch.ccResult = "Submitted  verifyCC";
			render("@verifyCC", sParams, sToken);
		}
	}

	/**
	 * Completes purchase if data is valid.<br>
	 * Displays Subscriptions comfirmPurchase.html
	 * 
	 * @param id
	 *            subscription key
	 * @param purchase
	 *            purchase object
	 */
	public static void confirmPurchase(Long id, Purchase purchase) {
		Subscription subscription = Subscription.findById(id);
		purchase.subscription = subscription;
		purchase.user = connected();
		validation.valid(purchase);

		// Errors or user wants to change something
		if (validation.hasErrors() || params.get("revise") != null) {
			render("@buy", purchase.subscription, purchase);
		}

		// Confirm
		if (params.get("confirm") != null) {

			Result<com.braintreegateway.Subscription> result = BrainTree
					.BuySubscription(purchase);
			if (result != null)
				if (result.isSuccess()) {
					purchase.save();
					flash.success(
							"Thank you, %s, your confimation number for %s is %s",
							connected().firstName, purchase.subscription.type,
							purchase.id);
					index();
				}
		}
		// Display purchase
		render(purchase.subscription, purchase);
	}

	/**
	 * User wants to delete a subscription.<br>
	 * 
	 * @param id
	 *            purchase key
	 */
	public static void cancelPurchase(Long id) {
		Purchase purchase = Purchase.findById(id);
		if (purchase == null) {
			JOptionPane.showMessageDialog(null,
					"Can not find Subscription ID: " + id,
					"Subscription not Found", JOptionPane.WARNING_MESSAGE);
			return;
		}
		int iRet = JOptionPane
				.showConfirmDialog(
						null,
						purchase.subscription.type
								+ "  "
								+ purchase.purchaseDate
								+ "  "
								+ purchase.getMonths()
								+ "\nAre you sure you want to Delete this Subscription ?",
						"Delete Subscription", JOptionPane.YES_NO_OPTION);
		if (iRet == JOptionPane.YES_OPTION) {
			purchase.delete();
			flash.success("Subscription cancelled for confirmation number %s",
					purchase.id);
		}
		index();
	}

	/**
	 * Displays the settings page
	 */
	public static void settings() {
		render();
	}

	/**
	 * Currently only changes the password
	 * 
	 * @param password
	 *            New password
	 * @param verifyPassword
	 *            Must match password
	 */
    public static void saveSettings(String password, String verifyPassword) {
        User user = connected();
        user.password = password;
        validation.valid(user);
        validation.required(verifyPassword);
        validation.equals(verifyPassword, password).message("Your password doesn't match");
        if(validation.hasErrors()) {
            render("@settings", user, verifyPassword);
        }
        user.save();
        flash.success("Password updated");
        index();
    }
}
