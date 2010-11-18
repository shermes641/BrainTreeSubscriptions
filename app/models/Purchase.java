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
    
    @Required
    @Temporal(TemporalType.DATE)
    public Date expireDate;
    
    //TODO get credit card from vault, if none in vault then have user save a cc to the vault
    @Required(message="Credit card number is required")
    @Match(value="^\\d{16}$", message="Credit card number must be numeric and 16 digits long")
    public String creditCard;
    
    @Required(message="Credit card name is required")
    @MinSize(value=3, message="Credit card name is required")
    @MaxSize(value=70, message="Credit card name is required")
    public String creditCardName;
    public int creditCardExpiryMonth;
    public int creditCardExpiryYear;
    //true = monthly renewal, else annual
    public boolean monthly;
    //TODO make this work
    //# of days till next renewal
    public int autoRenew;

    /*
     * Created when a user wants to purchase a subscription
     * INPUTS:
     * 	Subscription 	subscription	The selected subscription
     *  User 			user			The user (always the logged in user)
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
   
    public BigDecimal getTotal() {
        return subscription.price.multiply( new BigDecimal( getMonths() ) );
    }

    public int getMonths() {
        return (int) ( expireDate.getTime() - purchaseDate.getTime() ) / 1000 / 60 / 60 / 24;
    }

    public String getDescription() {
        DateFormat df = DateFormat.getDateInstance(DateFormat.MEDIUM);
        return subscription==null ? null : subscription.type + 
            ", " + df.format( purchaseDate ) + 
            " to " + df.format( expireDate );
    }

    public String toString() {
        return "Purchase(" + user + ","+ subscription + ")";
    }
}
