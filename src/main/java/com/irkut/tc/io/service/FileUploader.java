package com.irkut.tc.io.service;

import com.irkut.tc.io.session.TCSession;

import java.io.File;

import com.teamcenter.schemas.soa._2006_03.exceptions.ServiceException;
import com.teamcenter.services.loose.core._2006_03.FileManagement.DatasetFileInfo;
import com.teamcenter.services.loose.core._2006_03.FileManagement.GetDatasetWriteTicketsInputData;
import com.teamcenter.services.strong.core.DataManagementService;
import com.teamcenter.services.strong.core._2006_03.DataManagement.CreateRelationsResponse;
import com.teamcenter.services.strong.core._2006_03.DataManagement.Relationship;
import com.teamcenter.services.strong.core._2006_03.DataManagement.CreateDatasetsResponse;
import com.teamcenter.services.strong.core._2006_03.DataManagement.CreateItemsResponse;
import com.teamcenter.services.strong.core._2006_03.DataManagement.DatasetProperties;
import com.teamcenter.services.strong.core._2006_03.DataManagement.ItemProperties;
import com.teamcenter.services.strong.core._2008_06.DataManagement.CreateResponse;
import com.teamcenter.services.strong.core._2008_06.DataManagement.CreateIn;
import com.teamcenter.soa.client.FileManagementUtility;
import com.teamcenter.soa.client.model.ModelObject;
import com.teamcenter.soa.client.model.ServiceData;
import com.teamcenter.soa.client.model.strong.*;
import com.teamcenter.soa.exceptions.NotLoadedException;
import lombok.Getter;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class FileUploader {

    @Autowired
    @Getter
    private final TCSession session;
    private final FileManagementUtility fileManagementUtility;
    @Getter
    private final DataManagementService dataManagementService;

    @Getter
    private Item item;
    @Getter
    private Dataset dataset;
    private Folder homeFolder;
    private final static Logger logger = LogManager.getLogger(FileUploader.class);

    public FileUploader(TCSession session)
    {
        this.session = session;
        this.fileManagementUtility = new FileManagementUtility(session.getConnection());
        dataManagementService = DataManagementService.getService(session.getConnection());
    }


    /**
     * Проверяет наличие ошибок в переданных данных сервиса
     *
     * @param serviceData Обьект данных сервиса, который необходимо проверить
     * @return true, если в данных сервиса есть ошибки, иначе false.
     * <p>
     * Этот метод выполняет следующие действия:
     * 1. Проверяет, есть ли частичные ошибки в данных сервиса.
     * 2. Если есть ошибки, то перебирает их и выводит сообщения об ошибках в лог.
     * 3. Возвращает true, если были обнаружены ошибки, иначе false.
     * <p>
     * Данный метод используется для проверки корректности данных, полученных от сервиса,
     * перед их дальнейшей обработкой. Он помогает выявить и зафиксировать проблемы
     * на ранней стадии, чтобы предотвратить возможные ошибки в работе приложения.
     * @since 1.0.1
     */

    private boolean checkServiceDataError(ServiceData serviceData){

        if(serviceData.sizeOfPartialErrors() > 0){

            for(int i=0; i<serviceData.sizeOfPartialErrors(); i++){
                for(String mess : serviceData.getPartialError(i).getMessages()){
                    logger.error(mess);
                }
            }
            return true;
        }
        return false;
    }

    /**
     * Создает новую связь между объектами
     * @param primary Объект модели, который будет использоваться как основа для связи.
     * @param secondary Объект модели, который выполняет роль привязываемого объекта.
     * @param relationType Тип связи, который будет создан между объектами.
     * <p>
     * Метод создает новый объект связи, используя предоставленные объекты модели и тип связи.
     * Затем он передает созданный объект связи в сервис управления данными для создания связи.
     * Метод также выполняет проверку наличия ошибок в данных, полученных от сервиса.
     * <p>
     * Автор: Ivan Yashin, Alexey Chizmak
     * @since 1.0.1
     */
    public void createRelation(ModelObject primary, ModelObject secondary,String relationType){

        Relationship[] relation = new Relationship[1];
        relation[0] = new Relationship();
        relation[0].clientId = "newRelationCreate";
        relation[0].primaryObject = primary;
        relation[0].secondaryObject = secondary;
        relation[0].relationType = relationType;

        CreateRelationsResponse relationResponse = dataManagementService.createRelations(relation);
        checkServiceDataError(relationResponse.serviceData);

    }

    /**
     * Создает новый объект цифровой подписи и связывает его с указанным объектом.
     * <p>
     * @param targetRevision Объект модели, с которым будет связана цифровая подпись.
     * @return Созданный объект цифровой подписи, если операция успешна, иначе null.
     * @throws ServiceException Если возникает ошибка при взаимодействии с сервисом управления данными.
     * <p>
     * Этот метод выполняет следующие действия:
     * 1. Создает входные данные для создания нового объекта цифровой подписи.
     * 2. Устанавливает связь между создаваемым объектом и указанным объектом модели.
     * 3. Вызывает сервис управления данными для создания нового объекта.
     * 4. Проверяет были ли ошибки при создании объекта.
     * 5. Если объект успешно создан, создает связь между объектом модели и объектом цифровой подписи.
     * 6. Возвращает созданный объект цифровой подписи или null, если возникла ошибка.
     * <p>
     * Автор: Ivan Yashin, Alexey Chizmak
     * @since 1.0.1
     */
    public ModelObject createDgtSignatureObject(ModelObject targetRevision) throws ServiceException {

        CreateIn dgtSignCreateIn = new CreateIn();
        dgtSignCreateIn.clientId = "dgtSingObjectCreate";
        dgtSignCreateIn.data.boName = "ITS7_Fnd0DigitalSignature";
        dgtSignCreateIn.data.tagProps.put("fnd0Root",targetRevision);
        CreateResponse createObjectResponse = dataManagementService.createObjects(new CreateIn[]{dgtSignCreateIn});

        if(!checkServiceDataError(createObjectResponse.serviceData)){
            if(createObjectResponse.serviceData.sizeOfCreatedObjects() > 0){
                ModelObject dgtSingObj = createObjectResponse.serviceData.getCreatedObject(0);
                if(dgtSingObj != null){
                    createRelation(targetRevision,dgtSingObj,"Fnd0DigitalSignatureRel");
                }
                return dgtSingObj;
            }
        }else {
            return null;
        }
        return null;
    }

    /**
     * Создает новый контейнер элементов для хранения ответа от криптопровайдера.
     * <p>
     * Этот метод выполняет следующие действия:
     * 1. Создает массив свойств элемента с именем "test" и пустым описанием.
     * 2. Получает директорию "Home" текущего пользователя.
     * 3. Создает новый элемент с указанными аттрибутами в указаной директории "Home".
     * 4. Создает новый набор данных с именем "dgt_sign", описанием, типом "Text" и связывает его с созданным элементом.
     * 5. Сохраняет ссылки на созданный элемент и набор данных для дальнейшего использования.
     * <p>
     * @throws NotLoadedException экземпляр создается, если не удалось получить "Home" директорию пользователя.
     * @
     * Автор: Ivan Yashin, Alexey Chizmak
     * @since  1.0.1
     */
    public void CreateItemContainer(){

        ItemProperties[] itemProps = new ItemProperties[1];
        itemProps[0] = new ItemProperties();
        itemProps[0].name = "test";
        itemProps[0].description = "";
        try {
            homeFolder = session.getCurrentUser().get_home_folder();
        }
        catch (NotLoadedException e) {
            e.printStackTrace();
        }
        CreateItemsResponse itemResp = dataManagementService.createItems(itemProps,homeFolder,"");
        item = itemResp.output[0].item;
        ItemRevision itemRev = itemResp.output[0].itemRev;
        DatasetProperties[] dataProps = new DatasetProperties[] { new DatasetProperties()};
        dataProps[0].clientId = "new_dataset";
        dataProps[0].name = "dgt_sign";
        dataProps[0].description = "";
        dataProps[0].relationType = "IMAN_reference";
        dataProps[0].type = "Text";
        dataProps[0].toolUsed = "TextEditor";
        dataProps[0].container = itemRev;

        CreateDatasetsResponse dataResp = dataManagementService.createDatasets(dataProps);
        dataset = dataResp.output[0].dataset;
    }

    public void testDgtProps(ModelObject target){

        //AKrV9gZhJSAgVD

        ServiceData loadData = dataManagementService.loadObjects(new String[]{ "AKrV9gZhJSAgVD" });

        logger.info("testDgtProps: size plain objects: " + loadData.sizeOfPlainObjects());

        if(loadData.sizeOfPlainObjects() > 0){

            ModelObject dgtSgnObject = loadData.getPlainObject(0);
            dataManagementService.getProperties(new ModelObject[]{dgtSgnObject},new String[]{"fnd0CMSString"});

            try {
                String value[] = dgtSgnObject.getPropertyObject("fnd0CMSString").getStringArrayValue();
                for(String s : value){
                    logger.info("testDgtProps CMSString value: " + s);
                }


                CreateIn dgtSignCreateIn = new CreateIn();
                dgtSignCreateIn.clientId = "dgtSingObjectCreate";
                dgtSignCreateIn.data.boName = "Fnd0DigitalSignature";
                dgtSignCreateIn.data.tagProps.put("fnd0Target",target);
                dgtSignCreateIn.data.stringArrayProps.put("fnd0CMSString",value);
                //dgtSignCreateIn.data.stringProps.put("fnd0Comments","");

                logger.info("testDgtProps creating Fnd0DigitalSignature");
                logger.info("testDgtProps: user validation check:" + System.getenv("TC_DS_USERID_VALIDATION_ENABLED"));
                //System.setProperty("TC_DS_USERID_VALIDATION_ENABLED","FALSE");

                try {
                    CreateResponse createObjectResponse = dataManagementService.createObjects(new CreateIn[]{dgtSignCreateIn});
                } catch (ServiceException e) {
                    throw new RuntimeException(e);
                }


            } catch (NotLoadedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Создает новый набор данных для цифровой подписи с указанными параметрами.
     *
     * @param datasetName Имя создаваемого набора данных.
     * @param datasetType Тип создаваемого набора данных.
     * @param relationName Тип связи, который будет создан для нового набора данных.
     * @param toolUsed Инструмент, который будет использоваться для набора данных.
     * @param targetRevision Ревизия объекта, с которой будет связываться создаваемый набор данных.
     * @return Созданный объект набора данных, если операция успешна, иначе null.
     *<p>
     * Этот метод выполняет следующие действия:
     * 1. Создает массив свойств для нового набора данных с указанными параметрами.
     * 2. Вызывает сервис управления данными для создания нового набора данных.
     * 3. Проверяет наличие ошибок при создании набора данных.
     * 4. Если набор данных создан успешно, возвращает созданный объект.
     * 5. Если возникла ошибка, возвращает null.
     *<p>
     * Автор: Ivan Yashin, Alexey Chizmak
     * @since 1.0.1
     */
    public ModelObject createDgtSignDataset
            (String datasetName,
             String datasetType,
             String relationName,
             String toolUsed,
             ModelObject targetRevision)
    {

        DatasetProperties[] dataProps = new DatasetProperties[] { new DatasetProperties()};
        dataProps[0].clientId = "NewDgtSignDataset";
        dataProps[0].name = datasetName;
        dataProps[0].description = "";
        dataProps[0].relationType = relationName;
        dataProps[0].type = datasetType;
        dataProps[0].toolUsed = toolUsed;
        dataProps[0].container = null;//targetRevision;
        CreateDatasetsResponse datasetResponse = dataManagementService.createDatasets(dataProps);

        if(!checkServiceDataError(datasetResponse.serviceData)){
            if(datasetResponse.serviceData.sizeOfCreatedObjects() > 0){
                return datasetResponse.serviceData.getCreatedObject(0);
            }
        }else {
            return null;
        }
        return null;
    }

    /**
     * Создает входные данные для получения тикета на запись в набор данных с указанным файлом.
     *
     * @param dataset Набор данных, в который будет добавлен файл.
     * @param file Файл, который будет добавлен в набор данных.
     * @return Входные данные для получения билета на запись в набор данных.
     * <p>
     * Этот метод выполняет следующие действия:
     * 1. Создает информацию о файле, включая его имя, путь, тип и другие аттрибуты.
     * 2. Создает массивинформации о файлеи добавляет в него созданную информацию о файле.
     * 3. Создает входные данные для получения тикета на запись в набор данных с указанным файлом.
     * 4. Устанавливает связь между набором данных, файлом и другими необходимыми параметрами.
     * 5. Возвращает созданные входные данные для дальнейшего использования.
     * <p>
     * Автор: Ivan Yashin, Alexey Chizmak
     * @since 1.0.1
     */
    private GetDatasetWriteTicketsInputData getGetDatasetWriteTicketsInputData(Dataset dataset,File file){

        DatasetFileInfo[] fileInfo = new DatasetFileInfo[1];
        DatasetFileInfo fileInfoData = new DatasetFileInfo();

        fileInfoData.clientId = "new_file";
        fileInfoData.fileName = file.getAbsolutePath();
        fileInfoData.namedReferencedName = "Text";
        fileInfoData.isText = true;
        fileInfoData.allowReplace = false;

        fileInfo[0] = fileInfoData;

        GetDatasetWriteTicketsInputData inputData = new GetDatasetWriteTicketsInputData();
        inputData.dataset = dataset;
        inputData.createNewVersion = true;
        inputData.datasetFileInfos = fileInfo;

        return inputData;
    }

    /**
     * Добавляет файл в качестве именованной ссылки в указанный набор данных.
     *
     * @param dataset Набор данных, в который необходимо добавить файл.
     * @param file Файл, который добавляется в качестве именованной ссылки.
     * <p>
     * Этот метод выполняет следующие действия:
     * 1. Получает входные данные для получения тикета на запись в набор данных.
     * 2. Передает полученные входные данные в FileManagementUtility для добавления файла.
     * <p>
     * Добавление файла цифровой электронной подписи в качестве именованной ссылки позволяет связать файл с набором данных,
     * что позволит обратиться к нему через Workspace.
     * Автор: Ivan Yashin, Alexey Chizhmak
     * @since 1.0.1
     */
    public boolean addNamedReference(Dataset dataset, File file)  {
        GetDatasetWriteTicketsInputData inputData = getGetDatasetWriteTicketsInputData(dataset, file);
        GetDatasetWriteTicketsInputData[] inputs  = new  GetDatasetWriteTicketsInputData[] { inputData };
        ServiceData serviceData = fileManagementUtility.putFiles(inputs);
        return serviceData.sizeOfPartialErrors() == 0;
    }

}
