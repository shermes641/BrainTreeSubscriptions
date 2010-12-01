package models;

import play.db.jpa.*;
import play.data.validation.*;
import javax.persistence.*;
import java.util.*;
import java.text.*;
import java.math.*;

@Entity
public class Purchase extends Model {
    
    @Required
    @ManyToOne
    public User user;
    
    @Required
    public Subscription subscription;
    
    @Temporal(TemporalType.DATE) 
    public Date purchaseDate;
    public static String purchaseDateToolTip = "Required <br> Start date for Subscription<br>Select from calendar";
    	
    @Temporal(TemporalType.DATE)
    public Date expireDate;
    public static String expireDateToolTip = "Required <br> Expiration date for Subscription<br>Select from calendar";
    
    public int creditCardExpiryMonth;
    public int creditCardExpiryYear;
    //true = monthly renewal, else annual
    public boolean monthly;
    //TODO make this work
    //# of days till next renewal
    public int autoRenew;
    
    //BrainTree Transparent redirect vars
    public String transparentRedirectUrl;
    public String trData;
    public String ccResult;
    public String token;
    public String plan;

    /**
     *  Created when a user wants to purchase a subscription
     * @param subscription	the subscription being purchased
     * @param user			the currently logged in user
     */
    public Purchase(Subscription subscription, User user) {
    	if (subscription == null) {
    		
    	}
    	if (user == null) {
    		
    	}
    	//Even if the above is true set the Purchase items
    	this.subscription = subscription;
        this.user = user;
    }
   
    /**
     * Total cost of subscription
     * @return	monthly price of subscription * # of months for subscription
     */
    public BigDecimal getTotal() {
        return subscription.price.multiply( new BigDecimal( getMonths() ) );
    }

    /**
     * Calculate the # of months for the subscription
     * @return	At least 1 month.
     */
    public int getMonths() {
        //////////TODO enforce required dates
    	//return 1 + (int) ( expireDate.getTime() - purchaseDate.getTime() ) / 1000 / 60 / 60 / 24;
    	return 12;
    }

    /**
     * Short description of subscription
     * @return		type + purchaseDate + expireDate
     */
    public String getDescription() {
        DateFormat df = DateFormat.getDateInstance(DateFormat.MEDIUM);
        return subscription==null ? null : subscription.type + 
            ", " + df.format( purchaseDate ) + 
            " to " + df.format( expireDate );
    }

    /**
     * Return user and subscription for this purchase in string format
     */
    public String toString() {
        return "Purchase(" + user + ","+ subscription + ")";
    }
}
