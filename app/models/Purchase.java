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
    @ManyToOne
    public Subscription subscription;
    
    @Required
    @Temporal(TemporalType.DATE) 
    public Date purchaseDate;
    public static String purchaseDateToolTip = "Required <br> Start date for Subscription<br>Select from calendar";
    	
    @Required
    @Temporal(TemporalType.DATE)
    public Date expireDate;
    public static String expireDateToolTip = "Required <br> Expiration date for Subscription<br>Select from calendar";
    
    //TODO get credit card from vault, if none in vault then have user save a cc to the vault
    @Required(message="Credit card number is required")
    @Match(value="^\\d{16}$", message="Credit card number must be numeric and 16 digits long")
    public String creditCard;
    public static String creditCardToolTip = "Required<br>Exactly 16 digits<br>Currently not validated as a valid card number";

    @Required(message="Credit card name is required")
    @MinSize(value=3, message="Credit card name is required")
    @MaxSize(value=70, message="Credit card name is required")
    public String creditCardName;
    public static String creditCardNameToolTip = "Required<br>3 to 70 chars<br>Currently not validated as a valid card ";

    public int creditCardExpiryMonth;
    public int creditCardExpiryYear;
    //true = monthly renewal, else annual
    public boolean monthly;
    //TODO make this work
    //# of days till next renewal
    public int autoRenew;

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
        return 1 + (int) ( expireDate.getTime() - purchaseDate.getTime() ) / 1000 / 60 / 60 / 24;
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
