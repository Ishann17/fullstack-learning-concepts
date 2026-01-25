package com.ishan.user_service.utility;

import com.ishan.user_service.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class CSVReadWriteUtility {

    private static final Logger log = LoggerFactory.getLogger(CSVReadWriteUtility.class);
    private static final String NEW_LINE_SEPARATOR = "\n";
    private static final String FILE_HEADER = "id,name,age,gender,city,state,email,mobile";

    public static Resource writeCSV(List<User> userList, String path) {
        long startTime = System.currentTimeMillis();
        String csvFileName = "AllUsersCSV.csv";
        Path csvFilePath = Paths.get(path, csvFileName);
        try( FileWriter writer = new FileWriter(csvFilePath.toFile())) {
            writer.append(FILE_HEADER);
            writer.append(NEW_LINE_SEPARATOR);
             for (User user : userList){
                 writer.append(toCsvRow(user));
                 writer.append(NEW_LINE_SEPARATOR);
             }
            log.info("CSV Exported Successfully  file name : {} & stored at : {}",csvFileName,csvFilePath);
            return new FileSystemResource(csvFilePath);
        } catch (IOException e) {
            log.error("Error writing the CSV file: {}", e.getMessage());
        }finally {
            long endTime = System.currentTimeMillis();
            double executionTimeInSeconds = (endTime - startTime) / 1000.0;
            log.info("CSV Export completed in {} seconds", executionTimeInSeconds);
        }
        return null;
    }

    public static String toCsvRow(User user){
        String id = user.getId() != null ? String.valueOf(user.getId()) : "";
        String fName = user.getFirstName() != null ? user.getFirstName() : "";
        String lName = user.getLastName() != null ? user.getLastName() : "";
        String age = String.valueOf(user.getAge());
        String gender = user.getGender() != null ? user.getGender() : "";
        String city = user.getCity() != null ? user.getCity() : "";
        String state = user.getState() != null ? user.getState() : "";
        String email = user.getEmail() != null ? user.getEmail() : "";
        String mobile = user.getMobileNumber() != null ? user.getMobileNumber() : "";
        String name = fName + " " + lName;
        return String.join(",", id,name,age,gender,city,state,email,mobile);
    }

    public static String getCSVHeader(){
        return FILE_HEADER+NEW_LINE_SEPARATOR;
    }
}

