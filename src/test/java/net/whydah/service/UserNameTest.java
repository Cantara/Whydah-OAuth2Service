package net.whydah.service;

import net.whydah.sso.ddd.model.user.UserName;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class UserNameTest {

    @Test
    public void testUserName() {
        assertTrue(UserName.isValid("eivind@byx.digital"));
    }
}
