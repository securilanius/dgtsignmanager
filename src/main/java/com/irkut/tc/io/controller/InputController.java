package com.irkut.tc.io.controller;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.irkut.tc.io.Constants;
import com.irkut.tc.io.dto.RequestEntity;
import com.irkut.tc.io.dto.SignRequestData;
import com.irkut.tc.io.model.JsonData;
import com.irkut.tc.io.model.JsonDataFactory;
import com.irkut.tc.io.model.TcObjectData;
import com.irkut.tc.io.service.*;
import com.irkut.tc.io.service.io.FileMonitor;
import com.irkut.tc.io.session.TCSession;
import com.teamcenter.schemas.soa._2006_03.exceptions.ServiceException;
import com.teamcenter.services.strong.core.DataManagementService;
import com.teamcenter.services.strong.core._2007_01.DataManagement;
import com.teamcenter.soa.client.model.ModelObject;
import com.teamcenter.soa.client.model.ServiceData;
import com.teamcenter.soa.client.model.strong.*;
import com.teamcenter.soa.exceptions.NotLoadedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springdoc.api.ErrorMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.*;
import java.util.concurrent.Callable;

@RestController
@Validated
@RequestMapping("signing_test")
public class InputController {

    private static final Logger logger = LoggerFactory.getLogger(InputController.class);
    private final CollectedDataWriterService collectedDataWriterServiceImpl;
    private final SignatureService signatureService;
    private ArrayList<String> filesToUpload = new ArrayList<>();
    private ArrayList<TcObjectData> tcDataList = new ArrayList<TcObjectData>();
    private FileMonitor directoryMonitor;
    private final TCSession session;

    @Autowired
    public InputController (
            CollectedDataWriterService jsonFileService,
            ArrayList<TcObjectData> tcDataList,
            SignatureService signatureService,
            TCSession session)
    {

        this.collectedDataWriterServiceImpl = jsonFileService;
        this.tcDataList = tcDataList;
        this.directoryMonitor = null;
        this.signatureService = signatureService;
        this.session = session;

    }

    /**
     * Этот метод служит для обработки входящего post запроса с uid процесса и координации запросов к бизнес логике.
     */
    @PostMapping("/test")
    public ResponseEntity postController(@RequestBody RequestEntity requestEntity) throws ServiceException, NotLoadedException {

        signatureService.sign(false, requestEntity);
        return ResponseEntity.ok(HttpStatus.OK);

    }

    @PostMapping("/verify")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<Map<String, Object>> handleTcVerifySignRequest(@RequestBody SignRequestData signRequestData) throws ServiceException, NotLoadedException {

        Map<String, Object> returnRes = null;

        //String datasetUid = signRequestData.get(0).getUid(); //uid набора данных DigitalSign
        String datasetUid = signRequestData.getUids().get(0);
        ModelObject dgtSignDataset = null;
        ArrayList<ItemRevision> revisionList = new ArrayList<ItemRevision>();
        DataManagementService dtmService = session.getDataManagementService();

        ServiceData serviceData = dtmService.loadObjects(new String[]{datasetUid});
        if(serviceData.sizeOfPlainObjects() > 0){
            dgtSignDataset = serviceData.getPlainObject(0);
        } else {
            return ResponseEntity.badRequest().body(null);
        }

        // берем ссылки на подписанные ревизии (where referenced)
        DataManagement.WhereReferencedResponse referencedResponce =
                dtmService.whereReferenced(new WorkspaceObject[]{(WorkspaceObject) dgtSignDataset}, 1);
        if(referencedResponce.output != null){
            for(DataManagement.WhereReferencedOutput output : referencedResponce.output){
                DataManagement.WhereReferencedInfo[] refInfoArr = output.info;
                for(DataManagement.WhereReferencedInfo refInfo : refInfoArr){
                    if(refInfo.relation.equals(Constants.DGT_SIGNATURE_RElATION )){
                        revisionList.add((ItemRevision) refInfo.referencer);
                    }
                }
            }
        }

        if(!revisionList.isEmpty()){
            List<SignRequestData> verifyRequestData = new ArrayList<SignRequestData>();
            for(ItemRevision revision : revisionList){
                SignRequestData data = new SignRequestData();
                data.setUids(new  ArrayList<String>(Arrays.asList(revision.getUid())));
                System.out.println("revision to verify: " + revision.getUid());
                verifyRequestData.add(data);
            }

            if(!verifyRequestData.isEmpty()){


                try {
                    // Возвращаем статус ОК (200) и сообщение до выполнения метода
                    Map<String, Object> response = new HashMap<>();
                    response.put("status", "Запрос принят. Обрабатываю...");
                    ResponseEntity<Map<String, Object>> initialResponse = ResponseEntity.ok(response);
                    // Вызываем асинхронный метод для обработки запроса
                    Callable<Map<String, Object>> processTask = () -> processSignRequestAsync(true,datasetUid,verifyRequestData);
                    Map<String, Object> result = processTask.call();
                    returnRes = result;
                    // Возвращаем результат обработки запроса
                    return ResponseEntity.ok(result);
                } catch (Exception e) {
                    // Формируем ответ с ошибкой
                    Map<String, Object> response = new HashMap<>();
                    response.put("status", "Ошибка");
                    response.put("message", "Ошибка во время обработки запроса");
                    returnRes = response;
                    return ResponseEntity.badRequest().body(response);
                }

                ///result = signatureService.signFromRac(true,datasetUid,verifyRequestData);
            }
        }
        return ResponseEntity.badRequest().body(returnRes);
    }




