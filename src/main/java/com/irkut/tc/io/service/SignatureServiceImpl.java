package com.irkut.tc.io.service;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.irkut.tc.io.dto.DtoData;
import com.irkut.tc.io.dto.RequestEntity;
import com.irkut.tc.io.dto.SignRequestData;
import com.irkut.tc.io.model.JsonData;
import com.irkut.tc.io.model.JsonDataFactory;
import com.irkut.tc.io.model.TcObjectData;
import com.irkut.tc.io.service.io.DirectoryMonitor;
import com.irkut.tc.io.service.io.FileListener;
import com.irkut.tc.io.service.io.FileMonitor;
import com.irkut.tc.io.session.TCSession;
import com.teamcenter.schemas.soa._2006_03.exceptions.ServiceException;
import com.teamcenter.soa.client.model.ModelObject;
import com.teamcenter.soa.client.model.ServiceData;
import com.teamcenter.soa.client.model.strong.EPMTask;
import com.teamcenter.soa.client.model.strong.ItemRevision;
import com.teamcenter.soa.client.model.strong.Job;
import com.teamcenter.soa.exceptions.NotLoadedException;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.lang.InterruptedException;

@Service
@RequiredArgsConstructor(onConstructor_=@Autowired)
public class SignatureServiceImpl implements SignatureService{
    private static final Logger logger = LoggerFactory.getLogger(SignatureServiceImpl.class);
    private final TCSession session;
    private final UtilEntitiesSetupService utilEntitiesSetupService;
    private final CollectedDataWriterService collectedDataWriterService;
    private final FileDownloader fileDownloader;
    private final ArrayList<String> filesToUpload = new ArrayList<>();  //список файлов для загрузки в TC(absolutePath)
    private final HashMap<String,String> fileHashMap = new HashMap<>();  //map hash-файл - значение хэша
    private final ArrayList<TcObjectData> tcDataList = new ArrayList<TcObjectData>();
    private FileMonitor directoryMonitor;

    /* signatureStatus:
    *  0 - invalid
    *  1 - valid
    *  2 - Допустимо (allowed) - просто создана новая подпись
    *  3 - other state
    * */
    private AtomicInteger signatureStatus = new AtomicInteger(5);

    public void setSignStatus(int value){
        signatureStatus.set(value);
    }



