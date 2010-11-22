package controllers;

import play.*;
import play.db.jpa.*;
import play.i18n.Messages;

import javax.persistence.*;
import java.util.*;

@Entity
public class UserMessages extends Model {
	public static String lang = null; 
	public static Messages messages = null;
    public UserMessages(String lang, boolean b) {
    	messages = new Messages();
		this.lang = lang;
		getMessages(lang, true);
	}
	/**
     * Load up the messages in the newLang language. Does not load messages if the language has not changed, and bRefresh is false
     * @param newLang	The i18n country code fr, en, fr_FR 
     * @param bRefresh  If true we will load the the messages.
     * @return			true if language has not changed or we loaded the messages. false means we load 'en' messages, or failed
     *                 
     */
    public static boolean getMessages(String newLang, boolean bRefresh) {
        try{
        	if (messages == null) {
        		messages = new Messages();
        		bRefresh = true;
        	}
        	if (lang == null) {
        		lang = "en";
        		bRefresh = true;
        	}
        	if (newLang == null)
        		newLang = "en";
        	if ((!lang.equals(newLang)) || bRefresh) {	
        		lang = newLang;
        		play.i18n.Lang.set(newLang);
        	}
        } catch(Exception e) {
        	try{
        		play.i18n.Lang.set("en");
        		lang = "en";
        	} catch(Exception e1) {
        		return false;
        	}
        	return false;
        }	
        return true;
   }

}
