package com.irkut.tc.io.service;

import com.teamcenter.soa.client.model.strong.ItemRevision;

import java.util.ArrayList;

public interface FileDownloader {

    public ArrayList<String> extractTcFile(ItemRevision itemRevision);

}
