package com.irkut.tc.io.session;

import com.teamcenter.schemas.soa._2006_03.exceptions.InvalidCredentialsException;
import com.teamcenter.schemas.soa._2006_03.exceptions.InvalidUserException;
import com.teamcenter.soa.client.CredentialManager;
import com.teamcenter.soa.exceptions.CanceledOperationException;

public class CredentialManagerImpl implements CredentialManager {

    @Override
    public int getCredentialType() {
        return CLIENT_CREDENTIAL_TYPE_STD;
    }

    @Override
    public String[] getCredentials(InvalidCredentialsException e) throws CanceledOperationException {
        return new String[0];
    }

    @Override
    public String[] getCredentials(InvalidUserException e) throws CanceledOperationException {
        return new String[0];
    }

    @Override
    public void setUserPassword(String s, String s1, String s2) {

    }

    @Override
    public void setGroupRole(String s, String s1) {

    }
}
