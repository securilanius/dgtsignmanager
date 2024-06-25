package com.irkut.tc.io.urlfactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

public class CryptoarmURLConnection extends URLConnection {

    private String data;

    protected CryptoarmURLConnection(URL url){
        super(url);
    }

    @Override
    public void connect() throws IOException {

    }

    @Override
    public InputStream getInputStream() throws IOException {

          return new ByteArrayInputStream(data.getBytes());
    }
}
