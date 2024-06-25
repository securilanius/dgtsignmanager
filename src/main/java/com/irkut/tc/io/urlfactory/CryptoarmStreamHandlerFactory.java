package com.irkut.tc.io.urlfactory;

import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;

public class CryptoarmStreamHandlerFactory implements URLStreamHandlerFactory{
    @Override
    public URLStreamHandler createURLStreamHandler(String protocol) {
        if("cryptoarm".equals(protocol)){
            return new CryptoarmStreamHandler();
        }

        return null;
    }
}
