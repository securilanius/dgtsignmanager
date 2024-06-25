package com.irkut.tc.io.service;

import com.irkut.tc.io.dto.RequestEntity;
import com.irkut.tc.io.dto.SignRequestData;
import com.irkut.tc.io.model.TcObjectData;
import com.teamcenter.soa.exceptions.NotLoadedException;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.ArrayList;
import java.util.List;

/**
 * Processes a request and performs necessary operations.
 *
 * @param
 *
 */
public interface SignatureService {
    public void sign(boolean verify,RequestEntity requestEntity) throws NotLoadedException;

    public int signFromRac(boolean verify,String uidVerifiedObject,SignRequestData signRequestData) throws NotLoadedException;

}
