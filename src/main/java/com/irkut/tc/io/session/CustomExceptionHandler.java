package com.irkut.tc.io.session;

import com.teamcenter.schemas.soa._2006_03.exceptions.ConnectionException;
import com.teamcenter.schemas.soa._2006_03.exceptions.InternalServerException;
import com.teamcenter.schemas.soa._2006_03.exceptions.ProtocolException;
import com.teamcenter.soa.client.ExceptionHandler;
import com.teamcenter.soa.exceptions.CanceledOperationException;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;

public class CustomExceptionHandler implements ExceptionHandler {

    public void handleException(InternalServerException internalServerExceptione) {
        System.out.println("");
        System.out.println("*****");
        System.out.println("Exception caught in com.teamcenter.clientx.AppXExceptionHandler.handleException(InternalServerException).");

        LineNumberReader reader = new LineNumberReader(new InputStreamReader(System.in));

        if (internalServerExceptione instanceof ConnectionException)
        {
            System.out.println("\nThe server returned an connection error. \n" + internalServerExceptione.getMessage()
            + "\nDo you wish to retry the last service request? [Y/N]");
        }
        else
            if (internalServerExceptione instanceof ProtocolException)
            {
            System.out.println("\nThe server returned protocol error. \n" + internalServerExceptione.getMessage()
                    + "\nThis is most likely the result of a programming error."
                    + "\nDo you wish to retry the last service request? [Y/N]");
            }
            else
            {
            System.out.println("\nThe server returned an internal server error. \n" + internalServerExceptione.getMessage()
                    + "\nThis is most likely the result of a programming error."
                    + "\nRuntimeException will be thrown.");
            throw new RuntimeException(internalServerExceptione.getMessage());
            }
        try
        {
            String retry = reader.readLine();
            if (retry.equalsIgnoreCase("y") || retry.equalsIgnoreCase("yes")) return;

            throw new RuntimeException("The user has opted not to retry the last request");
        }
        catch (IOException exception)
        {
            System.err.println("Failed to read user response. \nA RuntimeException will be thrown.");
            throw new RuntimeException(exception.getMessage());
        }
    }

    public void handleException(CanceledOperationException canceledOperationException) {
        System.out.println("");
        System.out.println("*****");
        System.out.println("Exception caught in com.teamcenter.clientx.AppXExceptionHandler.handleException(CanceledOperationException).");
        throw new RuntimeException(canceledOperationException);
    }
}
