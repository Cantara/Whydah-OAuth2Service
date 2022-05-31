package net.whydah.service;

import static org.junit.Assert.assertTrue;

import java.net.URL;

import org.junit.Test;

import net.whydah.sso.ddd.model.user.UserName;

public class UserNameTest {

    @Test
    public void testUserName() {
        assertTrue(UserName.isValid("eivind@byx.digital"));
    }
    
    @Test
    public void testURL() {
    	try {
            URL url = new URL("SmartbuildingAuth://");
            url.toURI();
           
        } catch (Exception exception) {
           exception.printStackTrace();
        }
    }
}
