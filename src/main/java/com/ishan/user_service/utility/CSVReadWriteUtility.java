package com.ishan.user_service.utility;

import com.ishan.user_service.model.User;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.Buffer;
import java.util.List;

public class CSVReadWriteUtility {


    public static void writeCSV(List<User> userList) {

        String csvFileName = "output.csv";
        try( FileWriter writer = new FileWriter(csvFileName);) {
             for (User user : userList){
                 writer.append(String.join((CharSequence) ",", (CharSequence) user));
                 writer.append("\n");
             }
            System.out.println("CSV file created successfully :: " + csvFileName);
        } catch (IOException e) {
            System.out.println("Error writing the CSV file: " + e.getMessage());
        }
    }
}
