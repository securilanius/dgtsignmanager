package com.irkut.tc.io.session;

import com.teamcenter.soa.client.model.ErrorStack;
import com.teamcenter.soa.client.model.ErrorValue;
import com.teamcenter.soa.client.model.PartialErrorListener;

public class CustomPartialErrorListener implements PartialErrorListener {


    @Override
    public void handlePartialError(ErrorStack[] stacks) {

        if(stacks.length == 0) return;

        System.out.println("");
        System.out.println("*****");
        System.out.println("Partial Error Caught in com.teamcenter.clientx.AppXPartialErrorListener.");


        /*
            Различные сервисы могут содержать поля ModelObject, ClientId или ничего, для каждого представления PartialError
         */
        for (int i = 0; i < stacks.length; i++)
        {
            ErrorValue[] errors = stacks[i].getErrorValues();
            System.out.println("Partial Error for");
            if (stacks[i].hasAssociatedObject())
            {
                System.out.println("object" + stacks[i].getAssociatedObject().getUid());
            }
            else if (stacks[i].hasClientId())
            {
                System.out.println("client id" + stacks[i].getClientId());
            }
            else if (stacks[i].hasClientIndex())
            {
                System.out.println("client index" + stacks[i].getClientIndex());
            }
            for (int j = 0; j < errors.length; j++)
            {
                System.out.println("    Code: " + errors[j].getCode() + "\tSeverity: "
                        + errors[j].getLevel() + "\t" + errors[j].getMessage());
            }

        }
    }
}