    /**
     *  Обработка запроса на подписание из Teamcenter RAC
     * @param signRequestData
     */
    @PostMapping("/sign")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<Map<String, Object>> handleTcSignRequest(@Valid @RequestBody SignRequestData signRequestData) throws ServiceException, NotLoadedException {

            try {
                Map<String, Object> response = new HashMap<>();
                response.put("status", "Запрос принят. Обрабатываю...");
                ResponseEntity<Map<String, Object>> initialResponse = ResponseEntity.ok(response);
                // Вызываем асинхронный метод для обработки запроса
                Callable<Map<String, Object>> processTask = () -> processSignRequestAsync(false,null,signRequestData);
                Map<String, Object> result = processTask.call();
                // Возвращаем результат обработки запроса
                return ResponseEntity.ok(result);
            } catch (Exception e) {
                // Формируем ответ с ошибкой
                Map<String, Object> response = new HashMap<>();
                response.put("status", "Ошибка");
                response.put("message", "Ошибка во время обработки запроса");
                return ResponseEntity.badRequest().body(response);
            }
    }

    @Async
    public Map<String, Object> processSignRequestAsync(boolean verify, String uidVerifiedObject, SignRequestData signRequestData) {

        int status = -1;
        try {
            // Логика обработки запроса
            status = signatureService.signFromRac(verify,uidVerifiedObject,signRequestData);
            // Формируем ответ
            Map<String, Object> response = new HashMap<>();
            response.put("status", "Операция выполнена успешно");
            if(status == 1){
                response.put("signStatus","valid");
            }
            if(status == 0){
                response.put("signStatus","invalid");
            }
            return response;
        } catch (Exception e) {
            // Формируем ответ с ошибкой
            Map<String, Object> response = new HashMap<>();
            response.put("status", "Ошибка");
            response.put("message", e.getMessage());
            return response;
        }

    }

    @ExceptionHandler(NotLoadedException.class)
    public ResponseEntity<ErrorMessage> handleNotLoadedException(NotLoadedException notLoadedException) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ErrorMessage(notLoadedException.getMessage()));
    }

    @ExceptionHandler(ServiceException.class)
    public ResponseEntity<ErrorMessage> handleServiceException(ServiceException serviceException) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorMessage(serviceException.getLocalizedMessage()));
    }
    /*
    @ExceptionHandler(SignRequestProcessingException.class)
    public ResponseEntity <Map<String, Object>> handleSignRequestProcessingException(SignRequestProcessingException e) {
        logger.error("Ошибка обработки запроса на подпись: {}", e.getMessage());
        Map<String, Object> response = new HashMap<>();
        response.put("status", "Ошибка");
        response.put("message", e.getMessage());
        return ResponseEntity.badRequest().body(response);
    }
*/
    //@ExceptionHandler(HttpMessageNotReadableException.class)

}