    @Override
    public void sign(boolean verify, RequestEntity requestEntity) throws NotLoadedException {
        clearLists();
        changePolicy();
        ModelObject epmJob = extractObjects(requestEntity);
        processAttachmentsAndGenerateJson(false, epmJob, null);
        try {
            executeSigningProcess(this, verify,null);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int signFromRac(boolean verify, String uidVerifiedObject, SignRequestData signRequestData) throws NotLoadedException {

        int status = -1;
        logger.info("Request from RAC. verifiedObject: " + uidVerifiedObject);
         //logger.info("Request from RAC. Получены uid ревизий:" + signRequestData.size() + ", signRequestData содержимое: " + signRequestData.stream().collect(Collectors.joining(", ")));
        List<String> uidList = signRequestData.getUid();
        for (String uid : uidList) {
                logger.info(uid);
        }

        clearLists();
        changePolicy();
        processAttachmentsAndGenerateJson(true, null, signRequestData);
        logger.info("signFromRac: filesToUpload size: " + filesToUpload.size() + ", filesToUpload содержимое: " + filesToUpload.stream().collect(Collectors.joining(", ")));
        try {
            status = executeSigningProcess(this, verify, uidVerifiedObject);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        return status;
    }



    private  void clearLists() {
        utilEntitiesSetupService.clearList(filesToUpload);
        utilEntitiesSetupService.clearList(tcDataList);
        fileHashMap.clear();
        utilEntitiesSetupService.stopDirectoryMonitor(directoryMonitor);
    }
    /**
     * Метод для изменения политики свойств объектов Teamcenter.
     */
    private void changePolicy() {
        try {
            this.session.getSessionService().setObjectPropertyPolicy("ObjectPropertiesPolicy");
            logger.info("ObjectPropertyPolicy успешно изменено на ObjectPropertiesPolicy");
        } catch (ServiceException e) {
            e.printStackTrace();
        }
    }

    private ModelObject extractObjects(RequestEntity requestEntity) {
        //Запрашиваем из базы данных объект задачи
        ServiceData serviceData = session.getDataManagementService().loadObjects(new String[]{requestEntity.getUuid()});
        //Получаем его в нормальном виде. Обычные объекты - это просто объекты, которые возвращает служба
        //если в объект не было внесено никаких изменений.
        ModelObject modelObject = serviceData.getPlainObject(0);
        logger.info("Input object type: " + modelObject.getTypeObject().getName());
        return modelObject;
    }

    /**
     *
     * @param isTcRacRequest - откуда пришел запрос - true - из TC RAC, false - из WF
     * @param epmJob - optional, can be null
     * @throws NotLoadedException - optional, can be null
     */
    private void processAttachmentsAndGenerateJson(boolean isTcRacRequest, ModelObject epmJob,
                                                   SignRequestData signRequestData) throws NotLoadedException {
        List<String> uidList = signRequestData.getUid();
        if(isTcRacRequest && !uidList.isEmpty()){


            String[] uids = new String[uidList.size()];
            ModelObject[] attachments = null;
            for(int i=0; i < uidList.size(); i++){
                    uids[i] = uidList.get(i);
            }

            ServiceData serviceData = session.getDataManagementService().loadObjects(uids);
            if(serviceData.sizeOfPlainObjects() > 0){
                attachments = new ModelObject[serviceData.sizeOfPlainObjects()];
                for(int n=0; n < attachments.length; n++){
                    attachments[n] = serviceData.getPlainObject(n);
                }
                if (attachments.length > 0) {
                    ArrayList<DtoData> dataList = new ArrayList<>();
                    Arrays.stream(attachments)
                            .filter(obj -> obj instanceof ItemRevision)
                            .forEach(obj -> processTcFiles((ItemRevision) obj, dataList));
                    generateJsonAndFiles(dataList);
                }

            }
        }

        if(!isTcRacRequest && epmJob != null){
            if (epmJob instanceof Job) {
                ModelObject[] attacments = getTaskAttachments((Job) epmJob);
                if (attacments != null && attacments.length > 0) {
                    ArrayList<DtoData> dataList = new ArrayList<>();
                    Arrays.stream(attacments)
                            .filter(obj -> obj instanceof ItemRevision)
                            .forEach(obj -> processTcFiles((ItemRevision) obj, dataList));
                    generateJsonAndFiles(dataList);
                }
            } else {
                System.out.println("Input object is not EPMTask object");
            }
        }

    }

    private ModelObject [] getTaskAttachments(Job job) throws NotLoadedException {
        session.getDataManagementService().getProperties(new ModelObject[]{job}, new String[]{"root_task"});
        EPMTask root_task = job.get_root_task();
        session.getDataManagementService().getProperties(new ModelObject[]{root_task}, new String[]{"root_target_attachments"});
        try {
            //Инициализируем массив значениями полученными из root_task
            return root_task.get_root_target_attachments();
        } catch (NotLoadedException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void processTcFiles(ItemRevision itemRevision, ArrayList<DtoData> dataList) {
        TcObjectData tcObjectData = new TcObjectData();
        tcObjectData.revision = itemRevision;
        ArrayList<String> fileNames = fileDownloader.extractTcFile(itemRevision);

        if (fileNames != null && !fileNames.isEmpty()) {
            tcObjectData.fileList.addAll(fileNames);
            ArrayList<String> sigFileList = fileNames
                    .stream()
                    .map(str -> str += ".sig")
                    .collect(Collectors.toCollection(ArrayList::new));
            filesToUpload.addAll(sigFileList);

            HashMap<String,String> tmpMap = processHashFiles(fileNames);
            if(!tmpMap.isEmpty()){
                fileHashMap.putAll(tmpMap);
            }

        }
        tcDataList.add(tcObjectData);
        DtoData dto = new DtoData();
        dto.uid = itemRevision.getUid();
        Arrays.stream(itemRevision.getPropertyNames())
                      .forEach(property -> dto.properties.put(property, getPropertyDisplayableValue(itemRevision, property)));
        dataList.add(dto);
    }

    public HashMap<String,String> processHashFiles(ArrayList<String> fileNames) {
        HashMap<String,String> resultMap = new HashMap<String,String>();
        for (String fileName : fileNames) {
            File originFile = new File(fileName);
            if(originFile.exists()){
                String hashString = calculateHashCode(originFile);
                String hashFilePath = fileName + ".hash";
                File hashFile = new File(hashFilePath);
                writeHashToFile(hashFile, hashString);

                if(hashFile.exists()){
                    //Path path = Paths.get(hashFilePath);
                    resultMap.put(hashFile.getAbsolutePath(),hashString);
                }
                //fileHashMap.put(path.getFileName().toString(),hashString);

            }
        }

        return resultMap;
    }
    public String calculateHashCode(File file) {
        //return String.valueOf(file.hashCode());
        return FileUtilHelper.calculateFileHash(file.getAbsolutePath());
    }

    public void writeHashToFile(File file, String hashString) {
        try (FileWriter fw = new FileWriter(file)){
            fw.write(hashString);
            fw.flush();
        } catch (IOException e) {
            logger.error("Error writing to file", e);
        }
    }
    private String getPropertyDisplayableValue(ModelObject object, String property) {
        try {
            session.getDataManagementService().getProperties(new ModelObject[] {object}, new String[] {property});
            return object.getPropertyDisplayableValue(property);
        } catch (NotLoadedException e) {
            e.printStackTrace();
        }
        return null;
    }


    /**
     * Метод для генерации JSON и файлов.
     * @param dataList
     */
    private void generateJsonAndFiles(ArrayList<DtoData> dataList) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        try {
            String jsonArray = objectMapper.writeValueAsString(dataList);
            if (jsonArray != null) {
                String fileName = System.getenv("USERPROFILE") + "\\.Trusted\\CryptoARM\\Documents\\" + "attr.json";
                File jsonFile = new File(fileName);
                jsonFile.createNewFile();
                FileWriter fw = new FileWriter(jsonFile);
                fw.write(jsonArray);
                fw.flush();
                fw.close();
                if (jsonFile.exists()) {
                    filesToUpload.add(fileName + ".sig");
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        logger.info("tcDataList пуст: " + tcDataList.isEmpty());
        if (!tcDataList.isEmpty()) {
            JsonData jsonData = JsonDataFactory.createJsonData("file:///C:/Users/infodba/.Trusted/CryptoARM/Documents/", tcDataList);
            System.out.println(jsonData);
            logger.info("Creating file SignTest01.json");
            collectedDataWriterService.createFile(jsonData);
            logger.info("File created");
        }
    }

    /**
     * @param signatureService
     * @param verify           - false - подписание, true - проверка
     */
    private int executeSigningProcess(SignatureServiceImpl signatureService, boolean verify, String uidVerifiedObject) throws InterruptedException {

        int status = -1;
        if (isWindows()) {
            try {
                String command = "cmd /c start cryptoarm://signAndEncrypt/file:///C:/Users/" +
                        "infodba/.Trusted/CryptoARM/Documents/SignTest01.json";
                Process process = Runtime.getRuntime().exec(command);
                process.waitFor();
                logger.info("CryptoArm finished");
            } catch (Exception e) {
                e.printStackTrace();
            }
            filesToUpload.forEach(this::deleteFileIfExists);
            /*
            directoryMonitor = new FileMonitor(1000);
            FileListener fileListener = new FileListener(directoryMonitor, signatureService,
                                                         verify,uidVerifiedObject, filesToUpload,
                                                         fileHashMap, new FileUploader(session), tcDataList);
            directoryMonitor.monitor("C:/Users/infodba/.Trusted/CryptoARM/Documents/", fileListener);
            try {
                directoryMonitor.start();
               //directoryMonitor.stop();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            */
            FileAlterationMonitor monitor = new FileAlterationMonitor(5000);
            FileAlterationObserver observer = new FileAlterationObserver("C:/Users/infodba/.Trusted/CryptoARM/Documents/");
            observer.addListener(new FileListener(monitor,signatureService,
                                                    verify,uidVerifiedObject, filesToUpload,
                                                    fileHashMap, new FileUploader(session), tcDataList));
            monitor.addObserver(observer);

            Thread monitorThread = new Thread(new DirectoryMonitor(monitor));
            monitorThread.start();
            monitorThread.join(0);

            System.out.println("Signature status: " + this.signatureStatus.get());
            logger.info("filesToUpload size: " + filesToUpload.size() + ", filesToUpload содержимое: " + filesToUpload.stream().collect(Collectors.joining(", ")));
            logger.info("hashFilesToUpload size: " + fileHashMap.size() );

            status = this.signatureStatus.get();

        }

        return status;
    }

    /**
     * Проверка типа операционной системы.
     * @return true, если операционная система Windows, иначе false.
     */
    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().startsWith("windows");
    }

    /**
     * Удаление файлов.
     */
    private void deleteFileIfExists(String file) {
        System.out.println("File: " + file);
        try {
            boolean deleted = Files.deleteIfExists(Paths.get(file));
            if (deleted) {
                System.out.println("File " + file + " ... successfully deleted!");
            } else {
                System.out.println("Delete filed. File: " + file);
            }
        } catch (IOException | SecurityException exception) {
            logger.info("Delete file exception", exception);
        }
    }


    public int hashCode() {
        int result = 17;
        result = 31 * result + session.hashCode();
        result = 31 * result + utilEntitiesSetupService.hashCode();
        result = 31 * result + collectedDataWriterService.hashCode();
        result = 31 * result + fileDownloader.hashCode();
        result = 31 * result + filesToUpload.hashCode();
        result = 31 * result + tcDataList.hashCode();
        result = 31 * result + (directoryMonitor != null ? directoryMonitor.hashCode() : 0);
        return result;
    }
}
