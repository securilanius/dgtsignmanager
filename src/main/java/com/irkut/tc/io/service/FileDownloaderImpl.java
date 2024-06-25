package com.irkut.tc.io.service;

import java.util.ArrayList;

import com.irkut.tc.io.Constants;
import com.irkut.tc.io.session.TCSession;
import com.teamcenter.services.strong.core.DataManagementService;
import com.teamcenter.soa.client.GetFileResponse;
import com.teamcenter.soa.client.model.ModelObject;
import com.teamcenter.soa.client.model.Property;
import com.teamcenter.soa.client.model.strong.*;
import com.teamcenter.soa.exceptions.NotLoadedException;
import lombok.RequiredArgsConstructor;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor(onConstructor_=@Autowired)
public class FileDownloaderImpl implements FileDownloader{

    private final static Logger logger = LogManager.getLogger(FileDownloaderImpl.class);
    private final TCSession session;
    @Value("${folder.path}")
    private String exportDir;

    /**
     * <n>Получает файлы, связанные отношением с объектом типа {@code ItemRevision} и возвращает список их имен.</n>
     * Этот метод выполняет следующие действия:
     *  1. Получает атрибуты объекта {@code ItemRevision}, в том числе {@code IMAN_Specification}.
     *  2. Итерируется по связанным объектам <code>IMAN_specification</code>.
     *  3. Для каждого связанного объекта:
     *      a. Получает поле {@code ref_list};
     *      b. Извлекает файлы из {@code ref_list};
     *      c. Экспортирует полученные файлы в указанную директорию;
     *      d. Добавляет имена этих файлов в список;
     *  4. Возвращает список имен извлеченных файлов.
     *
     * @param revision Объект типа {@code ItemRevision}, с которым связаны запрашиваемые файлы.
     * @return Список имен файлов, полученных в ходе выполнения операции.
     * @throws RuntimeException Если возникает ошибка при извлечении файла.
     * @author Ivan Yashin, Alexey Chizhmak
     * @since 1.0.1
     */
    public ArrayList<String> extractTcFile(ItemRevision revision)
    {
        ArrayList<String> extractedFiles = new ArrayList<String>();
        DataManagementService dtmService = session.getDataManagementService();
        ModelObject[] objs = new ModelObject[] { revision };
        dtmService.getProperties(objs, new String[]{ "IMAN_specification" });
        try {
            ModelObject[] datasetArr = revision.get_IMAN_specification();
            //TODO: Добавить логику фильтрации в зависимости от полиси.
            if(datasetArr.length > 0)
            {
                for(ModelObject dataset : datasetArr){

                    //пропускаем существующий НД цифровой подписи
                    if(dataset instanceof  Dataset){
                        Dataset datasetObj = (Dataset) dataset;
                        dtmService.getProperties(new ModelObject[]{dataset}, new String[] {"object_name" });
                        String dtsName = datasetObj.get_object_name();
                        if(dtsName.startsWith(Constants.DGT_SIGNATURE_DATASET_NAME)){
                            continue;
                        }
                    }


                    logger.info("export dataset");
                    dtmService.getProperties(new ModelObject[]{dataset}, new String[] {"ref_list" });
                    Property refListProperty =  dataset.getPropertyObject("ref_list");
                    ModelObject[] refObjs = refListProperty.getModelObjectArrayValue();
                    dtmService.getProperties(refObjs, new String[] {"file_name","original_file_name","file_ext","object_string" });
                    for(ModelObject tcFile : refObjs){
                        if(tcFile instanceof ImanFile) {

                            String fileName = exportDir + "\\" + ((ImanFile) tcFile).get_original_file_name();

                            logger.info("extractTCFiles get_object_string: " + ((ImanFile) tcFile).get_object_string());
                            logger.info("extractTCFiles get_file_name: " + ((ImanFile) tcFile).get_file_name());
                            logger.info("extractTCFiles get_original_file_name: " + ((ImanFile) tcFile).get_original_file_name());
                            //пропускаем .qaf файлы, которые могут быть в НД
                            if(fileName.endsWith(".qaf")){
                                continue;
                            }
                            GetFileResponse fileResp = session.getFileManagementUtility().getFileToLocation(tcFile,
                                    fileName,null,null);
                            logger.info("Имя файла: " + fileName);
                            extractedFiles.add(fileName);
                        }
                    }
                  }
                }
            } catch (NotLoadedException e) {
            throw new RuntimeException(e);
        }
        return extractedFiles;
    }
}
