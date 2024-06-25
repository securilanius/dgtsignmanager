package com.irkut.tc.io.service.io;

import com.irkut.tc.io.Constants;
import com.irkut.tc.io.model.TcObjectData;
import com.irkut.tc.io.service.FileUploader;
import com.irkut.tc.io.service.SignatureServiceImpl;
import com.irkut.tc.io.session.TCSession;
import com.teamcenter.schemas.soa._2006_03.exceptions.ServiceException;
import com.teamcenter.services.strong.core.DataManagementService;
import com.teamcenter.services.strong.core._2010_09.DataManagement;
import com.teamcenter.soa.client.GetFileResponse;
import com.teamcenter.soa.client.model.ModelObject;
import com.teamcenter.soa.client.model.Property;
import com.teamcenter.soa.client.model.ServiceData;
import com.teamcenter.soa.client.model.strong.Dataset;
import com.teamcenter.soa.client.model.strong.ImanFile;
import com.teamcenter.soa.client.model.strong.ItemRevision;
import com.teamcenter.services.strong.core._2007_01.DataManagement.VecStruct;
import com.teamcenter.soa.exceptions.NotLoadedException;
import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class FileListener extends FileAlterationListenerAdaptor {
    private FileAlterationMonitor monitor;

    private final static Logger logger = LogManager.getLogger(FileListener.class);
    private final ArrayList<String> createdFilesList;
    private final Map<String, File> createdFilesMap;
    private final ArrayList<String> filesToUpload;
    private final HashMap<String,String> fileHashMap; //хэш оригинальных файлов
    private final FileUploader fileUploader;
    private final ArrayList<TcObjectData> tcData;
    private HashMap<String, String> newSigCompareMap = new HashMap<>();
    private final boolean verify;
    private String dgtSignDatasetUid = "";
    private final SignatureServiceImpl signatureService;
    private final static String attrDataFilePath = System.getenv("CRARM_DOCUMENTS") + "\\attr.json";

    public final static Object lock = new Object();
    public FileListener(FileAlterationMonitor monitor, SignatureServiceImpl signatureService,
                        boolean verify, String dgtSignDatasetUid, ArrayList<String> filesToUpload,
                        HashMap<String,String> fileHashMap, FileUploader fileUploader,
                        ArrayList<TcObjectData>  tcData) {
        this.monitor = monitor;
        this.filesToUpload = filesToUpload;
        this.fileHashMap = fileHashMap;
        this.fileUploader = fileUploader;
        this.tcData = tcData;
        this.createdFilesList = new ArrayList<>();
        this.createdFilesMap = new HashMap<>();
        this.verify = verify;
        this.dgtSignDatasetUid = dgtSignDatasetUid;
        this.signatureService = signatureService;
    }

    @Override
    public void onStart(FileAlterationObserver observer) {
        super.onStart(observer);
    }
    @Override
    public void onDirectoryCreate(File directory) {
        System.out.println("create：" + directory.getAbsolutePath());
    }
    @Override
    public void onDirectoryChange(File directory) {
        System.out.println("modify：" + directory.getAbsolutePath());
    }
    @Override
    public void onDirectoryDelete(File directory) {
        System.out.println("delete：" + directory.getAbsolutePath());
    }

    @Override
    public void onFileCreate(File file) {
        String path = file.getAbsolutePath();
        System.out.println("file create：" + path);
        if (file.canRead()) {

            if(path.endsWith(".sig")){

                createdFilesList.add(file.getAbsolutePath());
                createdFilesMap.put(file.getAbsolutePath(),file);
                System.out.println("createdFilesList current size: " + createdFilesList.size());

                Collections.sort(createdFilesList);
                Collections.sort(filesToUpload);

                //если список sig-файлов для загрузки сформирован
                if(createdFilesList.equals(filesToUpload)){
                    System.out.println("createdFilesList size: " + createdFilesList.size());
                    System.out.println("filesToUpload size: " + filesToUpload.size());

                    //формируем hash файлы для созданных sig файлов и помещаем в список для загрузки в TC
                    HashMap<String, String> sigMap = signatureService.processHashFiles(createdFilesList);
                    if(!sigMap.isEmpty()){

                        for(Map.Entry<String, String> entry : sigMap.entrySet()){

                            String filePath = entry.getKey();
                            String hashVal = entry.getValue();
                            File f = new File(filePath);
                            filesToUpload.add(filePath);
                            createdFilesMap.put(filePath,f);

                            //мапинг для валидации
                            newSigCompareMap.put(f.getName(),hashVal);
                        }
                    }

                    //добавляем файл attr.json к перечню загружаемых файлов
                    File attrDataFile = new File(attrDataFilePath);
                    System.out.println("attrDataFile: " + attrDataFile.getAbsolutePath());
                    if(attrDataFile.exists()){
                        filesToUpload.add(attrDataFile.getAbsolutePath());
                        createdFilesMap.put(attrDataFile.getAbsolutePath(),attrDataFile);

                    }

                    //добавляем hash файлы к перечню загружаемых файлов
                    /*
                    for(Map.Entry<String, String> entry : fileHashMap.entrySet()){
                        String filePath = entry.getKey();
                        File hashFile = new File(filePath);
                        System.out.println("hashFile: " + hashFile.getPath());
                        if(hashFile.exists()){
                            filesToUpload.add(hashFile.getAbsolutePath());
                            createdFilesMap.put(hashFile.getAbsolutePath(),hashFile);
                        }
                    }

                     */



                    //загрузка файлов в набор данных в TC
                    if(!verify){
                        boolean isUploaded = false;
                        isUploaded = upload(filesToUpload);

                        if(isUploaded){
                            signatureService.setSignStatus(2);
                        }else{
                            signatureService.setSignStatus(-1);
                        }

                        try {

                            System.out.println("DirectoryMonitor notify stop");
                            synchronized (lock) {
                                lock.notify();
                            }

                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }

                    //проверка подписей
                    if(verify && dgtSignDatasetUid != null){
                        ArrayList<String> sigFileList = filesToUpload.stream()
                                .filter(s -> s.endsWith(".sig"))
                                .collect(Collectors.toCollection(ArrayList::new));
                        boolean res = verify(dgtSignDatasetUid, newSigCompareMap);

                        if(res){
                            logger.info("Verify sign: VALID");

                            signatureService.setSignStatus(1);
                            try {

                                System.out.println("DirectoryMonitor notify stop");
                                synchronized (lock) {
                                    lock.notify();
                                }
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        }else{
                            logger.info("Verify sign: NOT VALID");
                            signatureService.setSignStatus(0);
                            try {
                                System.out.println("DirectoryMonitor notify stop");
                                synchronized (lock) {
                                    lock.notify();
                                }
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }

                }
            }


        }
        else{
            System.out.println("file.canRead()=false");
        }
    }

    @Override
    public void onFileChange(File file) {
        String path = file.getAbsolutePath();
        System.out.println("file modify：" + path);
    }

    @Override
    public void onFileDelete(File file) {
        System.out.println("file delete：" + file.getAbsolutePath());
    }


    private boolean upload(ArrayList<String> fileUploadList){

        DataManagementService dmService = fileUploader.getDataManagementService();

        //UPLOAD FILE
        logger.info("Uploading files to TC...");
        String sertificateOwner = "IntesoUser"; //temp hardcode
        if (!fileUploadList.isEmpty()) {
            ModelObject dgtSignDataset = fileUploader.createDgtSignDataset
                    ("DigitalSign_" + sertificateOwner,
                            "IDS8_DigitalSignature",
                            "",
                            "TextEditor",
                            null);

            if(dgtSignDataset != null){
                boolean result = false;
                for(String filePath : fileUploadList){
                    logger.info("Uploading file: " + filePath);
                    result = fileUploader.addNamedReference((Dataset) dgtSignDataset,createdFilesMap.get(filePath));
                }

                //создаем аудит
                if(result && dgtSignDataset != null) {
                    createAudit(tcData, "Fnd0Apply_Digital_Signature");

                    //установка св-ва markup_status
                    dmService.getProperties(new ModelObject[]{dgtSignDataset},new String[]{"markup_status"});
                    try {
                        Property markupProp = dgtSignDataset.getPropertyObject("markup_status");
                        if(markupProp.isNull()){
                            VecStruct propVec = new VecStruct();
                            propVec.stringVec = new String[]{"Допустимо"};
                            Map<String,VecStruct> propMap = new HashMap<>();
                            propMap.put("markup_status",propVec);
                            dmService.setProperties(new ModelObject[]{dgtSignDataset},propMap);
                        }
                    } catch (NotLoadedException e) {
                        throw new RuntimeException(e);
                    }
                }

                for(TcObjectData tcObject : tcData){
                    logger.info("upload to TC: add relation");
                    ItemRevision revision = (ItemRevision) tcObject.revision;
                    fileUploader.createRelation(revision, dgtSignDataset, Constants.DGT_SIGNATURE_RElATION );

                    //refresh all objects
                    dmService.refreshObjects(new ModelObject[]{revision});

                    ModelObject[] objs = new ModelObject[] { revision };
                    dmService.getProperties(objs, new String[]{ "IMAN_specification" });
                    try {
                        ModelObject[] related = revision.get_IMAN_specification();
                        if(related.length > 0){
                            dmService.refreshObjects(related);
                        }
                    } catch (NotLoadedException e) {
                        throw new RuntimeException(e);
                    }

                }

                if(dgtSignDataset != null){
                    return true;
                }
            }
        }

        return false;
    }

    private boolean verify(String dgtSignDatasetUid, HashMap<String,String> newSigCompareMap){


        DataManagementService dtmService = fileUploader.getDataManagementService();
        TCSession session = fileUploader.getSession();
        ServiceData serviceData = dtmService.loadObjects(new String [] {dgtSignDatasetUid});
        Dataset dataset = null;
        String tempDir = System.getenv("TEMP");
        HashMap<String,String> exportedSigMap = new HashMap<>(); //имя файла - хэш строка

        if(serviceData.sizeOfPlainObjects() > 0){
            dataset = (Dataset) serviceData.getPlainObject(0);
        }

        if(dataset == null){
            //todo
            return false;
        }

        logger.info("export files from DgtSign dataset");
        dtmService.getProperties(new ModelObject[]{dataset}, new String[] {"ref_list" });
        Property refListProperty = null;
        try {
            refListProperty = dataset.getPropertyObject("ref_list");

            ModelObject[] refObjs = refListProperty.getModelObjectArrayValue();
            dtmService.getProperties(refObjs, new String[]{"file_name", "file_ext","original_file_name"});
            for (ModelObject tcFile : refObjs) {
                if (tcFile instanceof ImanFile) {

                    String fileName = tempDir + "\\" + ((ImanFile) tcFile).get_original_file_name();
                    //String fileName = tempDir + "\\" + ((ImanFile) tcFile).get_file_name();
                    if(!fileName.endsWith(".hash")){
                        continue;
                    }
                    GetFileResponse fileResp = session.getFileManagementUtility().getFileToLocation(tcFile,
                            fileName, null, null);
                    logger.info("Имя экспортируемого hash файла: " + fileName);

                    //вычисляем hash для выгруженных sig файлов
                    if(fileResp.sizeOfFiles() != 0){

                        File sigFile = new File(fileName);
                        String hashString = readFileHash(sigFile);

                        exportedSigMap.put(sigFile.getName(),hashString);

                    }



                }
            }

            if(newSigCompareMap.size() != exportedSigMap.size()){
                return false;
            }

            if(exportedSigMap.equals(newSigCompareMap)){
                return true;
            }else {
                return false;
            }
        }
        catch (NotLoadedException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }


    /**
     * создание аудита
     * @param targetslist - подписываемые ревизии
     * @param eventType - тип события аудита
     */
    private void createAudit(ArrayList<TcObjectData> targetslist, String eventType){
        DataManagementService dtmService = fileUploader.getDataManagementService();

        for(TcObjectData data : targetslist){
            ArrayList<DataManagement.PostEventObjectProperties> postEventObjPropList = new ArrayList<DataManagement.PostEventObjectProperties>();
            ItemRevision revison = (ItemRevision) data.revision;
            try {
                dtmService.getProperties(new ModelObject[]{revison}, new String[]{ "IMAN_specification" });
                ModelObject[] related = revison.get_IMAN_specification();
                for(int i=0; i< related.length; i++) {
                    if(related[i] instanceof Dataset) {
                        Dataset dataset = (Dataset)related[i];
                        dtmService.getProperties(new ModelObject[]{dataset}, new String[]{ "object_name" });
                        String type = dataset.getTypeObject().getName();
                        String datasetName = dataset.get_object_name();
                        if(type.equals("Text") && datasetName.startsWith("DigitalSign")) {

                        }else {
                            DataManagement.PostEventObjectProperties postEventObjectProperties = new DataManagement.PostEventObjectProperties();
                            postEventObjectProperties.primaryObject = dataset;
                            postEventObjPropList.add(postEventObjectProperties);
                        }
                    }
                }

            } catch (Exception e) {
                logger.error(e);
            }
            DataManagement.PostEventObjectProperties[] paramArrayOfPostEventObjectProperties = new DataManagement.PostEventObjectProperties[postEventObjPropList.size()+1];
            DataManagement.PostEventObjectProperties postEventObjectProperties = new DataManagement.PostEventObjectProperties();
            postEventObjectProperties.primaryObject = revison;
            paramArrayOfPostEventObjectProperties[0] = postEventObjectProperties;
            for(int k=0; k< postEventObjPropList.size(); k++) {
                paramArrayOfPostEventObjectProperties[k+1] = postEventObjPropList.get(k);
            }
            try {

                DataManagement.PostEventResponse res = dtmService.postEvent(paramArrayOfPostEventObjectProperties, eventType);
                DataManagement.PostEventOutput[] outs = res.output;
                ServiceData sd = res.serviceData;

            } catch (ServiceException e) {
                logger.error(e);
            }
        }
    }

    //получение строки hash-a из файла
    private String readFileHash(File file) throws IOException {

        if(file == null){
            return null;
        }
        InputStream inputStream = new FileInputStream(file);
        StringBuilder result = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = br.readLine()) != null) {
                result.append(line);
            }
        }

        if(result.length() > 0){
            System.out.println("Read file hash value: " + result);
            return result.toString();
        }

        return null;
    }
}
