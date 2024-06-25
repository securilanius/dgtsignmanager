package com.irkut.tc.io.session;

import com.teamcenter.soa.client.RequestListener;

public class CustomRequestListener implements RequestListener {

    public void serviceRequest (final Info info)
    {
        //will log the service name when done
    }

    public void serviceResponse( final Info info)
    {
        System.out.println( info.id + ": " + info.service + "." + info.operation);
    }
}
