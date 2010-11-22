package models;

import play.db.jpa.*;
import play.data.validation.*;

import javax.persistence.*;

@Entity
@Table(name="Customer")

public class User extends Model {
    

	//BrainTree will generate this
	public String customerId = "";
	public String toolTips = "Show/Hide ToolTips";
	
	//These fields are provided by the user when he registers or updates his info
    @Required
    @MaxSize(25)
    @MinSize(4)
    @Match(value="^\\w*$", message="Not a valid username")
    public String userName;
    public static String userNameValidate = "Required <br> 4 to 25 chars <br> Can not contain ^ \\ w* $";

    @Required
    @MaxSize(15)
    @MinSize(5)
    public String password;
    public static String passwordValidate = "Required <br> 5 to 15 chars";
    
	@Required
    @MaxSize(30)
    public String firstName;
    
	@Required
    @MaxSize(30)
    public String lastName;
    public static String nameValidate = "Required <br> Max 30 chars";

	@Required
    @MaxSize(50)
    @Email(message="Email format incorrect") 
    public String email;
    public static String emailValidate = "Required <br> Max 50 chars <br> Valid format name@website.com (.net etc..)";

	//Required but we do a custom validation in the Application controller
    @MaxSize(25)
	public String phone;
    public static String phoneValidate = "Required <br> Phone Number formats: (nnn)nnn-nnnn; nnnnnnnnnn; nnn-nnn-nnnn";

    //Not required but we do a custom validation in the Application controller
    @MaxSize(25)
	public String cellPhone;
    public static String nrPhoneValidate = "Not Required <br> Phone Number formats: (nnn)nnn-nnnn; nnnnnnnnnn; nnn-nnn-nnnn";
    
    @MaxSize(50)
	public String addrLine1;
    @MaxSize(50)
	public String addrLine2;
    public static String addrLineValidate = "Not Required <br> Max 50 chars";
    
    @MaxSize(50)
    public String company;
    public static String companyValidate = "Not Required <br> Max 50 chars";

    @MaxSize(25)
	public String fax;

    @MaxSize(255)
	@URL
    public String website;
    public static String websiteValidate = "Not Required <br> Max 255 chars <br> Must start with http://";

    @MaxSize(255)
    public String notes;
    public static String notesValidate = "Not Required <br> Max 255 chars";
   
    /**
     *	BrainTree uses most of the user fields.<br>
     *  Alisting of error codes follows.<br><br>
     * 
     *  
     * @param customerId	Unique identifier for user<br>
     * 	91609 Customer ID has already been taken.  Customer IDs have to be unique.<br> 
     * 	91610 Customer ID is invalid. Valid characters are letters, numbers, - and _.<br>
     * 	91611 Customer ID is not an allowed ID. We reserve a few words that can’t be used as IDs. “all” and “new” currently cannot be used.<br> 
     * 	91612 Customer ID is too long. Must be less than or equal to 36 characters.<br><br>
     * @param firstName		<br>
     * 	81608 First name is too long. Must be less than or equal to 255 characters.<br><br>
     * @param lastName		<br>
     * 	81613 Last name is too long. Must be less than or equal to 255 characters.<br><br>
     * @param company		<br>
     * 	81601 Company is too long. Must be less than or equal to 255 characters.<br><br>
     * @param email			<br>
     * 	81604 Email is an invalid format. Email must be a well-formed email address. <br>
     * 		If you are migrating from a system that does not have this constraint and want to record the email address in the vault, you can use a custom field.<br> 
     * 	81605 Email is too long. Must be less than or equal to 255 characters.<br><br>
     * 	81606 Email is required if sending a receipt.<br> This only applies when creating a transaction. <br>
     * 		If you specify that you want to send a receipt then the customer email will be required.<br><br>
     * @param phone			<br>
     * 	81614 Phone is too long. Must be less than or equal to 255 characters.<br><br>
     * @param fax			<br>
     * 	81607 Fax is too long. Must be less than or equal to 255 characters.<br><br>
     * @param website		<br>
     * 	81615 Website is too long. Must be less than or equal to 255 characters.<br><br>
     *	81616 Website is an invalid format.Website must be well-formed. The http:// at the beginning is optional.<br>
     *  	If you want to provide websites that may be not well-formed you can use a custom field.<br><br>
     * @param username		<br>Not used by BrainTree<br>
     * @param password		<br>Not used by BrainTree<br>
     * @param notes			<br>User entered, free form<br>
     * 	91602 Custom field is invalid.<br>
     * 		Custom field keys must match the API name of a custom field configured in the control panel. <br>
     * 		The error message for this validation error will contain a list of the invalid keys.<br>
     * 	81603 Custom field is too long. Custom field values must be less than or equal to 255 characters. <br>
     * 		The error message for this validation error will contain a list of the custom fields that were too long.<br><br>
     */
    public User(String customerId,
    			String firstName,
    			String lastName,
    			String company,
    			String email,
    			String phone,
    			String fax,
    			String website,
    			String userName,
    			String password,
    			String notes) {
    	this.customerId 			= customerId;
        this.firstName 				= firstName;
        this.lastName 				= lastName;
        this.company				= company;
        this.email					= email;
        this.phone					= phone;
        this.fax					= fax;
        this.website				= website;
        this.userName 				= userName;
        this.password 				= password;
        this.notes	 				= notes;
    }

    public String toString()  {
        return "User(" + customerId + ","+firstName+","+lastName+","+company+","+email+","+phone+","+fax+","+website+","+userName+","+notes+")";
    }
    
}
