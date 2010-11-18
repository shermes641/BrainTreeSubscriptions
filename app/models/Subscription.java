package models;

import play.*;
import play.data.validation.MaxSize;
import play.data.validation.Required;
import play.db.jpa.*;

import javax.persistence.*;

import java.math.BigDecimal;
import java.util.*;

@Entity
public class Subscription extends Model {
	   
    @Required
    @MaxSize(50)
    public String type;

    @Required
    @MaxSize(500)
    public String descr;
   
    @Required
    public int durationMonths;
    
    
    @Column(precision=6, scale=2)
    public BigDecimal price;

    public String toString() {
        return "Subscription(" + type + ","  + price + ")";
    }
}
